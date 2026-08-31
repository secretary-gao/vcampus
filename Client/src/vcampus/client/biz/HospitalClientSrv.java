/*
 * HospitalClientSrv
 *
 * Version 1.0
 *
 * 2026-08-31
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.client.biz;

import vcampus.common.constant.IConstant;
import vcampus.common.vo.Appointment;
import vcampus.common.vo.Doctor;
import vcampus.common.vo.Message;
import vcampus.common.vo.MessageType;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * {@link IHospitalClientSrv} 的实现类，封装Socket请求逻辑，
 * 组装消息发送给服务端，并返回原始响应Message对象。
 */
public class HospitalClientSrv implements IHospitalClientSrv {

    /**
     * 通用发送消息工具方法：建立连接、收发Message
     * @param request 请求消息
     * @return 服务端返回的响应消息
     * @throws IOException IO异常
     * @throws ClassNotFoundException 反序列化异常
     */
    private Message sendMessage(Message request) throws IOException, ClassNotFoundException {
        try (Socket socket = new Socket(IConstant.SERVER_HOST, IConstant.SERVER_PORT);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
            out.flush();
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            out.writeObject(request);
            out.flush();
            return (Message) in.readObject();
        }
    }

    @Override
    public Message queryAllDoctor() throws IOException, ClassNotFoundException {
        Long uid = System.currentTimeMillis();
        Message req = new Message(uid, IConstant.MSG_HOSPITAL_QUERY_ALL_DOCTOR,
                MessageType.DATA, null, null, "Client");
        return sendMessage(req);
    }

    @Override
    public Message queryDoctorByDept(String department) throws IOException, ClassNotFoundException {
        Long uid = System.currentTimeMillis();
        Message req = new Message(uid, IConstant.MSG_HOSPITAL_QUERY_DOCTOR_BY_DEPT,
                MessageType.DATA, null, department, "Client");
        return sendMessage(req);
    }

    @Override
    public Message addAppointment(Appointment appoint) throws IOException, ClassNotFoundException {
        Long uid = System.currentTimeMillis();
        Message req = new Message(uid, IConstant.MSG_HOSPITAL_ADD_APPOINTMENT,
                MessageType.DATA, null, appoint, "Client");
        return sendMessage(req);
    }

    @Override
    public Message queryMyAppointment(String userId) throws IOException, ClassNotFoundException {
        Long uid = System.currentTimeMillis();
        Message req = new Message(uid, IConstant.MSG_HOSPITAL_QUERY_MY_APPOINTMENT,
                MessageType.DATA, null, userId, "Client");
        return sendMessage(req);
    }

    @Override
    public Message cancelAppointment(String appointId) throws IOException, ClassNotFoundException {
        Long uid = System.currentTimeMillis();
        Message req = new Message(uid, IConstant.MSG_HOSPITAL_CANCEL_APPOINTMENT,
                MessageType.DATA, null, appointId, "Client");
        return sendMessage(req);
    }
}
