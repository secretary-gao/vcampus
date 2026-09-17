/*
 * Order
 *
 * Version 1.0
 *
 * 2026-09-14
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.common.vo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 订单实体类，对应数据库 tblOrder（订单主表），一个订单可包含多个商品。
 * 因为需要通过 Socket 传输，所以实现 {@link Serializable} 接口。
 *
 * <p>设计说明：设计说明书把"同一订单可包含多个商品"作为开放问题做了简化处理
 * （一次购买仅针对单一商品）。本模块在购物车功能中按"一单多商品"落地，因此把订单拆成
 * 主表 + 明细两部分：主表 {@code tblOrder} 一行一个订单（订单号、下单人、订单总金额、
 * 下单时间），明细表 {@code tblPurchase} 一个订单多行（每行一个商品）。</p>
 *
 * <p>{@code _items} 是订单明细（{@link PurchaseRecord}），查询订单时由服务器按 orderId
 * 组装后一起返回，客户端无需再发一次请求即可展示订单详情。</p>
 */
public class Order implements Serializable {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 订单号（主键，服务器生成）。 */
    private String _orderId;

    /** 下单人ID（外键 -> tblUser.uId）。 */
    private String _userId;

    /** 订单总金额（= 各明细小计之和，>=0）。 */
    private BigDecimal _totalAmount;

    /** 下单时间。 */
    private LocalDateTime _orderTime;

    /** 订单明细（一个订单可含多个商品）。 */
    private List<PurchaseRecord> _items = new ArrayList<>();

    /**
     * 无参构造方法。
     */
    public Order() {
    }

    /**
     * 获取订单号。
     *
     * @return 订单号
     */
    public String getOrderId() {
        return _orderId;
    }

    /**
     * 设置订单号。
     *
     * @param orderId 订单号
     */
    public void setOrderId(String orderId) {
        this._orderId = orderId;
    }

    /**
     * 获取下单人ID。
     *
     * @return 下单人ID
     */
    public String getUserId() {
        return _userId;
    }

    /**
     * 设置下单人ID。
     *
     * @param userId 下单人ID
     */
    public void setUserId(String userId) {
        this._userId = userId;
    }

    /**
     * 获取订单总金额。
     *
     * @return 订单总金额
     */
    public BigDecimal getTotalAmount() {
        return _totalAmount;
    }

    /**
     * 设置订单总金额。
     *
     * @param totalAmount 订单总金额
     */
    public void setTotalAmount(BigDecimal totalAmount) {
        this._totalAmount = totalAmount;
    }

    /**
     * 获取下单时间。
     *
     * @return 下单时间
     */
    public LocalDateTime getOrderTime() {
        return _orderTime;
    }

    /**
     * 设置下单时间。
     *
     * @param orderTime 下单时间
     */
    public void setOrderTime(LocalDateTime orderTime) {
        this._orderTime = orderTime;
    }

    /**
     * 获取订单明细。
     *
     * @return 明细列表（可能为空列表，不会为 {@code null}）
     */
    public List<PurchaseRecord> getItems() {
        return _items;
    }

    /**
     * 设置订单明细。
     *
     * @param items 明细列表，传 {@code null} 时置为空列表
     */
    public void setItems(List<PurchaseRecord> items) {
        this._items = items == null ? new ArrayList<>() : items;
    }

    /**
     * 获取订单包含的商品种类数。
     *
     * @return 明细行数
     */
    public int getItemCount() {
        return _items == null ? 0 : _items.size();
    }

    /**
     * 获取订单包含的商品总件数（各行数量之和）。
     *
     * @return 总件数
     */
    public int getTotalQuantity() {
        if (_items == null) {
            return 0;
        }
        int sum = 0;
        for (PurchaseRecord item : _items) {
            sum += item.getQuantity();
        }
        return sum;
    }

    /**
     * 返回该订单的可读字符串表示（含明细行数与总金额）。
     *
     * @return 订单内容描述
     */
    @Override
    public String toString() {
        return "Order{" +
                "orderId='" + _orderId + '\'' +
                ", userId='" + _userId + '\'' +
                ", totalAmount=" + _totalAmount +
                ", orderTime=" + _orderTime +
                ", itemCount=" + getItemCount() +
                ", totalQuantity=" + getTotalQuantity() +
                '}';
    }
}
