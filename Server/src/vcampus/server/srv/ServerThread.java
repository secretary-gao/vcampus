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
import vcampus.common.vo.User;
import vcampus.server.srv.Library.LibraryHandler;

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

    /** 学籍模块请求处理器，由统一服务器负责分发请求。 */
    private final StudentRequestHandler _studentRequestHandler = new StudentRequestHandler();

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

        // ========= 学籍模块 =========
        _handlerMap.put(StudentProtocol.LIST, this::handleStudentRequest);
        _handlerMap.put(StudentProtocol.QUERY_BY_ID, this::handleStudentRequest);
        _handlerMap.put(StudentProtocol.QUERY_BY_CARD, this::handleStudentRequest);
        _handlerMap.put(StudentProtocol.QUERY_BY_NAME, this::handleStudentRequest);
        _handlerMap.put(StudentProtocol.ADD, this::handleStudentRequest);
        _handlerMap.put(StudentProtocol.UPDATE, this::handleStudentRequest);
        _handlerMap.put(StudentProtocol.DELETE, this::handleStudentRequest);
        _handlerMap.put(IConstant.MSG_USER_SET_STATUS, this::handleSetUserStatus);
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
        } catch (SQLException | IOException e) {
            return new Message(request.getUid(), request.getName(), MessageType.DATA,
                    IConstant.STATUS_ERROR, "新增预约异常：" + e.getMessage(), "Server");
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
            String msg = ok ? "删除已取消预约成功" : "删除失败：仅可删除状态为【已取消】的预约";
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
}
