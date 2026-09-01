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
import vcampus.common.vo.Goods;
import vcampus.common.vo.Message;
import vcampus.common.vo.MessageType;
import vcampus.common.vo.PurchaseRecord;
import vcampus.common.vo.ShopRequest;
import vcampus.common.vo.User;

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

    /** 商店业务服务，由本线程独立持有，避免多线程共享状态。 */
    private final IStoreServerSrv _storeServerSrv = new StoreServerSrv();

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
     * 注册请求处理器。
     */
    private void registerHandlers() {
        _handlerMap.put(IConstant.MSG_LOGIN, this::handleLogin);
        _handlerMap.put(IConstant.MSG_REGISTER, this::handleRegister);
        _handlerMap.put(IConstant.MSG_LOGOUT, this::handleLogout);
        _handlerMap.put(IConstant.MSG_SHOP_QUERY_GOODS, this::handleShopQueryGoods);
        _handlerMap.put(IConstant.MSG_SHOP_PURCHASE, this::handleShopPurchase);
        _handlerMap.put(IConstant.MSG_SHOP_QUERY_RECORDS, this::handleShopQueryRecords);
        _handlerMap.put(IConstant.MSG_SHOP_ADD_GOODS, this::handleShopAddGoods);
        _handlerMap.put(IConstant.MSG_SHOP_UPDATE_GOODS, this::handleShopUpdateGoods);
        _handlerMap.put(IConstant.MSG_SHOP_DELETE_GOODS, this::handleShopDeleteGoods);
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
     * 处理商店"查询商品"请求。
     *
     * @param request 查询请求消息，{@code data} 为 {@link ShopRequest}（含关键字/类别）
     * @return 查询结果消息：成功时 {@code data} 为 {@code List<Goods>}
     */
    private Message handleShopQueryGoods(Message request) {
        try {
            ShopRequest req = (ShopRequest) request.getData();
            List<Goods> goods = _storeServerSrv.queryGoods(req.getKeyword(), req.getCategory());
            return new Message(request.getUid(), IConstant.MSG_SHOP_QUERY_GOODS, MessageType.DATA,
                    IConstant.STATUS_SUCCESS, goods, "Server");
        } catch (SQLException | IOException e) {
            return new Message(request.getUid(), IConstant.MSG_SHOP_QUERY_GOODS, MessageType.DATA,
                    IConstant.STATUS_ERROR, "服务器内部异常：" + e.getMessage(), "Server");
        }
    }

    /**
     * 处理商店"购买商品"请求。
     *
     * @param request 购买请求消息，{@code data} 为 {@link ShopRequest}（含购买人/商品/数量）
     * @return 购买结果消息：成功时 {@code data} 为 {@link PurchaseRecord}
     */
    private Message handleShopPurchase(Message request) {
        try {
            ShopRequest req = (ShopRequest) request.getData();
            PurchaseRecord record = _storeServerSrv.purchaseGoods(req.getUserId(), req.getGoodsId(), req.getQuantity());
            return new Message(request.getUid(), IConstant.MSG_SHOP_PURCHASE, MessageType.DATA,
                    IConstant.STATUS_SUCCESS, record, "Server");
        } catch (ShopException e) {
            return new Message(request.getUid(), IConstant.MSG_SHOP_PURCHASE, MessageType.DATA,
                    e.getStatusCode(), e.getMessage(), "Server");
        } catch (SQLException | IOException e) {
            return new Message(request.getUid(), IConstant.MSG_SHOP_PURCHASE, MessageType.DATA,
                    IConstant.STATUS_ERROR, "服务器内部异常：" + e.getMessage(), "Server");
        }
    }

    /**
     * 处理商店"查询购买记录"请求。
     *
     * @param request 查询记录请求，{@code data} 为 {@link ShopRequest}（含 userId）
     * @return 查询结果消息：成功时 {@code data} 为 {@code List<PurchaseRecord>}
     */
    private Message handleShopQueryRecords(Message request) {
        try {
            ShopRequest req = (ShopRequest) request.getData();
            List<PurchaseRecord> records = _storeServerSrv.queryPurchaseRecords(req.getUserId());
            return new Message(request.getUid(), IConstant.MSG_SHOP_QUERY_RECORDS, MessageType.DATA,
                    IConstant.STATUS_SUCCESS, records, "Server");
        } catch (SQLException | IOException e) {
            return new Message(request.getUid(), IConstant.MSG_SHOP_QUERY_RECORDS, MessageType.DATA,
                    IConstant.STATUS_ERROR, "服务器内部异常：" + e.getMessage(), "Server");
        }
    }

    /**
     * 处理商店"新增商品"请求（管理员）。
     *
     * @param request 新增商品请求，{@code data} 为 {@link ShopRequest}（含商品对象）
     * @return 新增结果消息：成功时 {@code data} 为 {@link Goods}
     */
    private Message handleShopAddGoods(Message request) {
        try {
            ShopRequest req = (ShopRequest) request.getData();
            Goods goods = _storeServerSrv.addGoods(req.getGoods());
            return new Message(request.getUid(), IConstant.MSG_SHOP_ADD_GOODS, MessageType.DATA,
                    IConstant.STATUS_SUCCESS, goods, "Server");
        } catch (ShopException e) {
            return new Message(request.getUid(), IConstant.MSG_SHOP_ADD_GOODS, MessageType.DATA,
                    e.getStatusCode(), e.getMessage(), "Server");
        } catch (SQLException | IOException e) {
            return new Message(request.getUid(), IConstant.MSG_SHOP_ADD_GOODS, MessageType.DATA,
                    IConstant.STATUS_ERROR, "服务器内部异常：" + e.getMessage(), "Server");
        }
    }

    /**
     * 处理商店"修改商品"请求（管理员）。
     *
     * @param request 修改商品请求，{@code data} 为 {@link ShopRequest}（含商品对象）
     * @return 修改结果消息：成功时 {@code data} 为 {@link Goods}
     */
    private Message handleShopUpdateGoods(Message request) {
        try {
            ShopRequest req = (ShopRequest) request.getData();
            Goods goods = _storeServerSrv.updateGoods(req.getGoods());
            return new Message(request.getUid(), IConstant.MSG_SHOP_UPDATE_GOODS, MessageType.DATA,
                    IConstant.STATUS_SUCCESS, goods, "Server");
        } catch (ShopException e) {
            return new Message(request.getUid(), IConstant.MSG_SHOP_UPDATE_GOODS, MessageType.DATA,
                    e.getStatusCode(), e.getMessage(), "Server");
        } catch (SQLException | IOException e) {
            return new Message(request.getUid(), IConstant.MSG_SHOP_UPDATE_GOODS, MessageType.DATA,
                    IConstant.STATUS_ERROR, "服务器内部异常：" + e.getMessage(), "Server");
        }
    }

    /**
     * 处理商店"删除商品"请求（管理员）。
     *
     * @param request 删除商品请求，{@code data} 为 {@link ShopRequest}（含 goodsId）
     * @return 删除结果消息：成功时 {@code data} 为提示文本
     */
    private Message handleShopDeleteGoods(Message request) {
        try {
            ShopRequest req = (ShopRequest) request.getData();
            boolean ok = _storeServerSrv.deleteGoods(req.getGoodsId());
            return new Message(request.getUid(), IConstant.MSG_SHOP_DELETE_GOODS, MessageType.DATA,
                    ok ? IConstant.STATUS_SUCCESS : IConstant.STATUS_ERROR, ok ? "删除成功" : "删除失败", "Server");
        } catch (ShopException e) {
            return new Message(request.getUid(), IConstant.MSG_SHOP_DELETE_GOODS, MessageType.DATA,
                    e.getStatusCode(), e.getMessage(), "Server");
        } catch (SQLException | IOException e) {
            return new Message(request.getUid(), IConstant.MSG_SHOP_DELETE_GOODS, MessageType.DATA,
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
