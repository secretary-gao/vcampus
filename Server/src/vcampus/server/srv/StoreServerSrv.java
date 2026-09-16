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
import vcampus.common.vo.CartItem;
import vcampus.common.vo.Goods;
import vcampus.common.vo.Order;
import vcampus.common.vo.Promotion;
import vcampus.common.vo.PurchaseRecord;
import vcampus.common.vo.Wallet;
import vcampus.server.dao.GoodsDAO;
import vcampus.server.dao.OrderDAO;
import vcampus.server.dao.PromotionDAO;
import vcampus.server.dao.PurchaseDAO;
import vcampus.server.dao.WalletDAO;
import vcampus.server.dao.DbHelper;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * {@link IStoreServerSrv} 的实现类，承载虚拟商店模块的业务逻辑，
 * 具体的数据库读写委托给 {@link GoodsDAO}、{@link OrderDAO} 与 {@link PurchaseDAO}。
 *
 * <p>下单操作涉及"写订单主表 + 写订单明细 + 扣减库存 + 扣减余额"多个数据库写操作，
 * 为保证数据一致性，使用 JDBC 事务（关闭自动提交，成功后提交，异常时回滚），并在校验
 * 库存前通过 {@code SELECT ... FOR UPDATE} 锁定商品行以防并发超卖；购物车一次结算多个
 * 商品时按商品编号排序后逐行加锁，保证加锁顺序一致，避免并发死锁。</p>
 */
public class StoreServerSrv implements IStoreServerSrv {

    /** 商品数据访问对象。 */
    private final GoodsDAO _goodsDAO = new GoodsDAO();

    /** 订单主表数据访问对象。 */
    private final OrderDAO _orderDAO = new OrderDAO();

    /** 购买记录（订单明细）数据访问对象。 */
    private final PurchaseDAO _purchaseDAO = new PurchaseDAO();

    /** 钱包（校园卡余额）数据访问对象。 */
    private final WalletDAO _walletDAO = new WalletDAO();

    /** 促销（每日特价）数据访问对象。 */
    private final PromotionDAO _promotionDAO = new PromotionDAO();

    /** 订单号时间戳格式。 */
    private static final DateTimeFormatter ORDER_ID_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    /**
     * {@inheritDoc}
     *
     * <p>查询结果会附上"今日特价"：服务器按当前日期算出星期几，取 tblPromotion 中
     * 当天生效的活动（含 weekday=0 的每天特价），把折扣率与活动说明填进商品对象，
     * 供客户端展示划线原价与折扣角标。</p>
     */
    @Override
    public List<Goods> queryGoods(String keyword, String category) throws SQLException, IOException {
        List<Goods> goods = _goodsDAO.queryByCondition(keyword, category);
        attachTodayPromotion(goods);
        return goods;
    }

    /**
     * 给商品列表附上今日特价信息。
     *
     * <p>同一个商品若同时命中"当天活动"和"每天特价"，取折扣更大（更便宜）的那条。</p>
     *
     * @param goods 商品列表
     * @throws SQLException 数据库操作异常
     * @throws IOException  数据库配置文件读取异常
     */
    private void attachTodayPromotion(List<Goods> goods) throws SQLException, IOException {
        if (goods == null || goods.isEmpty()) {
            return;
        }
        Map<String, Promotion> today = bestPromotionMap(_promotionDAO.findByWeekday(todayWeekday()));
        for (Goods item : goods) {
            Promotion promotion = today.get(item.getGoodsId());
            if (promotion != null) {
                item.setDiscountRate(promotion.getDiscountRate());
                item.setPromotionRemark(promotion.getRemark());
            }
        }
    }

    /**
     * 把活动列表整理成"商品编号 -> 最优惠活动"的映射。
     *
     * @param promotions 活动列表
     * @return 商品编号到最优惠活动的映射
     */
    private Map<String, Promotion> bestPromotionMap(List<Promotion> promotions) {
        Map<String, Promotion> best = new LinkedHashMap<>();
        if (promotions == null) {
            return best;
        }
        for (Promotion promotion : promotions) {
            if (promotion.getGoodsId() == null || promotion.getDiscountRate() == null) {
                continue;
            }
            Promotion exist = best.get(promotion.getGoodsId());
            if (exist == null || promotion.getDiscountRate().compareTo(exist.getDiscountRate()) < 0) {
                best.put(promotion.getGoodsId(), promotion);
            }
        }
        return best;
    }

    /**
     * 今天是星期几（1=周一 … 7=周日，与 tblPromotion.weekday 的约定一致）。
     *
     * @return 星期编号
     */
    private int todayWeekday() {
        return LocalDate.now().getDayOfWeek().getValue();
    }

    /**
     * {@inheritDoc}
     *
     * <p>实现方式：把"一个商品 + 数量"包装成只有一条明细的购物车，复用
     * {@link #checkout(String, List)} 的事务逻辑，保证单商品下单与购物车结算在
     * 库存锁定、余额扣减、订单落库上完全一致。</p>
     */
    @Override
    public PurchaseRecord purchaseGoods(String userId, String goodsId, int quantity)
            throws ShopException, SQLException, IOException {
        List<CartItem> items = new ArrayList<>();
        items.add(new CartItem(goodsId, quantity));
        Order order = checkout(userId, items);
        return order.getItems().isEmpty() ? null : order.getItems().get(0);
    }

    /**
     * {@inheritDoc}
     *
     * <p>整个过程处于一个数据库事务中：先按商品编号排序逐行加锁校验库存（同一个商品在
     * 购物车里出现多次会先合并数量），再一次性扣减整单金额，最后写订单主表、逐行写订单
     * 明细并扣减库存；任何一步失败都整体回滚，不会出现"扣了钱没订单"或"订单缺库存"。</p>
     */
    @Override
    public Order checkout(String userId, List<CartItem> items)
            throws ShopException, SQLException, IOException {
        if (userId == null || userId.isBlank()) {
            throw new ShopException(IConstant.STATUS_CONFLICT, "未登录或用户信息缺失");
        }

        // 1) 合并购物车条目：同一商品多次加入合并数量；按商品编号排序，保证后续加锁顺序一致
        Map<String, Integer> merged = new TreeMap<>();
        for (CartItem item : items == null ? Collections.<CartItem>emptyList() : items) {
            if (item == null || item.getGoodsId() == null || item.getGoodsId().isBlank()) {
                throw new ShopException(IConstant.STATUS_CONFLICT, "购物车中存在无效商品");
            }
            if (item.getQuantity() <= 0) {
                throw new ShopException(IConstant.STATUS_CONFLICT, "购买数量必须为正整数");
            }
            merged.merge(item.getGoodsId(), item.getQuantity(), Integer::sum);
        }
        if (merged.isEmpty()) {
            throw new ShopException(IConstant.STATUS_CONFLICT, "购物车为空，无法结算");
        }

        Connection conn = null;
        try {
            conn = DbHelper.getConnection();
            conn.setAutoCommit(false);

            // 2) 取出今日特价（与扣库存、扣余额同一事务读取），成交价按"原价 × 当日折扣"计算
            Map<String, Promotion> today = bestPromotionMap(_promotionDAO.findByWeekday(conn, todayWeekday()));

            // 3) 逐个商品加锁校验库存，金额一律由服务器按数据库实时单价与当日活动计算
            BigDecimal totalAmount = BigDecimal.ZERO;
            List<PurchaseRecord> details = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : merged.entrySet()) {
                String goodsId = entry.getKey();
                int quantity = entry.getValue();
                Goods goods = _goodsDAO.findByGoodsIdForUpdate(conn, goodsId);
                if (goods == null) {
                    throw new ShopException(IConstant.STATUS_GOODS_NOT_FOUND, "商品不存在：" + goodsId);
                }
                if (goods.getStock() < quantity) {
                    throw new ShopException(IConstant.STATUS_STOCK_NOT_ENOUGH,
                            "库存不足：" + goods.getGoodsName() + " 当前库存 " + goods.getStock()
                                    + "，购买数量 " + quantity);
                }
                // 今日有活动就按折扣价成交（客户端传上来的价格完全不参与计算）
                Promotion promotion = today.get(goodsId);
                BigDecimal unitPrice = goods.getPrice();
                if (promotion != null && promotion.getDiscountRate() != null) {
                    unitPrice = unitPrice.multiply(promotion.getDiscountRate())
                            .setScale(2, RoundingMode.HALF_UP);
                }
                BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
                totalAmount = totalAmount.add(subtotal);

                PurchaseRecord detail = new PurchaseRecord();
                detail.setUserId(userId);
                detail.setGoodsId(goods.getGoodsId());
                detail.setGoodsName(goods.getGoodsName());
                detail.setQuantity(quantity);
                detail.setTotalPrice(subtotal);
                details.add(detail);
            }

            // 3) 校验并扣减校园卡余额（整单一次扣减，条件扣减从数据库层面防止扣成负数）
            Wallet wallet = _walletDAO.findByUserId(userId);
            BigDecimal balance = wallet == null ? BigDecimal.ZERO : wallet.getBalance();
            if (balance.compareTo(totalAmount) < 0) {
                throw new ShopException(IConstant.STATUS_BALANCE_NOT_ENOUGH,
                        "余额不足：当前余额 " + balance.toPlainString() + " 元，本单需要 "
                                + totalAmount.toPlainString() + " 元，请先充值");
            }
            if (_walletDAO.deduct(conn, userId, totalAmount) != 1) {
                throw new ShopException(IConstant.STATUS_BALANCE_NOT_ENOUGH, "余额不足，请先充值");
            }

            // 4) 写订单主表 + 逐行写明细并扣减库存（同一事务）
            LocalDateTime now = LocalDateTime.now();
            Order order = new Order();
            order.setOrderId(generateOrderId());
            order.setUserId(userId);
            order.setTotalAmount(totalAmount);
            order.setOrderTime(now);
            if (!_orderDAO.insert(conn, order)) {
                throw new ShopException(IConstant.STATUS_CONFLICT, "订单创建失败，请重试");
            }
            for (PurchaseRecord detail : details) {
                detail.setOrderId(order.getOrderId());
                detail.setOrderTime(now);
                _purchaseDAO.insert(conn, detail);
                int updated = _goodsDAO.updateStock(conn, detail.getGoodsId(), -detail.getQuantity());
                if (updated != 1) {
                    throw new ShopException(IConstant.STATUS_CONFLICT,
                            "库存扣减失败：" + detail.getGoodsName() + "，请重试");
                }
            }
            order.setItems(details);

            conn.commit();
            return order;
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
    public List<Order> queryOrders(String userId) throws SQLException, IOException {
        boolean all = userId == null || userId.isBlank(); // 管理员查看全部订单
        List<Order> orders = all ? _orderDAO.findAll() : _orderDAO.findByUserId(userId);
        List<PurchaseRecord> details = all ? _purchaseDAO.findAll() : _purchaseDAO.findByUserId(userId);

        // 用订单号把明细挂回对应订单：一次查询拿到全部明细，避免 N+1 次数据库往返
        Map<String, Order> orderById = new LinkedHashMap<>();
        for (Order order : orders) {
            orderById.put(order.getOrderId(), order);
        }
        for (PurchaseRecord detail : details) {
            Order order = orderById.get(detail.getOrderId());
            if (order != null) {
                order.getItems().add(detail);
            }
        }
        return orders;
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
        Goods existing = _goodsDAO.findByGoodsId(goods.getGoodsId());
        if (existing == null) {
            throw new ShopException(IConstant.STATUS_GOODS_NOT_FOUND, "商品不存在：" + goods.getGoodsId());
        }
        // 管理表单没有图片字段：本次没带图片地址时保留原有图片，避免把商品图改没了
        if (goods.getImageUrl() == null || goods.getImageUrl().isBlank()) {
            goods.setImageUrl(existing.getImageUrl());
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
     * {@inheritDoc}
     */
    @Override
    public BigDecimal queryBalance(String userId) throws SQLException, IOException {
        Wallet wallet = _walletDAO.findByUserId(userId);
        return wallet == null || wallet.getBalance() == null ? BigDecimal.ZERO : wallet.getBalance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BigDecimal recharge(String userId, BigDecimal amount) throws ShopException, SQLException, IOException {
        if (userId == null || userId.isBlank()) {
            throw new ShopException(IConstant.STATUS_CONFLICT, "未登录或用户信息缺失");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ShopException(IConstant.STATUS_CONFLICT, "充值金额必须为正数");
        }
        _walletDAO.recharge(userId, amount);
        return queryBalance(userId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Promotion> queryPromotions() throws SQLException, IOException {
        return _promotionDAO.findAll();
    }

    /**
     * {@inheritDoc}
     *
     * <p>校验顺序：字段合法性 → 商品是否存在 → 促销编号是否重复 → 入库（"同一商品同一天已有活动"
     * 由数据库唯一约束兜底，冲突时转成友好的业务提示）。</p>
     */
    @Override
    public Promotion addPromotion(Promotion promotion) throws ShopException, SQLException, IOException {
        validatePromotion(promotion);
        if (_promotionDAO.findByPromoId(promotion.getPromoId()) != null) {
            throw new ShopException(IConstant.STATUS_CONFLICT, "促销编号已存在：" + promotion.getPromoId());
        }
        try {
            _promotionDAO.insert(promotion);
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new ShopException(IConstant.STATUS_CONFLICT, conflictMessage(promotion));
        }
        return _promotionDAO.findByPromoId(promotion.getPromoId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promotion updatePromotion(Promotion promotion) throws ShopException, SQLException, IOException {
        validatePromotion(promotion);
        if (_promotionDAO.findByPromoId(promotion.getPromoId()) == null) {
            throw new ShopException(IConstant.STATUS_CONFLICT, "促销活动不存在：" + promotion.getPromoId());
        }
        try {
            if (_promotionDAO.update(promotion) != 1) {
                throw new ShopException(IConstant.STATUS_CONFLICT, "活动修改失败，请重试");
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new ShopException(IConstant.STATUS_CONFLICT, conflictMessage(promotion));
        }
        return _promotionDAO.findByPromoId(promotion.getPromoId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean deletePromotion(String promoId) throws ShopException, SQLException, IOException {
        if (promoId == null || promoId.isBlank()) {
            throw new ShopException(IConstant.STATUS_CONFLICT, "请先选择要删除的促销活动");
        }
        if (_promotionDAO.findByPromoId(promoId) == null) {
            throw new ShopException(IConstant.STATUS_CONFLICT, "促销活动不存在：" + promoId);
        }
        return _promotionDAO.delete(promoId) == 1;
    }

    /**
     * 校验促销活动字段：商品必须存在，折扣率必须落在 (0,1) 之间，星期必须是 0~7。
     *
     * @param promotion 待校验的活动
     * @throws ShopException       字段不合法或商品不存在
     * @throws SQLException        数据库操作异常
     * @throws IOException         数据库配置文件读取异常
     */
    private void validatePromotion(Promotion promotion) throws ShopException, SQLException, IOException {
        if (promotion == null) {
            throw new ShopException(IConstant.STATUS_CONFLICT, "活动信息为空");
        }
        if (promotion.getPromoId() == null || promotion.getPromoId().isBlank()) {
            throw new ShopException(IConstant.STATUS_CONFLICT, "促销编号不能为空");
        }
        if (promotion.getGoodsId() == null || promotion.getGoodsId().isBlank()) {
            throw new ShopException(IConstant.STATUS_CONFLICT, "请选择活动商品");
        }
        if (_goodsDAO.findByGoodsId(promotion.getGoodsId()) == null) {
            throw new ShopException(IConstant.STATUS_GOODS_NOT_FOUND, "商品不存在：" + promotion.getGoodsId());
        }
        BigDecimal rate = promotion.getDiscountRate();
        if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0 || rate.compareTo(BigDecimal.ONE) >= 0) {
            throw new ShopException(IConstant.STATUS_CONFLICT, "折扣率必须在 0 到 1 之间（例如 0.80 表示 8 折）");
        }
        if (promotion.getWeekday() < 0 || promotion.getWeekday() > 7) {
            throw new ShopException(IConstant.STATUS_CONFLICT, "生效星期必须是 0~7（0 表示每天）");
        }
    }

    /**
     * 生成"同一商品同一天已有活动"的友好提示。
     *
     * @param promotion 冲突的活动
     * @return 提示文案
     */
    private String conflictMessage(Promotion promotion) {
        String day = promotion.getWeekday() == 0 ? "每天特价" : "星期 " + promotion.getWeekday();
        return "商品【" + promotion.getGoodsId() + "】在【" + day + "】已经有活动了，同一商品同一天只能有一条活动";
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
