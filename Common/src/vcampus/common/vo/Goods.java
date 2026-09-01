/*
 * Goods
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

/**
 * 商品实体类，对应数据库 tblGoods 表，用于在客户端与服务器端之间传输商品信息
 * （商品查询、购买、商品管理等场景）。因为需要通过 Socket 传输，所以实现
 * {@link Serializable} 接口。
 *
 * <p>字段设计对应共享说明书中 tblGoods 表：goodsId（商品编号）、goodsName（名称）、
 * category（类别）、price（单价）、stock（库存）。</p>
 */
public class Goods implements Serializable {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 商品编号（主键）。 */
    private String _goodsId;

    /** 商品名称。 */
    private String _goodsName;

    /** 商品类别。 */
    private String _category;

    /** 单价（>=0）。 */
    private BigDecimal _price;

    /** 库存数量（>=0）。 */
    private int _stock;

    /**
     * 无参构造方法。
     */
    public Goods() {
    }

    /**
     * 全参构造方法。
     *
     * @param goodsId   商品编号
     * @param goodsName 商品名称
     * @param category  商品类别
     * @param price     单价
     * @param stock     库存数量
     */
    public Goods(String goodsId, String goodsName, String category, BigDecimal price, int stock) {
        this._goodsId = goodsId;
        this._goodsName = goodsName;
        this._category = category;
        this._price = price;
        this._stock = stock;
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
     * 获取商品类别。
     *
     * @return 商品类别
     */
    public String getCategory() {
        return _category;
    }

    /**
     * 设置商品类别。
     *
     * @param category 商品类别
     */
    public void setCategory(String category) {
        this._category = category;
    }

    /**
     * 获取单价。
     *
     * @return 单价
     */
    public BigDecimal getPrice() {
        return _price;
    }

    /**
     * 设置单价。
     *
     * @param price 单价
     */
    public void setPrice(BigDecimal price) {
        this._price = price;
    }

    /**
     * 获取库存数量。
     *
     * @return 库存数量
     */
    public int getStock() {
        return _stock;
    }

    /**
     * 设置库存数量。
     *
     * @param stock 库存数量
     */
    public void setStock(int stock) {
        this._stock = stock;
    }

    /**
     * 返回该商品的可读字符串表示。
     *
     * @return 商品信息的字符串描述
     */
    @Override
    public String toString() {
        return "Goods{" +
                "goodsId='" + _goodsId + '\'' +
                ", goodsName='" + _goodsName + '\'' +
                ", category='" + _category + '\'' +
                ", price=" + _price +
                ", stock=" + _stock +
                '}';
    }
}
