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
 * category（类别）、price（单价）、stock（库存）。{@code _imageUrl} 为后续迭代新增的
 * 商品图片地址；{@code _discountRate}/{@code _promotionRemark} 是"每日特价"功能的
 * 附加字段——由服务器查促销表（tblPromotion）后填写，**只用于展示**，
 * 成交价一律由服务器在结算时重新计算（见 {@link #getDiscountPrice()}）。</p>
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

    /** 单价（原价，>=0）。 */
    private BigDecimal _price;

    /** 库存数量（>=0）。 */
    private int _stock;

    /** 商品图片地址（可空；为空时界面用类别图标占位）。 */
    private String _imageUrl;

    /** 今日折扣率（可空；{@code null} 或 1.00 表示今日无折扣，0.80 表示 8 折）。 */
    private BigDecimal _discountRate;

    /** 今日活动说明（如"周三文具日"，由促销表附带，便于界面展示）。 */
    private String _promotionRemark;

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
     * 全参构造方法（含图片地址）。
     *
     * @param goodsId   商品编号
     * @param goodsName 商品名称
     * @param category  商品类别
     * @param price     单价
     * @param stock     库存数量
     * @param imageUrl  商品图片地址
     */
    public Goods(String goodsId, String goodsName, String category, BigDecimal price, int stock, String imageUrl) {
        this._goodsId = goodsId;
        this._goodsName = goodsName;
        this._category = category;
        this._price = price;
        this._stock = stock;
        this._imageUrl = imageUrl;
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
     * 获取商品图片地址。
     *
     * @return 图片地址
     */
    public String getImageUrl() {
        return _imageUrl;
    }

    /**
     * 设置商品图片地址。
     *
     * @param imageUrl 图片地址
     */
    public void setImageUrl(String imageUrl) {
        this._imageUrl = imageUrl;
    }

    /**
     * 获取今日折扣率。
     *
     * @return 折扣率（{@code null} 表示今日无折扣）
     */
    public BigDecimal getDiscountRate() {
        return _discountRate;
    }

    /**
     * 设置今日折扣率。
     *
     * @param discountRate 折扣率（0.10~0.95，{@code null} 表示无折扣）
     */
    public void setDiscountRate(BigDecimal discountRate) {
        this._discountRate = discountRate;
    }

    /**
     * 获取今日活动说明。
     *
     * @return 活动说明（可空）
     */
    public String getPromotionRemark() {
        return _promotionRemark;
    }

    /**
     * 设置今日活动说明。
     *
     * @param promotionRemark 活动说明
     */
    public void setPromotionRemark(String promotionRemark) {
        this._promotionRemark = promotionRemark;
    }

    /**
     * 判断今日是否有折扣。
     *
     * @return {@code true} 表示今日有折扣
     */
    public boolean hasDiscount() {
        return _discountRate != null
                && _discountRate.compareTo(BigDecimal.ONE) < 0
                && _discountRate.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * 计算今日特价（原价 × 折扣率，四舍五入到分）。
     *
     * <p>注意：这里只是给界面展示用的计算结果，服务器在结算时会按数据库中的实时
     * 原价与当日活动重新算一遍，客户端传上来的价格不参与成交。</p>
     *
     * @return 特价；今日无折扣时返回原价
     */
    public BigDecimal getDiscountPrice() {
        if (!hasDiscount() || _price == null) {
            return _price;
        }
        return _price.multiply(_discountRate).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * 获取成交单价（有折扣时返回特价，否则返回原价）。
     *
     * @return 成交单价
     */
    public BigDecimal getEffectivePrice() {
        return hasDiscount() ? getDiscountPrice() : _price;
    }

    /**
     * 获取折扣的中文标签，例如 0.80 -> "8折"、0.85 -> "8.5折"。
     *
     * @return 折扣标签；今日无折扣时返回空串
     */
    public String getDiscountLabel() {
        if (!hasDiscount()) {
            return "";
        }
        BigDecimal tenths = _discountRate.multiply(BigDecimal.TEN).stripTrailingZeros();
        return tenths.toPlainString() + "折";
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
                ", imageUrl='" + _imageUrl + '\'' +
                ", discountRate=" + _discountRate +
                '}';
    }
}
