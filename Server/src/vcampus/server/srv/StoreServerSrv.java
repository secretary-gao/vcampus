/*
 * StoreServerSrv
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
import vcampus.common.vo.PurchaseRecord;
import vcampus.server.dao.GoodsDAO;
import vcampus.server.dao.PurchaseDAO;
import vcampus.server.dao.DbHelper;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * {@link IStoreServerSrv} 的实现类，承载虚拟商店模块的业务逻辑，
 * 具体的数据库读写委托给 {@link GoodsDAO} 与 {@link PurchaseDAO}。
 *
 * <p>购买操作涉及"插入订单 + 扣减库存"两个数据库写操作，为保证数据一致性，
 * 使用 JDBC 事务（关闭自动提交，成功后提交，异常时回滚），并在校验库存前
 * 通过 {@code SELECT ... FOR UPDATE} 锁定商品行以防并发超卖。</p>
 */
public class StoreServerSrv implements IStoreServerSrv {

    /** 商品数据访问对象。 */
    private final GoodsDAO _goodsDAO = new GoodsDAO();

    /** 购买记录数据访问对象。 */
    private final PurchaseDAO _purchaseDAO = new PurchaseDAO();

    /** 订单号时间戳格式。 */
    private static final DateTimeFormatter ORDER_ID_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Goods> queryGoods(String keyword, String category) throws SQLException, IOException {
        return _goodsDAO.queryByCondition(keyword, category);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PurchaseRecord purchaseGoods(String userId, String goodsId, int quantity)
            throws ShopException, SQLException, IOException {
        if (quantity <= 0) {
            throw new ShopException(IConstant.STATUS_CONFLICT, "购买数量必须为正整数");
        }
        Connection conn = null;
        try {
            conn = DbHelper.getConnection();
            conn.setAutoCommit(false);

            // 1) 锁定商品行，防止并发超卖
            Goods goods = _goodsDAO.findByGoodsIdForUpdate(conn, goodsId);
            if (goods == null) {
                throw new ShopException(IConstant.STATUS_GOODS_NOT_FOUND, "商品不存在：" + goodsId);
            }
            if (goods.getStock() < quantity) {
                throw new ShopException(IConstant.STATUS_STOCK_NOT_ENOUGH,
                        "库存不足：当前库存 " + goods.getStock() + "，购买数量 " + quantity);
            }

            // 2) 生成订单并计算总价
            BigDecimal totalPrice = goods.getPrice().multiply(BigDecimal.valueOf(quantity));
            PurchaseRecord record = new PurchaseRecord();
            record.setOrderId(generateOrderId());
            record.setUserId(userId);
            record.setGoodsId(goods.getGoodsId());
            record.setGoodsName(goods.getGoodsName());
            record.setQuantity(quantity);
            record.setTotalPrice(totalPrice);
            record.setOrderTime(LocalDateTime.now());

            // 3) 插入订单 + 扣减库存（同一事务）
            _purchaseDAO.insert(conn, record);
            int updated = _goodsDAO.updateStock(conn, goodsId, -quantity);
            if (updated != 1) {
                throw new ShopException(IConstant.STATUS_CONFLICT, "库存扣减失败，请重试");
            }

            conn.commit();
            return record;
        } catch (ShopException e) {
            rollbackQuietly(conn);
            throw e;
        } catch (SQLException e) {
            rollbackQuietly(conn);
            throw e;
        } catch (IOException e) {
            rollbackQuietly(conn);
            throw e;
        } finally {
            DbHelper.close(conn, null, null);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PurchaseRecord> queryPurchaseRecords(String userId) throws SQLException, IOException {
        if (userId == null || userId.isBlank()) {
            return _purchaseDAO.findAll(); // 管理员查看全部记录
        }
        return _purchaseDAO.findByUserId(userId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Goods addGoods(Goods goods) throws ShopException, SQLException, IOException {
        if (_goodsDAO.findByGoodsId(goods.getGoodsId()) != null) {
            throw new ShopException(IConstant.STATUS_CONFLICT, "商品编号已存在：" + goods.getGoodsId());
        }
        _goodsDAO.insert(goods);
        return _goodsDAO.findByGoodsId(goods.getGoodsId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Goods updateGoods(Goods goods) throws ShopException, SQLException, IOException {
        if (_goodsDAO.findByGoodsId(goods.getGoodsId()) == null) {
            throw new ShopException(IConstant.STATUS_GOODS_NOT_FOUND, "商品不存在：" + goods.getGoodsId());
        }
        _goodsDAO.update(goods);
        return _goodsDAO.findByGoodsId(goods.getGoodsId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean deleteGoods(String goodsId) throws ShopException, SQLException, IOException {
        if (_goodsDAO.findByGoodsId(goodsId) == null) {
            throw new ShopException(IConstant.STATUS_GOODS_NOT_FOUND, "商品不存在：" + goodsId);
        }
        if (_purchaseDAO.existsByGoodsId(goodsId)) {
            throw new ShopException(IConstant.STATUS_CONFLICT, "该商品已有购买记录，无法删除");
        }
        return _goodsDAO.delete(goodsId);
    }

    /**
     * 生成全局唯一的订单号：ORDER + 时间戳 + 随机数（服务器端生成，客户端不参与）。
     *
     * @return 订单号
     */
    private String generateOrderId() {
        String ts = ORDER_ID_TIME.format(LocalDateTime.now());
        int rand = ThreadLocalRandom.current().nextInt(1000, 10000);
        return "ORDER" + ts + rand;
    }

    /**
     * 对当前事务连接执行回滚，异常静默忽略。
     *
     * @param conn 事务连接，可为 {@code null}
     */
    private void rollbackQuietly(Connection conn) {
        if (conn != null) {
            try {
                conn.rollback();
            } catch (SQLException ignored) {
                // 忽略回滚异常
            }
        }
    }
}
