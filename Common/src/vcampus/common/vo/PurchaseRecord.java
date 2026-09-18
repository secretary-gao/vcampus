/*
 * PurchaseRecord
 *
 * Version 1.0
 *
 * 2026-09-01
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.common.vo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 购买记录（订单明细行）实体类，对应数据库 tblPurchase 表，用于在客户端与服务器端
 * 之间传输购买记录（购买、查询购买记录等场景）。因为需要通过 Socket 传输，
 * 所以实现 {@link Serializable} 接口。
 *
 * <p>字段设计对应共享说明书中 tblPurchase 表：orderId（订单号）、userId（购买人）、
 * goodsId（商品编号）、quantity（数量）、totalPrice（金额）、orderTime（下单时间）。
 * {@code _goodsName} 为查询时通过 LEFT JOIN tblGoods 附带得到，便于界面展示，不单独入库。</p>
 *
 * <p>购物车功能上线后，一个订单可以包含多个商品，本类在实际存储中表示订单的<b>一行明细</b>：
 * {@code _totalPrice} 为该行小计（成交单价×数量），订单总金额存在订单主表
 * {@link Order#getTotalAmount()} 中；一个订单的多行明细通过同一个 {@code orderId} 关联。</p>
 *
 * <p>{@code _originalPrice}/{@code _discountRate} 是<b>下单当时的价格快照</b>：商品单价可能被管理员
 * 修改、每日特价也每天不同，落快照才能让历史订单准确反映"当时成交了多少、优惠了多少"
 * （{@link #getSaving()}），不受事后改价或改活动的影响。</p>
 */
public class PurchaseRecord implements Serializable {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 订单号（服务器生成，一个订单的多行明细共用同一个订单号）。 */
    private String _orderId;

    /** 购买人ID（外键 -> tblUser.uId）。 */
    private String _userId;

    /** 商品编号（外键 -> tblGoods.goodsId）。 */
    private String _goodsId;

    /** 商品名称（查询时由 tblGoods 附带，便于展示）。 */
    private String _goodsName;

    /** 购买数量（>0）。 */
    private int _quantity;

    /** 本行小计（= 成交单价 * 数量，>=0）。 */
    private BigDecimal _totalPrice;

    /** 下单时原价快照（单价；历史数据按成交价回填，无快照时等于成交单价）。 */
    private BigDecimal _originalPrice;

    /** 下单时折扣率快照（0.10~0.95；下单时无活动为 {@code null}）。 */
    private BigDecimal _discountRate;

    /** 下单时间。 */
    private LocalDateTime _orderTime;

    /**
     * 无参构造方法。
     */
    public PurchaseRecord() {
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
     * 获取购买人ID。
     *
     * @return 购买人ID
     */
    public String getUserId() {
        return _userId;
    }

    /**
     * 设置购买人ID。
     *
     * @param userId 购买人ID
     */
    public void setUserId(String userId) {
        this._userId = userId;
    }

    /**
     * 获取商品编号。
     *
     * @return 商品编号
     */
    public String getGoodsId() {
        return _goodsId;
    }

    /**
     * 设置商品编号。
     *
     * @param goodsId 商品编号
     */
    public void setGoodsId(String goodsId) {
        this._goodsId = goodsId;
    }

    /**
     * 获取商品名称。
     *
     * @return 商品名称
     */
    public String getGoodsName() {
        return _goodsName;
    }

    /**
     * 设置商品名称。
     *
     * @param goodsName 商品名称
     */
    public void setGoodsName(String goodsName) {
        this._goodsName = goodsName;
    }

    /**
     * 获取购买数量。
     *
     * @return 购买数量
     */
    public int getQuantity() {
        return _quantity;
    }

    /**
     * 设置购买数量。
     *
     * @param quantity 购买数量
     */
    public void setQuantity(int quantity) {
        this._quantity = quantity;
    }

    /**
     * 获取本行小计。
     *
     * @return 本行小计
     */
    public BigDecimal getTotalPrice() {
        return _totalPrice;
    }

    /**
     * 设置本行小计。
     *
     * @param totalPrice 本行小计
     */
    public void setTotalPrice(BigDecimal totalPrice) {
        this._totalPrice = totalPrice;
    }

    /**
     * 获取下单时的原价快照。
     *
     * @return 原价（单价）；无快照时返回 {@code null}
     */
    public BigDecimal getOriginalPrice() {
        return _originalPrice;
    }

    /**
     * 设置下单时的原价快照。
     *
     * @param originalPrice 原价（单价）
     */
    public void setOriginalPrice(BigDecimal originalPrice) {
        this._originalPrice = originalPrice;
    }

    /**
     * 获取下单时的折扣率快照。
     *
     * @return 折扣率；下单时无活动返回 {@code null}
     */
    public BigDecimal getDiscountRate() {
        return _discountRate;
    }

    /**
     * 设置下单时的折扣率快照。
     *
     * @param discountRate 折扣率
     */
    public void setDiscountRate(BigDecimal discountRate) {
        this._discountRate = discountRate;
    }

    /**
     * 计算这一行的成交单价（小计 ÷ 数量，保留两位小数）。
     *
     * @return 成交单价
     */
    public BigDecimal getUnitPrice() {
        if (_totalPrice == null || _quantity <= 0) {
            return BigDecimal.ZERO;
        }
        return _totalPrice.divide(BigDecimal.valueOf(_quantity), 2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * 计算这一行优惠了多少钱（（原价 - 成交单价）× 数量）。
     *
     * @return 优惠金额；没有原价快照或没有优惠时返回 {@link BigDecimal#ZERO}
     */
    public BigDecimal getSaving() {
        if (_originalPrice == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal diff = _originalPrice.subtract(getUnitPrice());
        if (diff.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return diff.multiply(BigDecimal.valueOf(_quantity)).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * 判断这一行下单时是否享有了折扣。
     *
     * @return {@code true} 表示有优惠
     */
    public boolean hasDiscount() {
        return getSaving().compareTo(BigDecimal.ZERO) > 0;
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
     * 返回该购买记录的可读字符串表示。
     *
     * @return 购买记录内容的字符串描述
     */
    @Override
    public String toString() {
        return "PurchaseRecord{" +
                "orderId='" + _orderId + '\'' +
                ", userId='" + _userId + '\'' +
                ", goodsId='" + _goodsId + '\'' +
                ", goodsName='" + _goodsName + '\'' +
                ", quantity=" + _quantity +
                ", totalPrice=" + _totalPrice +
                ", originalPrice=" + _originalPrice +
                ", discountRate=" + _discountRate +
                ", orderTime=" + _orderTime +
                '}';
    }
}
