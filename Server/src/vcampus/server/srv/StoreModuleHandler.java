/*
 * StoreModuleHandler
 *
 * Version 1.0
 *
 * 2026-09-01
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.server.srv;

import vcampus.common.constant.IConstant;
import vcampus.common.vo.Goods;
import vcampus.common.vo.Message;
import vcampus.common.vo.MessageType;
import vcampus.common.vo.Order;
import vcampus.common.vo.PurchaseRecord;
import vcampus.common.vo.ShopRequest;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

/**
 * 虚拟商店模块的服务器端请求处理器：把{@link ServerThread} 中的商店逻辑
 * 独立出来。该处理器声明了商店模块支持的消息名，并负责对这些请求做业务
 * 分发、调用 {@link StoreServerSrv} 完成数据处理后再封装成响应消息。
 *
 * <p>通过 {@link ModuleHandler} 接口接入，ServerThread 只需在启动时把
 * 本处理器加入处理器列表即可，不再需要在共享文件里写商店相关的 if-else
 * 或注册行，从源头减小多人并发修改 ServerThread 的冲突面。</p>
 */
public class StoreModuleHandler implements ModuleHandler {

    /** 商店业务服务。 */
    private final IStoreServerSrv _storeServerSrv = new StoreServerSrv();

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> supportedMessages() {
        return Set.of(
                IConstant.MSG_SHOP_QUERY_GOODS,
                IConstant.MSG_SHOP_PURCHASE,
                IConstant.MSG_SHOP_QUERY_RECORDS,
                IConstant.MSG_SHOP_ADD_GOODS,
                IConstant.MSG_SHOP_UPDATE_GOODS,
                IConstant.MSG_SHOP_DELETE_GOODS,
                IConstant.MSG_SHOP_QUERY_BALANCE,
                IConstant.MSG_SHOP_RECHARGE,
                IConstant.MSG_SHOP_CHECKOUT,
                IConstant.MSG_SHOP_QUERY_ORDERS
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Message handle(Message request) {
        switch (request.getName() == null ? "" : request.getName()) {
            case IConstant.MSG_SHOP_QUERY_GOODS:
                return handleQueryGoods(request);
            case IConstant.MSG_SHOP_PURCHASE:
                return handlePurchase(request);
            case IConstant.MSG_SHOP_QUERY_RECORDS:
                return handleQueryRecords(request);
            case IConstant.MSG_SHOP_ADD_GOODS:
                return handleAddGoods(request);
            case IConstant.MSG_SHOP_UPDATE_GOODS:
                return handleUpdateGoods(request);
            case IConstant.MSG_SHOP_DELETE_GOODS:
                return handleDeleteGoods(request);
            case IConstant.MSG_SHOP_QUERY_BALANCE:
                return handleQueryBalance(request);
            case IConstant.MSG_SHOP_RECHARGE:
                return handleRecharge(request);
            case IConstant.MSG_SHOP_CHECKOUT:
                return handleCheckout(request);
            case IConstant.MSG_SHOP_QUERY_ORDERS:
                return handleQueryOrders(request);
            default:
                return new Message(request.getUid(), request.getName(), MessageType.DATA,
                        IConstant.STATUS_ERROR, "未知的商店操作：" + request.getName(), "Server");
        }
    }

    /**
     * 处理"查询商品"。
     *
     * @param request 查询请求，{@code data} 为 {@link ShopRequest}
     * @return 查询结果消息：{@code data} 为 {@code List<Goods>}
     */
    private Message handleQueryGoods(Message request) {
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
     * 处理"购买商品"。
     *
     * @param request 购买请求，{@code data} 为 {@link ShopRequest}
     * @return 购买结果消息：{@code data} 为 {@link PurchaseRecord}
     */
    private Message handlePurchase(Message request) {
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
     * 处理"查询购买记录"。
     *
     * @param request 查询记录请求，{@code data} 为 {@link ShopRequest}
     * @return 查询结果消息：{@code data} 为 {@code List<PurchaseRecord>}
     */
    private Message handleQueryRecords(Message request) {
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
     * 处理"新增商品"（管理员）。
     *
     * @param request 新增商品请求，{@code data} 为 {@link ShopRequest}
     * @return 新增结果消息：{@code data} 为 {@link Goods}
     */
    private Message handleAddGoods(Message request) {
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
     * 处理"修改商品"（管理员）。
     *
     * @param request 修改商品请求，{@code data} 为 {@link ShopRequest}
     * @return 修改结果消息：{@code data} 为 {@link Goods}
     */
    private Message handleUpdateGoods(Message request) {
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
     * 处理"删除商品"（管理员）。
     *
     * @param request 删除商品请求，{@code data} 为 {@link ShopRequest}
     * @return 删除结果消息：{@code data} 为提示文本
     */
    private Message handleDeleteGoods(Message request) {
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
     * 处理"查询校园卡余额"。
     *
     * @param request 查询请求，{@code data} 为 {@link ShopRequest}
     * @return 查询结果消息：{@code data} 为余额（{@code BigDecimal}）
     */
    private Message handleQueryBalance(Message request) {
        try {
            ShopRequest req = (ShopRequest) request.getData();
            java.math.BigDecimal balance = _storeServerSrv.queryBalance(req.getUserId());
            return new Message(request.getUid(), IConstant.MSG_SHOP_QUERY_BALANCE, MessageType.DATA,
                    IConstant.STATUS_SUCCESS, balance, "Server");
        } catch (SQLException | IOException e) {
            return new Message(request.getUid(), IConstant.MSG_SHOP_QUERY_BALANCE, MessageType.DATA,
                    IConstant.STATUS_ERROR, "服务器内部异常：" + e.getMessage(), "Server");
        }
    }

    /**
     * 处理"校园卡充值"。
     *
     * @param request 充值请求，{@code data} 为 {@link ShopRequest}（含 amount）
     * @return 充值结果消息：{@code data} 为充值后的最新余额
     */
    private Message handleRecharge(Message request) {
        try {
            ShopRequest req = (ShopRequest) request.getData();
            java.math.BigDecimal balance = _storeServerSrv.recharge(req.getUserId(), req.getAmount());
            return new Message(request.getUid(), IConstant.MSG_SHOP_RECHARGE, MessageType.DATA,
                    IConstant.STATUS_SUCCESS, balance, "Server");
        } catch (ShopException e) {
            return new Message(request.getUid(), IConstant.MSG_SHOP_RECHARGE, MessageType.DATA,
                    e.getStatusCode(), e.getMessage(), "Server");
        } catch (SQLException | IOException e) {
            return new Message(request.getUid(), IConstant.MSG_SHOP_RECHARGE, MessageType.DATA,
                    IConstant.STATUS_ERROR, "服务器内部异常：" + e.getMessage(), "Server");
        }
    }

    /**
     * 处理"购物车结算"。
     *
     * @param request 结算请求，{@code data} 为 {@link ShopRequest}（含 items）
     * @return 结算结果消息：{@code data} 为 {@link Order}（含订单号、总金额与明细）
     */
    private Message handleCheckout(Message request) {
        try {
            ShopRequest req = (ShopRequest) request.getData();
            Order order = _storeServerSrv.checkout(req.getUserId(), req.getItems());
            return new Message(request.getUid(), IConstant.MSG_SHOP_CHECKOUT, MessageType.DATA,
                    IConstant.STATUS_SUCCESS, order, "Server");
        } catch (ShopException e) {
            return new Message(request.getUid(), IConstant.MSG_SHOP_CHECKOUT, MessageType.DATA,
                    e.getStatusCode(), e.getMessage(), "Server");
        } catch (SQLException | IOException e) {
            return new Message(request.getUid(), IConstant.MSG_SHOP_CHECKOUT, MessageType.DATA,
                    IConstant.STATUS_ERROR, "服务器内部异常：" + e.getMessage(), "Server");
        }
    }

    /**
     * 处理"查询订单（含明细）"。
     *
     * @param request 查询请求，{@code data} 为 {@link ShopRequest}
     * @return 查询结果消息：{@code data} 为 {@code List<Order>}
     */
    private Message handleQueryOrders(Message request) {
        try {
            ShopRequest req = (ShopRequest) request.getData();
            List<Order> orders = _storeServerSrv.queryOrders(req.getUserId());
            return new Message(request.getUid(), IConstant.MSG_SHOP_QUERY_ORDERS, MessageType.DATA,
                    IConstant.STATUS_SUCCESS, orders, "Server");
        } catch (SQLException | IOException e) {
            return new Message(request.getUid(), IConstant.MSG_SHOP_QUERY_ORDERS, MessageType.DATA,
                    IConstant.STATUS_ERROR, "服务器内部异常：" + e.getMessage(), "Server");
        }
    }
}
