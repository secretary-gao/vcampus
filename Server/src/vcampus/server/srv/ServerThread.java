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
import vcampus.common.vo.Message;
import vcampus.common.vo.MessageType;
import vcampus.common.vo.User;

import vcampus.server.srv.Library.LibraryHandler;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.sql.SQLException;

/**
 * 服务线程类：{@link Server} 每接受一个客户端连接，就创建一个 ServerThread
 * 交给独立线程运行，实现"一个客户端一个线程"的多线程模型。
 *
 * <p>本线程只处理一次请求/响应：读取客户端发来的一个 {@link Message}，
 * 根据 {@code Message.getName()} 判断是登录还是注册，调用
 * {@link UserServerSrv} 完成业务处理后，把结果封装成响应 {@link Message}
 * 写回客户端，随后关闭连接、线程结束。</p>
 *
 * <p>注意：无论先读还是先写，{@link ObjectOutputStream} 都必须在
 * {@link ObjectInputStream} 之前创建并 flush，否则两端会互相等待对方的
 * 流头信息而卡死（这是 Java 对象序列化流的一个经典坑）。</p>
 */
public class ServerThread implements Runnable {

    /** 与客户端建立的连接。 */
    private final Socket _socket;

    /** 用户业务服务，由本线程独立持有，避免多线程共享状态。 */
    private final IUserServerSrv _userServerSrv = new UserServerSrv();

    private final LibraryHandler _libraryHandler = new LibraryHandler();
    /**
     * 构造方法。
     *
     * @param socket 已经与客户端建立好的连接
     */
    public ServerThread(Socket socket) {
        this._socket = socket;
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
        if (IConstant.MSG_LOGIN.equals(name)) {
            return handleLogin(request);
        } else if (IConstant.MSG_REGISTER.equals(name)) {
            return handleRegister(request);
        } else if (IConstant.MSG_QUERY_BOOKS.equals(name) ||
               IConstant.MSG_BORROW_BOOK.equals(name) ||
               IConstant.MSG_RETURN_BOOK.equals(name) ||
               IConstant.MSG_GET_BORROW_RECORDS.equals(name)) {
            return handleLibraryRequest(request);
        }
        else {
            return new Message(request.getUid(), name, MessageType.DATA,
                    IConstant.STATUS_ERROR, "未知的请求类型：" + name, "Server");
        }
    }
    private Message handleLibraryRequest(Message request) {
        return _libraryHandler.handle(request);
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
        } catch (UserExistsException e) {
            return new Message(request.getUid(), IConstant.MSG_REGISTER, MessageType.DATA,
                    IConstant.STATUS_USER_EXISTS, e.getMessage(), "Server");
        } catch (SQLException | IOException e) {
            return new Message(request.getUid(), IConstant.MSG_REGISTER, MessageType.DATA,
                    IConstant.STATUS_ERROR, "服务器内部异常：" + e.getMessage(), "Server");
        }
    }
}
