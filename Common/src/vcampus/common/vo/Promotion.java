/*
 * Promotion
 *
 * Version 1.0
 *
 * 2026-09-16
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.common.vo;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 商品促销（每日特价）实体类，对应数据库 tblPromotion 表，表示"某个商品在某个星期几打折"。
 * 因为需要通过 Socket 传输，所以实现 {@link Serializable} 接口。
 *
 * <p>需求是"每天有不同的特价商品搞活动"，所以活动按"商品 × 星期"配置：
 * {@code _weekday} 为 1（周一）~ 7（周日），0 表示每天特价。服务器按当前日期算出
 * 今天是星期几，取 {@code weekday = 今天 或 0} 的活动，把折扣率附加到
 * {@link Goods#getDiscountRate()} 上返回给客户端展示，并在结算时据此计算成交价。
 * 客户端不参与计价，只负责显示。</p>
 */
public class Promotion implements Serializable {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 促销编号（主键）。 */
    private String _promoId;

    /** 商品编号（外键 -> tblGoods.goodsId）。 */
    private String _goodsId;

    /** 商品名称（查询时由 tblGoods 附带，便于展示）。 */
    private String _goodsName;

    /** 折扣率（0.10~0.95，如 0.80 表示 8 折）。 */
    private BigDecimal _discountRate;

    /** 生效星期：1=周一 … 7=周日，0=每天。 */
    private int _weekday;

    /** 活动说明（如"周三文具日"）。 */
    private String _remark;

    /**
     * 无参构造方法。
     */
    public Promotion() {
    }

    /**
     * 完整构造方法。
     *
     * @param promoId      促销编号
     * @param goodsId      商品编号
     * @param discountRate 折扣率
     * @param weekday      生效星期（1~7，0 表示每天）
     * @param remark       活动说明
     */
    public Promotion(String promoId, String goodsId, BigDecimal discountRate, int weekday, String remark) {
        this._promoId = promoId;
        this._goodsId = goodsId;
        this._discountRate = discountRate;
        this._weekday = weekday;
        this._remark = remark;
    }

    /**
     * 获取促销编号。
     *
     * @return 促销编号
     */
    public String getPromoId() {
        return _promoId;
    }

    /**
     * 设置促销编号。
     *
     * @param promoId 促销编号
     */
    public void setPromoId(String promoId) {
        this._promoId = promoId;
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
     * 获取折扣率。
     *
     * @return 折扣率
     */
    public BigDecimal getDiscountRate() {
        return _discountRate;
    }

    /**
     * 设置折扣率。
     *
     * @param discountRate 折扣率
     */
    public void setDiscountRate(BigDecimal discountRate) {
        this._discountRate = discountRate;
    }

    /**
     * 获取生效星期。
     *
     * @return 1=周一 … 7=周日，0=每天
     */
    public int getWeekday() {
        return _weekday;
    }

    /**
     * 设置生效星期。
     *
     * @param weekday 1=周一 … 7=周日，0=每天
     */
    public void setWeekday(int weekday) {
        this._weekday = weekday;
    }

    /**
     * 获取活动说明。
     *
     * @return 活动说明
     */
    public String getRemark() {
        return _remark;
    }

    /**
     * 设置活动说明。
     *
     * @param remark 活动说明
     */
    public void setRemark(String remark) {
        this._remark = remark;
    }

    /**
     * 获取折扣的中文标签，例如 0.80 -> "8折"。
     *
     * @return 折扣标签
     */
    public String getDiscountLabel() {
        if (_discountRate == null) {
            return "";
        }
        return _discountRate.multiply(BigDecimal.TEN).stripTrailingZeros().toPlainString() + "折";
    }

    /**
     * 获取生效星期的中文标签。
     *
     * @return 例如"周三"、"每天"
     */
    public String getWeekdayLabel() {
        if (_weekday == 0) {
            return "每天";
        }
        String[] names = {"", "周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        return _weekday >= 1 && _weekday <= 7 ? names[_weekday] : "未知";
    }

    /**
     * 返回该促销活动的可读字符串表示。
     *
     * @return 活动内容描述
     */
    @Override
    public String toString() {
        return "Promotion{" +
                "promoId='" + _promoId + '\'' +
                ", goodsId='" + _goodsId + '\'' +
                ", goodsName='" + _goodsName + '\'' +
                ", discountRate=" + _discountRate +
                ", weekday=" + _weekday +
                ", remark='" + _remark + '\'' +
                '}';
    }
}
