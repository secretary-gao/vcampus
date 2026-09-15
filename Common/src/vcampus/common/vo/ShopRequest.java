/*
 * ShopRequest
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
import java.util.List;

/**
 * 虚拟商店模块的请求载体，用于把客户端发起的各类商店业务参数打包进
 * {@link Message#getData()} 中，随报文发送给服务器。因为需要通过 Socket 传输，
 * 所以实现 {@link Serializable} 接口。
 *
 * <p>不同操作只用到其中部分字段，例如：查询商品用 {@code _keyword}/{@code _category}；
 * 购买商品用 {@code _userId}/{@code _goodsId}/{@code _quantity}；查询购买记录用
 * {@code _userId}；新增/修改商品用 {@code _goods}；删除商品用 {@code _goodsId}；
 * 校园卡充值用 {@code _userId}/{@code _amount}；购物车结算用
 * {@code _userId}/{@code _items}（一次可含多个商品）。</p>
 */
public class ShopRequest implements Serializable {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 商品查询关键字（名称/类别模糊）。 */
    private String _keyword;

    /** 商品类别筛选。 */
    private String _category;

    /** 购买人ID / 查询购买记录的用户ID。 */
    private String _userId;

    /** 商品编号（购买/删除）。 */
    private String _goodsId;

    /** 购买数量。 */
    private int _quantity;

    /** 商品对象（增加/修改商品）。 */
    private Goods _goods;

    /** 充值金额（校园卡充值）。 */
    private BigDecimal _amount;

    /** 购物车结算的商品条目（一次结算可含多个商品）。 */
    private List<CartItem> _items;

    /**
     * 无参构造方法。
     */
    public ShopRequest() {
    }

    /**
     * 获取商品查询关键字。
     *
     * @return 查询关键字
     */
    public String getKeyword() {
        return _keyword;
    }

    /**
     * 设置商品查询关键字。
     *
     * @param keyword 查询关键字
     */
    public void setKeyword(String keyword) {
        this._keyword = keyword;
    }

    /**
     * 获取商品类别筛选。
     *
     * @return 类别
     */
    public String getCategory() {
        return _category;
    }

    /**
     * 设置商品类别筛选。
     *
     * @param category 类别
     */
    public void setCategory(String category) {
        this._category = category;
    }

    /**
     * 获取用户ID。
     *
     * @return 用户ID
     */
    public String getUserId() {
        return _userId;
    }

    /**
     * 设置用户ID。
     *
     * @param userId 用户ID
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
     * 获取商品对象。
     *
     * @return 商品对象
     */
    public Goods getGoods() {
        return _goods;
    }

    /**
     * 设置商品对象。
     *
     * @param goods 商品对象
     */
    public void setGoods(Goods goods) {
        this._goods = goods;
    }

    /**
     * 获取充值金额。
     *
     * @return 充值金额
     */
    public BigDecimal getAmount() {
        return _amount;
    }

    /**
     * 设置充值金额。
     *
     * @param amount 充值金额
     */
    public void setAmount(BigDecimal amount) {
        this._amount = amount;
    }

    /**
     * 获取购物车结算条目。
     *
     * @return 购物车条目列表
     */
    public List<CartItem> getItems() {
        return _items;
    }

    /**
     * 设置购物车结算条目。
     *
     * @param items 购物车条目列表
     */
    public void setItems(List<CartItem> items) {
        this._items = items;
    }
}
