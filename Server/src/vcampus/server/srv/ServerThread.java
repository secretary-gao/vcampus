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
import vcampus.common.constant.StudentProtocol;
import vcampus.common.vo.Message;
import vcampus.common.vo.MessageType;
import vcampus.common.vo.User;

import vcampus.server.srv.Library.LibraryHandler;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * 服务线程类：{@link Server} 每接受一个客户端连接，就创建一个 ServerThread
 * 交给独立线程运行，实现"一个客户端一个线程"的多线程模型。
 *
 * <p>
 * 本线程只处理一次请求/响应：读取客户端发来的一个 {@link Message}，
 * 根据 {@code Message.getName()} 通过可扩展的处理器表进行分发，调用
 * {@link UserServerSrv} 完成业务处理后，把结果封装成响应 {@link Message}
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

    /** 用户业务服务，由本线程独立持有，避免多线程共享状态。 */
    private final IUserServerSrv _userServerSrv = new UserServerSrv();

    /** 图书馆模块业务服务，由本线程独立持有，避免多线程共享状态。 */
    private final LibraryHandler _libraryHandler = new LibraryHandler();

    /** 学籍模块请求处理器，由统一服务器负责分发请求。 */
    private final StudentRequestHandler _studentRequestHandler = new StudentRequestHandler();

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
     */
    private void registerHandlers() {
        _handlerMap.put(IConstant.MSG_LOGIN, this::handleLogin);
        _handlerMap.put(IConstant.MSG_REGISTER, this::handleRegister);
        _handlerMap.put(IConstant.MSG_LOGOUT, this::handleLogout);
        _handlerMap.put(IConstant.MSG_QUERY_BOOKS, this::handleLibraryRequest);
        _handlerMap.put(IConstant.MSG_BORROW_BOOK, this::handleLibraryRequest);
        _handlerMap.put(IConstant.MSG_RETURN_BOOK, this::handleLibraryRequest);
        _handlerMap.put(IConstant.MSG_GET_BORROW_RECORDS, this::handleLibraryRequest);
        _handlerMap.put(StudentProtocol.LIST, this::handleStudentRequest);
        _handlerMap.put(StudentProtocol.GET_SELF, this::handleStudentRequest);
        _handlerMap.put(StudentProtocol.QUERY_BY_ID, this::handleStudentRequest);
        _handlerMap.put(StudentProtocol.QUERY_BY_CARD, this::handleStudentRequest);
        _handlerMap.put(StudentProtocol.QUERY_BY_NAME, this::handleStudentRequest);
        _handlerMap.put(StudentProtocol.ADD, this::handleStudentRequest);
        _handlerMap.put(StudentProtocol.UPDATE, this::handleStudentRequest);
        _handlerMap.put(StudentProtocol.DELETE, this::handleStudentRequest);
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
