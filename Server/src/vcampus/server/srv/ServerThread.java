/*
 * ServerThread
 *
 * Version 1.1 新增查询可删除医生处理器
 *
 * 2026-08-30
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.server.srv;

import vcampus.common.constant.StudentProtocol;
import vcampus.common.constant.IConstant;
import vcampus.common.vo.Appointment;
import vcampus.common.vo.Doctor;
import vcampus.common.vo.HospitalAdminReq;
import vcampus.common.vo.Message;
import vcampus.common.vo.MessageType;
import vcampus.common.vo.Student;
import vcampus.common.vo.User;
import vcampus.server.srv.Library.LibraryHandler;
import vcampus.server.srv.Library.PaperHandler;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 服务线程类：{@link Server} 每接受一个客户端连接，就创建一个 ServerThread
 * 交给独立线程运行，实现"一个客户端一个线程"的多线程模型。
 *
 * <p>
 * 本线程只处理一次请求/响应：读取客户端发来的一个 {@link Message}，
 * 根据 {@code Message.getName()} 进行分发。用户管理、图书馆、学籍、医院模块
 * 沿用注册表（{@code _handlerMap}）方式；其余业务模块（如商店）则通过
 * {@link ModuleHandler} 处理器列表接入（见 {@link StoreModuleHandler}），
 * 处理完成后把结果封装成响应 {@link Message} 写回客户端，随后关闭连接、线程结束。
 * </p>
 *
 * <p>
 * 注意：无论先读还是先写，{@link ObjectOutputStream} 都必须在
 * {@link ObjectInputStream} 之前创建并 flush，否则两端会互相等待对方的
 * 流头信息而卡死（这是 Java 对象序列化流的一个经典坑）。
 * </p>
 */
public class ServerThread implements Runnable {
    /** 与客户端建立的连接。 */
    private final Socket _socket;
    /** 用户业务服务，由本线程独立持有，避免多线程共享状态。 */
    private final IUserServerSrv _userServerSrv = new UserServerSrv();
    /** 图书馆模块业务服务，由本线程独立持有，避免多线程共享状态。 */
    private final LibraryHandler _libraryHandler = new LibraryHandler();

    private final PaperHandler _paperHandler = new PaperHandler();

    /** 学籍模块请求处理器，由统一服务器负责分发请求。 */
    private final StudentRequestHandler _studentRequestHandler = new StudentRequestHandler();

    /**
     * 学籍模块业务服务，单独再持有一份（不经过 {@link StudentRequestHandler}），
     * 专门给"学生自助注册顺带写学籍记录"这个场景用——{@code StudentRequestHandler}
     * 那层的新增/修改学籍是管理员专属操作，会做权限校验，但注册时创建自己的
     * 学籍记录不应该被这个校验拦住，所以直接调业务层，绕开权限检查这一层。
     */
    private final StudentServerSrv _studentServerSrv = new StudentServerSrv();

    /** 医院挂号模块业务服务 */
    private final IHospitalServerSrv _hospitalSrv = new HospitalServerSrv();

    /** AI 问答模块业务服务。 */
    private final IAIServerSrv _aiServerSrv = new AIServerSrv();

    /** 各业务模块的请求处理器列表（模块化接入，见 {@link ModuleHandler}）。 */
    private final List<ModuleHandler> _moduleHandlers = new ArrayList<>();

    /** 用户管理/图书馆/学籍/医院模块的请求处理器注册表。 */
    private final Map<String, RequestHandler> _handlerMap = new HashMap<>();

    /**
     * 构造方法。
     *
     * @param socket 已经与客户端建立好的连接
     */
    public ServerThread(Socket socket) {
        this._socket = socket;
        registerHandlers();
        _moduleHandlers.add(new StoreModuleHandler());
        _moduleHandlers.add(new CourseHandler());
    }

    /**
     * 线程执行体：读取一个请求、处理、返回一个响应，然后关闭连接。
     */
    @Override
    public void run() {
        String remote = String.valueOf(_socket.getRemoteSocketAddress());
        try (Socket socket = _socket;
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
            out.flush();
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            Message request = (Message) in.readObject();
            System.out.println("[" + remote + "] 收到请求：" + request);
            Message response = handleRequest(request);
            out.writeObject(response);
            out.flush();
            System.out.println("[" + remote + "] 已返回响应：" + response);
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("[" + remote + "] 处理客户端请求时发生异常：" + e.getMessage());
        } finally {
            System.out.println("[" + remote + "] 连接已关闭");
        }
    }

    /**
     * 根据请求的消息名分发到具体的业务处理方法。
     *
     * @param request 客户端发来的请求消息
     * @return 处理结果对应的响应消息
     */
    private Message handleRequest(Message request) {
        String name = request.getName();
        RequestHandler handler = _handlerMap.get(name);
        if (handler != null) {
            return handler.handle(request);
        }
        for (ModuleHandler moduleHandler : _moduleHandlers) {
            if (moduleHandler.supportedMessages().contains(name)) {
                return moduleHandler.handle(request);
            }
        }
        return new Message(request.getUid(), name, MessageType.DATA,
                IConstant.STATUS_ERROR, "未知的请求类型：" + name, "Server");
    }

    /**
     * 处理图书馆模块请求，转发给 {@link LibraryHandler}。
     *
     * @param request 图书馆相关请求消息
     * @return 图书馆模块的处理结果
     */
    private Message handleLibraryRequest(Message request) {
        return _libraryHandler.handle(request);
    }

    /**
     * 处理学籍模块请求，转发给 {@link StudentRequestHandler}。
     *
     * @param request 学籍模块请求消息
     * @return 学籍模块处理结果
     */
    private Message handleStudentRequest(Message request) {
        return _studentRequestHandler.handle(request);
    }

    /**
     * 注册请求处理器。各模块按 {@code Message.getName()} 的取值把自己的处理
     * 方法注册进来，新增模块时只需在这里加一行，不用改 {@link #handleRequest}。
     * 不方便用固定消息名列表接入的模块（如商店），改用 {@link #_moduleHandlers}
     * 列表接入，见构造方法。
     */
    private void registerHandlers() {
        // ========= 用户模块（main主干原样保留） =========
        _handlerMap.put(IConstant.MSG_LOGIN, this::handleLogin);
        _handlerMap.put(IConstant.MSG_REGISTER, this::handleRegister);
        _handlerMap.put(IConstant.MSG_LOGOUT, this::handleLogout);

        // ========= 图书馆模块（main主干原样保留） =========
        _handlerMap.put(IConstant.MSG_QUERY_BOOKS, this::handleLibraryRequest);
        _handlerMap.put(IConstant.MSG_BORROW_BOOK, this::handleLibraryRequest);
        _handlerMap.put(IConstant.MSG_RETURN_BOOK, this::handleLibraryRequest);
        _handlerMap.put(IConstant.MSG_GET_BORROW_RECORDS, this::handleLibraryRequest);
        _handlerMap.put(IConstant.MSG_ADD_BOOK, this::handleLibraryRequest);
        _handlerMap.put(IConstant.MSG_UPDATE_BOOK, this::handleLibraryRequest);
        _handlerMap.put(IConstant.MSG_DELETE_BOOK, this::handleLibraryRequest);
        _handlerMap.put(IConstant.MSG_QUERY_PAPERS, this::handlePaperRequest);
        _handlerMap.put(IConstant.MSG_GET_PDF_DATA, this::handlePaperRequest);
        _handlerMap.put(IConstant.MSG_ADD_PAPER, this::handlePaperRequest);
        _handlerMap.put(IConstant.MSG_DELETE_PAPER, this::handlePaperRequest);
        _handlerMap.put(IConstant.MSG_UPLOAD_PDF, this::handlePaperRequest);

        // ========= 学籍模块 =========
        _handlerMap.put(StudentProtocol.LIST, this::handleStudentRequest);
        _handlerMap.put(StudentProtocol.GET_SELF, this::handleStudentRequest);
        _handlerMap.put(StudentProtocol.QUERY_BY_ID, this::handleStudentRequest);
        _handlerMap.put(StudentProtocol.QUERY_BY_CARD, this::handleStudentRequest);
        _handlerMap.put(StudentProtocol.QUERY_BY_NAME, this::handleStudentRequest);
        _handlerMap.put(StudentProtocol.ADD, this::handleStudentRequest);
        _handlerMap.put(StudentProtocol.UPDATE, this::handleStudentRequest);
        _handlerMap.put(StudentProtocol.DELETE, this::handleStudentRequest);
        _handlerMap.put(IConstant.MSG_USER_SET_STATUS, this::handleSetUserStatus);
        _handlerMap.put(IConstant.MSG_USER_LIST_PENDING, this::handleListPendingUsers);
        _handlerMap.put(IConstant.MSG_REGISTER_STUDENT, this::handleRegisterStudent);
        _handlerMap.put(IConstant.MSG_AI_ASK, this::handleAiAsk);

        // ========= 医院挂号模块【新增，只追加不删除原有】 =========
        _handlerMap.put(IConstant.MSG_HOSPITAL_QUERY_ALL_DOCTOR, this::handleQueryAllDoctor);
        _handlerMap.put(IConstant.MSG_HOSPITAL_QUERY_DOCTOR_BY_DEPT, this::handleQueryDoctorByDept);
        _handlerMap.put(IConstant.MSG_HOSPITAL_ADD_APPOINTMENT, this::handleAddAppointment);
        _handlerMap.put(IConstant.MSG_HOSPITAL_QUERY_MY_APPOINTMENT, this::handleQueryMyAppointment);
        _handlerMap.put(IConstant.MSG_HOSPITAL_CANCEL_APPOINTMENT, this::handleCancelAppointment);
        _handlerMap.put(IConstant.MSG_HOSPITAL_QUERY_ALL_APPOINTMENT, this::handleQueryAllAppointment);
        _handlerMap.put(IConstant.MSG_HOSPITAL_ADD_DOCTOR, this::handleAddDoctor);
        _handlerMap.put(IConstant.MSG_HOSPITAL_UPDATE_DOCTOR, this::handleUpdateDoctor);
        _handlerMap.put(IConstant.MSG_HOSPITAL_DELETE_DOCTOR, this::handleDeleteDoctor);
        _handlerMap.put(IConstant.MSG_HOSPITAL_GET_OCCUPIED_TIME, this::handleGetDoctorOccupiedTime);
        //新增：查询可安全删除医生
        _handlerMap.put(IConstant.MSG_HOSPITAL_QUERY_CAN_DELETE_DOCTOR, this::handleQueryCanDeleteDoctor);
        _handlerMap.put(IConstant.MSG_HOSPITAL_DELETE_CANCEL_APPOINT, this::handleDeleteCancelAppoint);
    }

    /**
     * 处理登录请求。
     *
     * @param request 登录请求消息，{@code data} 为待验证的 {@link User}
     * @return 登录结果消息：成功时 {@code data} 为完整用户信息，失败时为错误提示文本
     */
    private Message handleLogin(Message request) {
        try {
            User loginUser = (User) request.getData();
            User found = _userServerSrv.login(loginUser);
            if (found == null) {
                return new Message(request.getUid(), IConstant.MSG_LOGIN, MessageType.DATA,
                        IConstant.STATUS_LOGIN_FAIL, "用户名或密码错误", "Server");
            }
            return new Message(request.getUid(), IConstant.MSG_LOGIN, MessageType.DATA,
                    IConstant.STATUS_SUCCESS, found, "Server");
        } catch (UserDisabledException e) {
            return new Message(request.getUid(), IConstant.MSG_LOGIN, MessageType.DATA,
                    IConstant.STATUS_ACCOUNT_DISABLED, e.getMessage(), "Server");
        } catch (UserPendingApprovalException e) {
            return new Message(request.getUid(), IConstant.MSG_LOGIN, MessageType.DATA,
                    IConstant.STATUS_ACCOUNT_PENDING, e.getMessage(), "Server");
        } catch (IllegalArgumentException e) {
            return new Message(request.getUid(), IConstant.MSG_LOGIN, MessageType.DATA,
                    IConstant.STATUS_BAD_REQUEST, e.getMessage(), "Server");
        } catch (SQLException | IOException e) {
            return new Message(request.getUid(), IConstant.MSG_LOGIN, MessageType.DATA,
                    IConstant.STATUS_ERROR, "服务器内部异常：" + e.getMessage(), "Server");
        }
    }

    /**
     * 处理注册请求。
     *
     * @param request 注册请求消息，{@code data} 为待注册的 {@link User}
     * @return 注册结果消息：{@code data} 为提示文本
     */
    private Message handleRegister(Message request) {
        try {
            User newUser = (User) request.getData();
            boolean ok = _userServerSrv.register(newUser);
            String statusCode = ok ? IConstant.STATUS_SUCCESS : IConstant.STATUS_ERROR;
            String data = ok ? "注册成功" : "注册失败，请稍后重试";
            return new Message(request.getUid(), IConstant.MSG_REGISTER, MessageType.DATA,
                    statusCode, data, "Server");
        } catch (IllegalArgumentException e) {
            return new Message(request.getUid(), IConstant.MSG_REGISTER, MessageType.DATA,
                    IConstant.STATUS_BAD_REQUEST, e.getMessage(), "Server");
        } catch (UserExistsException e) {
            return new Message(request.getUid(), IConstant.MSG_REGISTER, MessageType.DATA,
                    IConstant.STATUS_USER_EXISTS, e.getMessage(), "Server");
        } catch (SQLException | IOException e) {
            return new Message(request.getUid(), IConstant.MSG_REGISTER, MessageType.DATA,
                    IConstant.STATUS_ERROR, "服务器内部异常：" + e.getMessage(), "Server");
        }
    }

    /**
     * 处理"学生"角色自助注册请求：先按普通流程创建登录账号（状态强制为
     * 待审核），成功后紧接着直接调用学籍模块的业务层 {@link StudentServerSrv}
     * 插入一条对应的学籍记录——学号、一卡通号由服务器根据登录ID自动生成，
     * 不用学生自己填，也不会跟已有数据冲突。
     *
     * <p>注意这里是直接调 {@code StudentServerSrv}，不经过
     * {@link StudentRequestHandler}（那一层的新增学籍是管理员专属操作，
     * 会做权限校验），因为这是注册流程内部触发的，不是学生自己调用了
     * 管理员接口。</p>
     *
     * @param request 请求消息，{@code data} 约定为
     *                {@code Object[]{User newUser, Student profile}}
     * @return 处理结果消息
     */
    private Message handleRegisterStudent(Message request) {
        try {
            Object[] args = (Object[]) request.getData();
            User newUser = (User) args[0];
            Student profile = (Student) args[1];

            boolean userOk = _userServerSrv.register(newUser);
            if (!userOk) {
                return new Message(request.getUid(), IConstant.MSG_REGISTER_STUDENT, MessageType.DATA,
                        IConstant.STATUS_ERROR, "注册失败，请稍后重试", "Server");
            }

            profile.setUserId(newUser.getUId());
            profile.setEnrollmentDate(java.time.LocalDate.now());
            if (profile.getStatus() == null) {
                profile.setStatus(vcampus.common.vo.StudentStatus.ENROLLED);
            }
            _studentServerSrv.addStudent(profile);

            return new Message(request.getUid(), IConstant.MSG_REGISTER_STUDENT, MessageType.DATA,
                    IConstant.STATUS_SUCCESS, "注册成功，账号和学籍信息都已提交，请等待管理员审核", "Server");
        } catch (IllegalArgumentException | ClassCastException e) {
            return new Message(request.getUid(), IConstant.MSG_REGISTER_STUDENT, MessageType.DATA,
                    IConstant.STATUS_BAD_REQUEST, e.getMessage(), "Server");
        } catch (UserExistsException e) {
            return new Message(request.getUid(), IConstant.MSG_REGISTER_STUDENT, MessageType.DATA,
                    IConstant.STATUS_USER_EXISTS, e.getMessage(), "Server");
        } catch (StudentServiceException e) {
            return new Message(request.getUid(), IConstant.MSG_REGISTER_STUDENT, MessageType.DATA,
                    IConstant.STATUS_ERROR,
                    "账号已创建，但学籍信息保存失败（" + e.getMessage() + "），请联系管理员补录", "Server");
        } catch (SQLException | IOException e) {
            return new Message(request.getUid(), IConstant.MSG_REGISTER_STUDENT, MessageType.DATA,
                    IConstant.STATUS_ERROR, "服务器内部异常：" + e.getMessage(), "Server");
        }
    }

    /**
     * 处理登出请求。
     *
     * @param request 登出请求消息，{@code data} 为当前登录的 {@link User}
     * @return 登出结果消息：{@code data} 为提示文本
     */
    private Message handleLogout(Message request) {
        try {
            User currentUser = (User) request.getData();
            boolean ok = _userServerSrv.logout(currentUser);
            String statusCode = ok ? IConstant.STATUS_SUCCESS : IConstant.STATUS_ERROR;
            String data = ok ? "登出成功" : "登出失败，请稍后重试";
            return new Message(request.getUid(), IConstant.MSG_LOGOUT, MessageType.DATA,
                    statusCode, data, "Server");
        } catch (SQLException | IOException e) {
            return new Message(request.getUid(), IConstant.MSG_LOGOUT, MessageType.DATA,
                    IConstant.STATUS_ERROR, "服务器内部异常：" + e.getMessage(), "Server");
        }
    }

    //===================== 医院模块全部处理器方法（完整保留你的代码）=====================
    private Message handleQueryAllDoctor(Message request) {
        try {
            List<Doctor> list = _hospitalSrv.queryAllDoctor();
            return new Message(request.getUid(), request.getName(), MessageType.DATA,
                    IConstant.STATUS_SUCCESS, list, "Server");
        } catch (SQLException | IOException e) {
            return new Message(request.getUid(), request.getName(), MessageType.DATA,
                    IConstant.STATUS_ERROR, "查询医生失败：" + e.getMessage(), "Server");
        }
    }

    private Message handleQueryDoctorByDept(Message request) {
        try {
            String dept = (String) request.getData();
            List<Doctor> list = _hospitalSrv.queryDoctorByDept(dept);
            return new Message(request.getUid(), request.getName(), MessageType.DATA,
                    IConstant.STATUS_SUCCESS, list, "Server");
        } catch (SQLException | IOException e) {
            return new Message(request.getUid(), request.getName(), MessageType.DATA,
                    IConstant.STATUS_ERROR, "按科室查询失败：" + e.getMessage(), "Server");
        }
    }

    private Message handleAddAppointment(Message request) {
    try {
        Appointment appoint = (Appointment) request.getData();
        boolean success = _hospitalSrv.addAppointment(appoint);
        String code = success ? IConstant.STATUS_SUCCESS : IConstant.STATUS_ERROR;
        String msg = success ? "预约成功" : "预约失败";
        return new Message(request.getUid(), request.getName(), MessageType.DATA, code, msg, "Server");
    } catch (IOException e) {
        //业务提示（预约时间错误/时段冲突）直接把异常消息返回前端
        return new Message(request.getUid(), request.getName(), MessageType.DATA,
                IConstant.STATUS_ERROR, e.getMessage(), "Server");
    } catch (SQLException e) {
        //数据库异常
        return new Message(request.getUid(), request.getName(), MessageType.DATA,
                IConstant.STATUS_ERROR, "数据库异常：" + e.getMessage(), "Server");
    }
}



    private Message handleQueryMyAppointment(Message request) {
        try {
            String userId = (String) request.getData();
            List<Appointment> list = _hospitalSrv.queryMyAppointment(userId);
            return new Message(request.getUid(), request.getName(), MessageType.DATA,
                    IConstant.STATUS_SUCCESS, list, "Server");
        } catch (SQLException | IOException e) {
            return new Message(request.getUid(), request.getName(), MessageType.DATA,
                    IConstant.STATUS_ERROR, "查询预约记录失败：" + e.getMessage(), "Server");
        }
    }

    private Message handleCancelAppointment(Message request) {
        try {
            Object[] arr = (Object[]) request.getData();
            String loginUserId = (String) arr[0];
            String appointId = (String) arr[1];
            boolean ok = _hospitalSrv.cancelAppointment(loginUserId, appointId);
            String code = ok ? IConstant.STATUS_SUCCESS : IConstant.STATUS_APPOINT_CANCEL_FAIL;
            String msg = ok ? "取消预约成功" : "取消预约失败：记录不存在/非本人/状态不可取消";
            return new Message(request.getUid(), request.getName(), MessageType.DATA, code, msg, "Server");
        } catch (SQLException | IOException e) {
            return new Message(request.getUid(), request.getName(), MessageType.DATA,
                    IConstant.STATUS_ERROR, "取消预约异常：" + e.getMessage(), "Server");
        }
    }

    private Message handleQueryAllAppointment(Message request) {
        HospitalAdminReq adminReq = (HospitalAdminReq) request.getData();
        try {
            List<Appointment> list = _hospitalSrv.queryAllAppointment();
            return new Message(request.getUid(), request.getName(), MessageType.DATA,
                    IConstant.STATUS_SUCCESS, list, "Server");
        } catch (SQLException | IOException e) {
            return new Message(request.getUid(), request.getName(), MessageType.DATA,
                    IConstant.STATUS_ERROR, "查询全部预约异常：" + e.getMessage(), "Server");
        }
    }

    private Message handleAddDoctor(Message request) {
        HospitalAdminReq adminReq = (HospitalAdminReq) request.getData();
        Doctor doctor = (Doctor) adminReq.getPayload();
        try {
            boolean ok = _hospitalSrv.addDoctor(doctor);
            String code = ok ? IConstant.STATUS_SUCCESS : IConstant.STATUS_ERROR;
            String msg = ok ? "新增医生成功" : "新增医生失败";
            return new Message(request.getUid(), request.getName(), MessageType.DATA, code, msg, "Server");
        } catch (SQLException | IOException e) {
            return new Message(request.getUid(), request.getName(), MessageType.DATA,
                    IConstant.STATUS_ERROR, "新增医生异常：" + e.getMessage(), "Server");
        }
    }

    private Message handleUpdateDoctor(Message request) {
        HospitalAdminReq adminReq = (HospitalAdminReq) request.getData();
        Doctor doctor = (Doctor) adminReq.getPayload();
        try {
            boolean ok = _hospitalSrv.updateDoctor(doctor);
            String code = ok ? IConstant.STATUS_SUCCESS : IConstant.STATUS_ERROR;
            String msg = ok ? "修改医生成功" : "修改医生失败";
            return new Message(request.getUid(), request.getName(), MessageType.DATA, code, msg, "Server");
        } catch (SQLException | IOException e) {
            return new Message(request.getUid(), request.getName(), MessageType.DATA,
                    IConstant.STATUS_ERROR, "修改医生异常：" + e.getMessage(), "Server");
        }
    }

    private Message handleDeleteDoctor(Message request) {
        HospitalAdminReq adminReq = (HospitalAdminReq) request.getData();
        String doctorId = (String) adminReq.getPayload();
        try {
            boolean ok = _hospitalSrv.deleteDoctor(doctorId);
            String code = ok ? IConstant.STATUS_SUCCESS : IConstant.STATUS_ERROR;
            String msg = ok ? "删除医生成功" : "删除医生失败";
            return new Message(request.getUid(), request.getName(), MessageType.DATA, code, msg, "Server");
        } catch (SQLException | IOException e) {
            return new Message(request.getUid(), request.getName(), MessageType.DATA,
                    IConstant.STATUS_ERROR, "删除医生异常：" + e.getMessage(), "Server");
        }
    }

    private Message handleDeleteCancelAppoint(Message request) {
    try {
        String appointId = (String) request.getData();
        boolean ok = _hospitalSrv.deleteCancelAppointment(appointId);
        String code = ok ? IConstant.STATUS_SUCCESS : IConstant.STATUS_ERROR;
        String msg = ok ? "删除预约记录成功" : "删除失败：仅可删除【已取消】或【已就诊】预约记录";
        return new Message(request.getUid(), request.getName(), MessageType.DATA, code, msg, "Server");
    } catch (SQLException | IOException e) {
        return new Message(request.getUid(), request.getName(), MessageType.DATA,
                IConstant.STATUS_ERROR, "删除预约异常：" + e.getMessage(), "Server");
    }
}


    //【新增处理器：查询没有待就诊预约、可以安全删除的医生】
    private Message handleQueryCanDeleteDoctor(Message request) {
        try {
            List<Doctor> list = _hospitalSrv.queryCanDeleteDoctor();
            return new Message(request.getUid(), request.getName(), MessageType.DATA,
                    IConstant.STATUS_SUCCESS, list, "Server");
        } catch (SQLException | IOException e) {
            return new Message(request.getUid(), request.getName(), MessageType.DATA,
                    IConstant.STATUS_ERROR, "查询可删除医生失败：" + e.getMessage(), "Server");
        }
    }

    /**
     * 处理管理员禁用/启用账号请求。
     *
     * @param request 请求消息，{@code data} 约定为
     *                {@code Object[]{operatorUId, targetUId, newStatus}}
     * @return 操作结果消息
     */
    private Message handleSetUserStatus(Message request) {
        try {
            Object[] args = (Object[]) request.getData();
            String operatorUId = (String) args[0];
            String targetUId = (String) args[1];
            String newStatus = (String) args[2];

            boolean ok = _userServerSrv.setUserStatus(operatorUId, targetUId, newStatus);
            String statusCode = ok ? IConstant.STATUS_SUCCESS : IConstant.STATUS_ERROR;
            String data = ok ? ("已将 " + targetUId + " 的账号状态设为：" + newStatus) : "操作失败，请稍后重试";
            return new Message(request.getUid(), IConstant.MSG_USER_SET_STATUS, MessageType.DATA,
                    statusCode, data, "Server");
        } catch (PermissionDeniedException e) {
            return new Message(request.getUid(), IConstant.MSG_USER_SET_STATUS, MessageType.DATA,
                    IConstant.STATUS_FORBIDDEN, e.getMessage(), "Server");
        } catch (IllegalArgumentException | ClassCastException e) {
            return new Message(request.getUid(), IConstant.MSG_USER_SET_STATUS, MessageType.DATA,
                    IConstant.STATUS_BAD_REQUEST, e.getMessage(), "Server");
        } catch (SQLException | IOException e) {
            return new Message(request.getUid(), IConstant.MSG_USER_SET_STATUS, MessageType.DATA,
                    IConstant.STATUS_ERROR, "服务器内部异常：" + e.getMessage(), "Server");
        }
    }
    
    private Message handlePaperRequest(Message request) {
        return _paperHandler.handle(request);
    }
    /**
     * 处理"查询待审核账号列表"请求，仅管理员可用。
     *
     * @param request 请求消息，{@code data} 为操作者登录ID（{@code String}）
     * @return 操作结果消息：成功时 {@code data} 为 {@code List<User>}
     */
    private Message handleListPendingUsers(Message request) {
        try {
            String operatorUId = (String) request.getData();
            java.util.List<User> pending = _userServerSrv.listPendingUsers(operatorUId);
            return new Message(request.getUid(), IConstant.MSG_USER_LIST_PENDING, MessageType.DATA,
                    IConstant.STATUS_SUCCESS, pending, "Server");
        } catch (PermissionDeniedException e) {
            return new Message(request.getUid(), IConstant.MSG_USER_LIST_PENDING, MessageType.DATA,
                    IConstant.STATUS_FORBIDDEN, e.getMessage(), "Server");
        } catch (IllegalArgumentException | ClassCastException e) {
            return new Message(request.getUid(), IConstant.MSG_USER_LIST_PENDING, MessageType.DATA,
                    IConstant.STATUS_BAD_REQUEST, e.getMessage(), "Server");
        } catch (SQLException | IOException e) {
            return new Message(request.getUid(), IConstant.MSG_USER_LIST_PENDING, MessageType.DATA,
                    IConstant.STATUS_ERROR, "服务器内部异常：" + e.getMessage(), "Server");
        }
    }

    /**
     * 处理 AI 问答请求。
     *
     * @param request 请求消息，{@code data} 为问题文本（{@link String}）
     * @return 响应消息：成功时 {@code data} 为 AI 回答文本，失败时为错误提示
     */
    private Message handleAiAsk(Message request) {
        try {
            String question = (String) request.getData();
            String answer = _aiServerSrv.ask(question);
            return new Message(request.getUid(), IConstant.MSG_AI_ASK, MessageType.DATA,
                    IConstant.STATUS_SUCCESS, answer, "Server");
        } catch (IllegalArgumentException | ClassCastException e) {
            return new Message(request.getUid(), IConstant.MSG_AI_ASK, MessageType.DATA,
                    IConstant.STATUS_BAD_REQUEST, e.getMessage(), "Server");
        } catch (IOException e) {
            return new Message(request.getUid(), IConstant.MSG_AI_ASK, MessageType.DATA,
                    IConstant.STATUS_ERROR, e.getMessage(), "Server");
        }
    }

    /**
     * 请求处理器函数式接口。
     */
    @FunctionalInterface
    private interface RequestHandler {
        /**
         * 处理请求。
         *
         * @param request 请求消息
         * @return 响应消息
         */
        Message handle(Message request);
    }

    private Message handleGetDoctorOccupiedTime(Message request) {
    try {
        String doctorId = (String) request.getData();
        List<java.sql.Timestamp> tsList = _hospitalSrv.getDoctorOccupiedTime(doctorId);
        return new Message(request.getUid(), request.getName(), MessageType.DATA,
                IConstant.STATUS_SUCCESS, tsList, "Server");
    } catch (SQLException | IOException e) {
        return new Message(request.getUid(), request.getName(), MessageType.DATA,
                IConstant.STATUS_ERROR, "查询医生占用时段异常：" + e.getMessage(), "Server");
    }
}



}
