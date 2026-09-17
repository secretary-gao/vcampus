/*
 * AIClientSrv
 *
 * Version 1.0
 *
 * 2026-09-07
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.client.biz;

import vcampus.common.constant.IConstant;
import vcampus.common.vo.Message;
import vcampus.common.vo.MessageType;
import vcampus.common.vo.User;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * {@link IAIClientSrv} 的实现类，做法和 {@link UserClientSrv} 一样：每次
 * 提问新建一个 Socket 连接，发一个请求、收一个响应后立即关闭。
 */
public class AIClientSrv implements IAIClientSrv {

    /**
     * {@inheritDoc}
     */
    @Override
    public Message ask(String question) throws IOException, ClassNotFoundException {
        return ask(question, null);
    }

    /** 携带当前登录用户上下文，供校园 AI 做学籍相关的个性化回答。 */
    public Message ask(String question, User currentUser)
            throws IOException, ClassNotFoundException {
        Object data = currentUser == null ? question : new Object[] {question, currentUser};
        Message request = new Message(System.currentTimeMillis(), IConstant.MSG_AI_ASK,
                MessageType.COMMAND, null, data, currentUser);
        return sendAndReceive(request);
    }

    /**
     * 建立连接、发送请求、读取响应、关闭连接的通用流程，见
     * {@link UserClientSrv#sendAndReceive} 对流顺序的说明。
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
