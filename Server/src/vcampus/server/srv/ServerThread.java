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

public class ServerThread implements Runnable {
    private final Socket _socket;
    private final IHospitalServerSrv _hospitalSrv = new HospitalServerSrv();
    private final Map<String, RequestHandler> _handlerMap = new HashMap<>();

    public ServerThread(Socket socket) {
        this._socket = socket;
        registerHandlers();
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
        RequestHandler handler = _handlerMap.get(name);
        if (handler != null) {
            return handler.handle(request);
        } else {
            return new Message(request.getUid(), name, MessageType.DATA,
                    IConstant.STATUS_ERROR, "未知的请求类型：" + name, "Server");
        }
    }

    private void registerHandlers() {
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
        _handlerMap.put(IConstant.MSG_HOSPITAL_DELETE_CANCEL_APPOINT,this::handleDeleteCancelAppoint);

    }

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

    @FunctionalInterface
    private interface RequestHandler {
        Message handle(Message request);
    }
}
