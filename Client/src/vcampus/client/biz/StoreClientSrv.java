/*
 * StoreClientSrv
 *
 * Version 1.0
 *
 * 2026-09-01
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.client.biz;

import vcampus.common.constant.IConstant;
import vcampus.common.vo.CartItem;
import vcampus.common.vo.Goods;
import vcampus.common.vo.Message;
import vcampus.common.vo.MessageType;
import vcampus.common.vo.ShopRequest;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.net.Socket;
import java.util.List;

/**
 * {@link IStoreClientSrv} 的实现类：每次调用都新建一个 Socket 连接到服务器
 * （见 {@link IConstant#SERVER_HOST}、{@link IConstant#SERVER_PORT}），发送一个
 * 请求 {@link Message}、接收一个响应 {@link Message} 后立即关闭连接。
 *
 * <p>购买/查询等操作的参数统一打包进 {@link ShopRequest}，放入
 * {@code Message.data} 一起随报文发送。</p>
 */
public class StoreClientSrv implements IStoreClientSrv {

    /**
     * {@inheritDoc}
     */
    @Override
    public Message queryGoods(String keyword, String category) throws IOException, ClassNotFoundException {
        ShopRequest req = new ShopRequest();
        req.setKeyword(keyword);
        req.setCategory(category);
        return sendAndReceive(new Message(System.currentTimeMillis(),
                IConstant.MSG_SHOP_QUERY_GOODS, MessageType.COMMAND, null, req, "store-client"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Message purchaseGoods(String userId, String goodsId, int quantity) throws IOException, ClassNotFoundException {
        ShopRequest req = new ShopRequest();
        req.setUserId(userId);
        req.setGoodsId(goodsId);
        req.setQuantity(quantity);
        return sendAndReceive(new Message(System.currentTimeMillis(),
                IConstant.MSG_SHOP_PURCHASE, MessageType.COMMAND, null, req, userId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Message queryPurchaseRecords(String userId) throws IOException, ClassNotFoundException {
        ShopRequest req = new ShopRequest();
        req.setUserId(userId);
        return sendAndReceive(new Message(System.currentTimeMillis(),
                IConstant.MSG_SHOP_QUERY_RECORDS, MessageType.COMMAND, null, req, userId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Message addGoods(Goods goods) throws IOException, ClassNotFoundException {
        ShopRequest req = new ShopRequest();
        req.setGoods(goods);
        return sendAndReceive(new Message(System.currentTimeMillis(),
                IConstant.MSG_SHOP_ADD_GOODS, MessageType.COMMAND, null, req, "store-admin"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Message updateGoods(Goods goods) throws IOException, ClassNotFoundException {
        ShopRequest req = new ShopRequest();
        req.setGoods(goods);
        return sendAndReceive(new Message(System.currentTimeMillis(),
                IConstant.MSG_SHOP_UPDATE_GOODS, MessageType.COMMAND, null, req, "store-admin"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Message deleteGoods(String goodsId) throws IOException, ClassNotFoundException {
        ShopRequest req = new ShopRequest();
        req.setGoodsId(goodsId);
        return sendAndReceive(new Message(System.currentTimeMillis(),
                IConstant.MSG_SHOP_DELETE_GOODS, MessageType.COMMAND, null, req, "store-admin"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Message queryBalance(String userId) throws IOException, ClassNotFoundException {
        ShopRequest req = new ShopRequest();
        req.setUserId(userId);
        return sendAndReceive(new Message(System.currentTimeMillis(),
                IConstant.MSG_SHOP_QUERY_BALANCE, MessageType.COMMAND, null, req, userId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Message recharge(String userId, BigDecimal amount) throws IOException, ClassNotFoundException {
        ShopRequest req = new ShopRequest();
        req.setUserId(userId);
        req.setAmount(amount);
        return sendAndReceive(new Message(System.currentTimeMillis(),
                IConstant.MSG_SHOP_RECHARGE, MessageType.COMMAND, null, req, userId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Message checkout(String userId, List<CartItem> items) throws IOException, ClassNotFoundException {
        ShopRequest req = new ShopRequest();
        req.setUserId(userId);
        req.setItems(items);
        return sendAndReceive(new Message(System.currentTimeMillis(),
                IConstant.MSG_SHOP_CHECKOUT, MessageType.COMMAND, null, req, userId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Message queryOrders(String userId) throws IOException, ClassNotFoundException {
        ShopRequest req = new ShopRequest();
        req.setUserId(userId);
        return sendAndReceive(new Message(System.currentTimeMillis(),
                IConstant.MSG_SHOP_QUERY_ORDERS, MessageType.COMMAND, null, req, userId));
    }

    /**
     * 建立连接、发送请求、读取响应、关闭连接的通用流程。
     *
     * <p>注意：{@link ObjectOutputStream} 必须在 {@link ObjectInputStream}
     * 之前创建并 flush（发送流头），否则会和服务器端相互等待卡死——服务器端
     * 遵循同样的顺序，见 {@code vcampus.server.srv.ServerThread}。</p>
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
