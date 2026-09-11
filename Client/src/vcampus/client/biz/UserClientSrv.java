/*
 * UserClientSrv
 *
 * Version 1.0
 *
 * 2026-08-30
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.client.biz;

import vcampus.common.constant.IConstant;
import vcampus.common.vo.Message;
import vcampus.common.vo.MessageType;
import vcampus.common.vo.Student;
import vcampus.common.vo.User;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * {@link IUserClientSrv} 的实现类：每次调用都新建一个 Socket 连接到服务器
 * （见 {@link IConstant#SERVER_HOST}、{@link IConstant#SERVER_PORT}），
 * 发送一个请求 {@link Message}、接收一个响应 {@link Message} 后立即关闭连接。
 * 这种"一次请求一条连接"的方式对登录/注册这种低频操作足够简单可靠；
 * 后续如果有需要保持长连接的场景（如实时通知），可以再扩展。
 */
public class UserClientSrv implements IUserClientSrv {

    /**
     * {@inheritDoc}
     */
    @Override
    public Message login(User loginUser) throws IOException, ClassNotFoundException {
        Message request = new Message(System.currentTimeMillis(), IConstant.MSG_LOGIN,
                MessageType.COMMAND, null, loginUser, loginUser.getUId());
        return sendAndReceive(request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Message register(User newUser) throws IOException, ClassNotFoundException {
        Message request = new Message(System.currentTimeMillis(), IConstant.MSG_REGISTER,
                MessageType.COMMAND, null, newUser, newUser.getUId());
        return sendAndReceive(request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Message logout(User currentUser) throws IOException, ClassNotFoundException {
        Message request = new Message(System.currentTimeMillis(), IConstant.MSG_LOGOUT,
                MessageType.COMMAND, null, currentUser, currentUser == null ? null : currentUser.getUId());
        return sendAndReceive(request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Message setUserStatus(String operatorUId, String targetUId, String newStatus)
            throws IOException, ClassNotFoundException {
        Object[] payload = new Object[] {operatorUId, targetUId, newStatus};
        Message request = new Message(System.currentTimeMillis(), IConstant.MSG_USER_SET_STATUS,
                MessageType.COMMAND, null, payload, operatorUId);
        return sendAndReceive(request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Message listPendingUsers(String operatorUId) throws IOException, ClassNotFoundException {
        Message request = new Message(System.currentTimeMillis(), IConstant.MSG_USER_LIST_PENDING,
                MessageType.COMMAND, null, operatorUId, operatorUId);
        return sendAndReceive(request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Message registerStudent(User newUser, Student profile) throws IOException, ClassNotFoundException {
        Object[] payload = new Object[] {newUser, profile};
        Message request = new Message(System.currentTimeMillis(), IConstant.MSG_REGISTER_STUDENT,
                MessageType.COMMAND, null, payload, newUser == null ? null : newUser.getUId());
        return sendAndReceive(request);
    }

    /**
     * 建立连接、发送请求、读取响应、关闭连接的通用流程。
     *
     * <p>
     * 注意：{@link ObjectOutputStream} 必须在 {@link ObjectInputStream}
     * 之前创建并 flush（发送流头），否则会和服务器端相互等待卡死——服务器端
     * 遵循同样的顺序，见 {@code vcampus.server.srv.ServerThread}。
     * </p>
     *
     * @param request 请求消息
     * @return 服务器返回的响应消息
     * @throws IOException            网络连接异常
     * @throws ClassNotFoundException 反序列化响应对象失败
     */
    private Message sendAndReceive(Message request) throws IOException, ClassNotFoundException {
        try (Socket socket = new Socket(IConstant.SERVER_HOST, IConstant.SERVER_PORT);
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
            out.flush();
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

            out.writeObject(request);
            out.flush();

            return (Message) in.readObject();
        }
    }
}
