/*
 * ServerThread
 *
 * Version 1.0
 *
 * 2026-08-31
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.server.srv;

import vcampus.common.constant.IConstant;
import vcampus.common.vo.Appointment;
import vcampus.common.vo.Doctor;
import vcampus.common.vo.Message;
import vcampus.common.vo.MessageType;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.sql.SQLException;
import java.util.List;

/**
 * 服务线程类：{@link Server} 每接受一个客户端连接，就创建一个 ServerThread
 * 交给独立线程运行，实现"一个客户端一个线程"的多线程模型。
 * <p>本版本（hospital分支）仅实现医院模块接口，用户模块待合并。</p>
 */
public class ServerThread implements Runnable {
    /** 与客户端建立的连接。 */
    private final Socket _socket;
    /** 医院业务服务 */
    private final IHospitalServerSrv _hospitalSrv = new HospitalServerSrv();

    /**
     * 构造方法。
     *
     * @param socket 已经与客户端建立好的连接
     */
    public ServerThread(Socket socket) {
        this._socket = socket;
    }

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

    private Message handleRequest(Message request) {
        String name = request.getName();
        if (IConstant.MSG_HOSPITAL_QUERY_ALL_DOCTOR.equals(name)) {
            return handleQueryAllDoctor(request);
        } else if (IConstant.MSG_HOSPITAL_QUERY_DOCTOR_BY_DEPT.equals(name)) {
            return handleQueryDoctorByDept(request);
        } else if (IConstant.MSG_HOSPITAL_ADD_APPOINTMENT.equals(name)) {
            return handleAddAppointment(request);
        } else if (IConstant.MSG_HOSPITAL_QUERY_MY_APPOINTMENT.equals(name)) {
            return handleQueryMyAppointment(request);
        } else if (IConstant.MSG_HOSPITAL_CANCEL_APPOINTMENT.equals(name)) {
            return handleCancelAppointment(request);
        } else {
            return new Message(request.getUid(), name, MessageType.DATA,
                    IConstant.STATUS_ERROR, "未知的请求类型：" + name, "Server");
        }
    }

    // ====================== 医院模块处理方法 ======================
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
            String appointId = (String) request.getData();
            boolean ok = _hospitalSrv.cancelAppointment(appointId);
            String code = ok ? IConstant.STATUS_SUCCESS : IConstant.STATUS_APPOINT_CANCEL_FAIL;
            String msg = ok ? "取消预约成功" : "取消预约失败，记录不存在";
            return new Message(request.getUid(), request.getName(), MessageType.DATA, code, msg, "Server");
        } catch (SQLException | IOException e) {
            return new Message(request.getUid(), request.getName(), MessageType.DATA,
                    IConstant.STATUS_ERROR, "取消预约异常：" + e.getMessage(), "Server");
        }
    }
}
