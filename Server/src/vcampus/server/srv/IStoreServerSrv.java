/*
 * IStoreServerSrv
 *
 * Version 1.0
 *
 * 2026-09-01
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.server.srv;

import vcampus.common.vo.CartItem;
import vcampus.common.vo.Goods;
import vcampus.common.vo.Order;
import vcampus.common.vo.Promotion;
import vcampus.common.vo.PurchaseRecord;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

/**
 * 服务器端商店业务服务接口，对应共享说明书中"虚拟商店模块"的 IStoreServerSrv。
 * 由 {@link ServerThread} 的商店处理器调用，屏蔽
 * {@link vcampus.server.dao.GoodsDAO} / {@link vcampus.server.dao.PurchaseDAO}
 * 的具体数据库细节，暴露商品查询、购买、记录查询、商品管理六个业务动作。
 */
public interface IStoreServerSrv {

    /**
     * 查询商品：按关键字（名称/类别模糊）与类别筛选。
     *
     * @param keyword  名称/类别关键字，可为 {@code null}（不筛选）
     * @param category 类别，可为 {@code null}（不筛选）
     * @return 符合条件的商品列表
     * @throws SQLException 数据库操作异常
     * @throws IOException  数据库配置文件读取异常
     */
    List<Goods> queryGoods(String keyword, String category) throws SQLException, IOException;

    /**
     * 购买商品：校验库存与数量，生成订单并原子性扣减库存（事务）。
     *
     * @param userId   购买人ID
     * @param goodsId  商品编号
     * @param quantity 购买数量
     * @return 生成的购买记录（订单）
     * @throws ShopException 业务规则不满足（商品不存在/库存不足等）
     * @throws SQLException  数据库操作异常
     * @throws IOException   数据库配置文件读取异常
     */
    PurchaseRecord purchaseGoods(String userId, String goodsId, int quantity)
            throws ShopException, SQLException, IOException;

    /**
     * 查询购买记录（订单明细行）：指定用户返回其本人记录；userId 为空则返回全部记录（管理员）。
     *
     * @param userId 购买人ID，可为 {@code null} 或空串（表示查询全部）
     * @return 购买记录列表
     * @throws SQLException 数据库操作异常
     * @throws IOException  数据库配置文件读取异常
     */
    List<PurchaseRecord> queryPurchaseRecords(String userId) throws SQLException, IOException;

    /**
     * 购物车结算：把购物车中的多个商品作为一个订单提交，原子性地完成
     * "写订单主表 + 写多条订单明细 + 扣减库存 + 扣减整单金额"。
     *
     * @param userId 下单人ID
     * @param items  购物车条目（同一个商品重复出现会合并数量）
     * @return 结算成功后的订单（含订单号、订单总金额与明细列表）
     * @throws ShopException 购物车为空/商品不存在/库存不足/余额不足等业务规则不满足
     * @throws SQLException  数据库操作异常
     * @throws IOException   数据库配置文件读取异常
     */
    Order checkout(String userId, List<CartItem> items) throws ShopException, SQLException, IOException;

    /**
     * 查询订单（含订单明细）：指定用户返回其本人订单；userId 为空则返回全部订单（管理员）。
     *
     * @param userId 下单人ID，可为 {@code null} 或空串（表示查询全部）
     * @return 订单列表（每个订单内含明细），按下单时间倒序
     * @throws SQLException 数据库操作异常
     * @throws IOException  数据库配置文件读取异常
     */
    List<Order> queryOrders(String userId) throws SQLException, IOException;

    /**
     * 新增商品（管理员）：商品编号已存在时抛出业务异常。
     *
     * @param goods 待新增的商品对象
     * @return 新增成功后的商品对象
     * @throws ShopException 商品编号已存在
     * @throws SQLException  数据库操作异常
     * @throws IOException   数据库配置文件读取异常
     */
    Goods addGoods(Goods goods) throws ShopException, SQLException, IOException;

    /**
     * 修改商品（管理员，编号不可修改）。
     *
     * @param goods 待修改的商品对象（以 goodsId 定位）
     * @return 修改成功后的商品对象
     * @throws ShopException 商品不存在
     * @throws SQLException  数据库操作异常
     * @throws IOException   数据库配置文件读取异常
     */
    Goods updateGoods(Goods goods) throws ShopException, SQLException, IOException;

    /**
     * 删除商品（管理员）：若该商品已存在购买记录则禁止删除。
     *
     * @param goodsId 商品编号
     * @return 删除成功返回 {@code true}
     * @throws ShopException 商品不存在/存在购买记录禁止删除
     * @throws SQLException  数据库操作异常
     * @throws IOException   数据库配置文件读取异常
     */
    boolean deleteGoods(String goodsId) throws ShopException, SQLException, IOException;

    /**
     * 查询校园卡余额：用户还没有钱包记录时返回 0。
     *
     * @param userId 用户ID
     * @return 当前余额
     * @throws SQLException 数据库操作异常
     * @throws IOException  数据库配置文件读取异常
     */
    BigDecimal queryBalance(String userId) throws SQLException, IOException;

    /**
     * 校园卡充值（演示用，直接增加余额）。
     *
     * @param userId 用户ID
     * @param amount 充值金额（必须为正数）
     * @return 充值后的最新余额
     * @throws ShopException 金额非法
     * @throws SQLException  数据库操作异常
     * @throws IOException   数据库配置文件读取异常
     */
    BigDecimal recharge(String userId, BigDecimal amount) throws ShopException, SQLException, IOException;

    /**
     * 查询全部促销活动（管理员配置"每日特价"用，含 7 天与每天特价）。
     *
     * @return 全部活动列表
     * @throws SQLException 数据库操作异常
     * @throws IOException  数据库配置文件读取异常
     */
    List<Promotion> queryPromotions() throws SQLException, IOException;

    /**
     * 新增促销活动（管理员）。
     *
     * @param promotion 活动对象（商品必须存在，折扣率 0~1 之间，星期 0~7）
     * @return 新增后的活动对象
     * @throws ShopException 字段不合法/商品不存在/促销编号重复/同商品同天已有活动
     * @throws SQLException  数据库操作异常
     * @throws IOException   数据库配置文件读取异常
     */
    Promotion addPromotion(Promotion promotion) throws ShopException, SQLException, IOException;

    /**
     * 修改促销活动（管理员，按促销编号定位）。
     *
     * @param promotion 活动对象
     * @return 修改后的活动对象
     * @throws ShopException 活动不存在/字段不合法/同商品同天已有活动
     * @throws SQLException  数据库操作异常
     * @throws IOException   数据库配置文件读取异常
     */
    Promotion updatePromotion(Promotion promotion) throws ShopException, SQLException, IOException;

    /**
     * 删除促销活动（管理员）。
     *
     * @param promoId 促销编号
     * @return 删除成功返回 {@code true}
     * @throws ShopException 活动不存在或未选择活动
     * @throws SQLException  数据库操作异常
     * @throws IOException   数据库配置文件读取异常
     */
    boolean deletePromotion(String promoId) throws ShopException, SQLException, IOException;
}
