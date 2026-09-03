/*
 * ServerThread
 *
 * Version 1.0
 *
 * 2026-08-30
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.server.srv;
import vcampus.common.constant.IConstant;
import vcampus.common.vo.Appointment;
import vcampus.common.vo.Doctor;
import vcampus.common.vo.HospitalAdminReq;
import vcampus.common.vo.Message;
import vcampus.common.vo.MessageType;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**
 * 服务线程类：{@link Server} 每接受一个客户端连接，就创建一个 ServerThread
 * 交给独立线程运行，实现"一个客户端一个线程"的多线程模型。
 *
 * <p>
 * 本线程只处理一次请求/响应：读取客户端发来的一个 {@link Message}，
 * 根据 {@code Message.getName()} 通过可扩展的处理器表进行分发，调用
 * 业务服务完成处理后，把结果封装成响应 {@link Message}
 * 写回客户端，随后关闭连接、线程结束。
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
    /** 医院模块业务服务，由本线程独立持有，避免多线程共享状态。 */
    private final IHospitalServerSrv _hospitalSrv = new HospitalServerSrv();
    /** 请求处理器注册表。 */
    private final Map<String, RequestHandler> _handlerMap = new HashMap<>();
    /**
     * 构造方法。
     *
     * @param socket 已经与客户端建立好的连接
     */
    public ServerThread(Socket socket) {
        this._socket = socket;
        registerHandlers();
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
        } else {
            return new Message(request.getUid(), name, MessageType.DATA,
                    IConstant.STATUS_ERROR, "未知的请求类型：" + name, "Server");
        }
    }
    /**
     * 注册请求处理器。各模块按 {@code Message.getName()} 的取值把自己的处理
     * 方法注册进来，新增模块时只需在这里加一行，不用改 {@link #handleRequest}。
     */
    private void registerHandlers() {
        // ===================== 医院模块：只在这里追加注册行，其他不动 =====================
        _handlerMap.put(IConstant.MSG_HOSPITAL_QUERY_ALL_DOCTOR, this::handleQueryAllDoctor);
        _handlerMap.put(IConstant.MSG_HOSPITAL_QUERY_DOCTOR_BY_DEPT, this::handleQueryDoctorByDept);
        _handlerMap.put(IConstant.MSG_HOSPITAL_ADD_APPOINTMENT, this::handleAddAppointment);
        _handlerMap.put(IConstant.MSG_HOSPITAL_QUERY_MY_APPOINTMENT, this::handleQueryMyAppointment);
        _handlerMap.put(IConstant.MSG_HOSPITAL_CANCEL_APPOINTMENT, this::handleCancelAppointment);
        _handlerMap.put(IConstant.MSG_HOSPITAL_QUERY_ALL_APPOINTMENT, this::handleQueryAllAppointment);
        _handlerMap.put(IConstant.MSG_HOSPITAL_ADD_DOCTOR, this::handleAddDoctor);
        _handlerMap.put(IConstant.MSG_HOSPITAL_UPDATE_DOCTOR, this::handleUpdateDoctor);
        _handlerMap.put(IConstant.MSG_HOSPITAL_DELETE_DOCTOR, this::handleDeleteDoctor);
    }

    // ====================== 医院模块普通用户处理方法 ======================
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
    // ====================== 医院模块【管理员】处理方法 ======================
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
    /**
     * 请求处理器函数式接口。
     */
    @FunctionalInterface
    private interface RequestHandler {
        Message handle(Message request);
    }
}
