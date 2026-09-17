/*
 * CartItem
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

/**
 * 购物车条目实体类，表示"某个商品 + 购买数量"，同时作为购物车结算请求的传输对象。
 * 因为需要通过 Socket 传输，所以实现 {@link Serializable} 接口。
 *
 * <p>购物车本身保存在客户端内存中（不建数据库表），因此本类不入库；结算时客户端把
 * 若干 {@code CartItem} 打包进 {@link ShopRequest#getItems()} 发给服务器，服务器据此
 * 生成一个含多条明细的订单（tblOrder + tblPurchase）。</p>
 *
 * <p>{@code _goodsName}/{@code _unitPrice} 是加入购物车时的商品快照，仅用于界面展示与
 * 结算前估算金额；服务器结算时以数据库中的实时单价为准，防止客户端改价。</p>
 */
public class CartItem implements Serializable {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 商品编号（外键 -> tblGoods.goodsId）。 */
    private String _goodsId;

    /** 商品名称（加入购物车时的快照，便于展示）。 */
    private String _goodsName;

    /** 加入购物车时的单价快照（仅用于展示，结算以服务器实时价格为准）。 */
    private BigDecimal _unitPrice;

    /** 购买数量（>0）。 */
    private int _quantity;

    /**
     * 无参构造方法。
     */
    public CartItem() {
    }

    /**
     * 完整构造方法。
     *
     * @param goodsId   商品编号
     * @param goodsName 商品名称
     * @param unitPrice 单价（可为 null，表示只关心编号与数量）
     * @param quantity  购买数量
     */
    public CartItem(String goodsId, String goodsName, BigDecimal unitPrice, int quantity) {
        this._goodsId = goodsId;
        this._goodsName = goodsName;
        this._unitPrice = unitPrice;
        this._quantity = quantity;
    }

    /**
     * 只带商品编号与数量的构造方法（用于构造结算请求）。
     *
     * @param goodsId  商品编号
     * @param quantity 购买数量
     */
    public CartItem(String goodsId, int quantity) {
        this(goodsId, null, null, quantity);
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
     * 获取单价快照。
     *
     * @return 单价
     */
    public BigDecimal getUnitPrice() {
        return _unitPrice;
    }

    /**
     * 设置单价快照。
     *
     * @param unitPrice 单价
     */
    public void setUnitPrice(BigDecimal unitPrice) {
        this._unitPrice = unitPrice;
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
     * 计算本条目小计（单价 × 数量）。
     *
     * @return 小计金额；单价未知时返回 {@link BigDecimal#ZERO}
     */
    public BigDecimal getSubtotal() {
        if (_unitPrice == null) {
            return BigDecimal.ZERO;
        }
        return _unitPrice.multiply(BigDecimal.valueOf(_quantity));
    }

    /**
     * 返回该购物车条目的可读字符串表示。
     *
     * @return 条目内容描述
     */
    @Override
    public String toString() {
        return "CartItem{" +
                "goodsId='" + _goodsId + '\'' +
                ", goodsName='" + _goodsName + '\'' +
                ", unitPrice=" + _unitPrice +
                ", quantity=" + _quantity +
                '}';
    }
}
