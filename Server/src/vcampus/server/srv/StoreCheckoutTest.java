/*
 * StoreCheckoutTest
 *
 * Version 1.0
 *
 * 2026-09-14
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.server.srv;

import vcampus.common.constant.IConstant;
import vcampus.common.vo.CartItem;
import vcampus.common.vo.Goods;
import vcampus.common.vo.Order;
import vcampus.common.vo.PurchaseRecord;
import vcampus.common.vo.Wallet;
import vcampus.server.dao.DbHelper;
import vcampus.server.dao.GoodsDAO;
import vcampus.server.dao.OrderDAO;
import vcampus.server.dao.WalletDAO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 购物车结算（{@link StoreServerSrv#checkout}）的自测程序，重点验证"一单多商品"的事务原子性：
 *
 * <ol>
 *   <li>正常结算：一单两商品 → 订单主表 1 行 + 明细 2 行，库存与余额按整单扣减；</li>
 *   <li>库存不足回滚：第二件商品超量 → 第一件也不能生效（库存/余额/订单都不变）；</li>
 *   <li>余额不足回滚：整单金额超过余额 → 库存/余额/订单都不变；</li>
 *   <li>订单查询：{@link StoreServerSrv#queryOrders} 能把多条明细按订单号分回同一订单；</li>
 *   <li>今日特价：给测试商品建一条当天生效的 5 折活动后，结算金额与扣款都按特价计算，
 *       且查询商品时会带上折扣信息。</li>
 * </ol>
 *
 * <p>测试使用专用测试商品（TS901/TS902）与测试用户 09010101，运行结束后会删除测试商品与
 * 测试订单、并把扣掉的余额充值回去，因此可以重复运行，不影响演示数据。</p>
 *
 * <p>运行前请确认：已执行过 {@code sql/migration_add_order_group.sql}（或全新安装的
 * {@code sql/shop/vcampus_shop.sql}）；已正确配置 {@code Server/db.properties}；
 * 运行时的工作目录为项目根目录。</p>
 */
public class StoreCheckoutTest {

    /** 测试商品编号（价格便宜，用于正常结算）。 */
    private static final String GOODS_A = "TS901";

    /** 测试商品编号（价格昂贵，用于构造余额不足）。 */
    private static final String GOODS_B = "TS902";

    /** 测试用户（必须已存在钱包记录）。 */
    private static final String TEST_USER = "09010101";

    /** 断言失败计数。 */
    private static int failures = 0;

    /**
     * 程序入口：执行购物车结算自测。
     *
     * @param args 命令行参数（未使用）
     */
    public static void main(String[] args) {
        StoreServerSrv srv = new StoreServerSrv();
        GoodsDAO goodsDAO = new GoodsDAO();
        WalletDAO walletDAO = new WalletDAO();
        OrderDAO orderDAO = new OrderDAO();

        try {
            prepare(goodsDAO);

            BigDecimal balanceBefore = balanceOf(walletDAO);
            int stockABefore = stockOf(goodsDAO, GOODS_A);
            int stockBBefore = stockOf(goodsDAO, GOODS_B);
            int orderCountBefore = orderDAO.findByUserId(TEST_USER).size();
            System.out.println("初始状态：余额=" + balanceBefore.toPlainString()
                    + "，" + GOODS_A + " 库存=" + stockABefore
                    + "，" + GOODS_B + " 库存=" + stockBBefore
                    + "，本人订单数=" + orderCountBefore);

            // ============ 1. 正常结算：一单两商品 ============
            System.out.println();
            System.out.println("=== 1. 正常结算（一单两商品） ===");
            List<CartItem> cart = new ArrayList<>();
            cart.add(new CartItem(GOODS_A, 3));
            cart.add(new CartItem(GOODS_B, 1));
            Order order = srv.checkout(TEST_USER, cart);
            System.out.println("订单号：" + order.getOrderId()
                    + "，明细行数：" + order.getItemCount()
                    + "，总件数：" + order.getTotalQuantity()
                    + "，总金额：" + order.getTotalAmount().toPlainString());

            BigDecimal expectedTotal = priceOf(goodsDAO, GOODS_A).multiply(BigDecimal.valueOf(3))
                    .add(priceOf(goodsDAO, GOODS_B));
            check(order.getItemCount() == 2, "订单应包含 2 条明细，实际 " + order.getItemCount());
            check(order.getTotalQuantity() == 4, "订单总件数应为 4，实际 " + order.getTotalQuantity());
            check(order.getTotalAmount().compareTo(expectedTotal) == 0,
                    "订单总金额应为 " + expectedTotal.toPlainString() + "，实际 " + order.getTotalAmount().toPlainString());
            check(stockOf(goodsDAO, GOODS_A) == stockABefore - 3,
                    "商品A库存应减少 3：" + stockABefore + " -> " + stockOf(goodsDAO, GOODS_A));
            check(stockOf(goodsDAO, GOODS_B) == stockBBefore - 1,
                    "商品B库存应减少 1：" + stockBBefore + " -> " + stockOf(goodsDAO, GOODS_B));
            check(balanceOf(walletDAO).compareTo(balanceBefore.subtract(expectedTotal)) == 0,
                    "余额应扣减 " + expectedTotal.toPlainString() + "，实际剩余 " + balanceOf(walletDAO).toPlainString());

            // 明细行共用同一个订单号
            boolean sameOrderId = true;
            for (PurchaseRecord detail : order.getItems()) {
                sameOrderId &= order.getOrderId().equals(detail.getOrderId());
            }
            check(sameOrderId, "同一订单的所有明细行应共用同一个订单号");
            check(orderDAO.findByUserId(TEST_USER).size() == orderCountBefore + 1,
                    "订单主表应新增 1 行");

            // ============ 2. 订单查询：明细按订单号分组 ============
            System.out.println();
            System.out.println("=== 2. 查询订单（含明细） ===");
            Order queried = null;
            for (Order o : srv.queryOrders(TEST_USER)) {
                if (order.getOrderId().equals(o.getOrderId())) {
                    queried = o;
                }
            }
            check(queried != null, "应能查询到刚结算的订单 " + order.getOrderId());
            if (queried != null) {
                System.out.println("查询到订单：" + queried);
                check(queried.getItemCount() == 2, "查询到的订单应含 2 条明细，实际 " + queried.getItemCount());
                BigDecimal sum = BigDecimal.ZERO;
                for (PurchaseRecord detail : queried.getItems()) {
                    sum = sum.add(detail.getTotalPrice());
                    System.out.println("    明细：" + detail.getGoodsName() + " × " + detail.getQuantity()
                            + " = " + detail.getTotalPrice().toPlainString());
                }
                check(sum.compareTo(queried.getTotalAmount()) == 0,
                        "明细小计之和应等于订单总金额：" + sum.toPlainString() + " vs " + queried.getTotalAmount().toPlainString());
            }

            // ============ 3. 库存不足应整单回滚 ============
            System.out.println();
            System.out.println("=== 3. 库存不足 → 整单回滚 ===");
            BigDecimal balanceBefore3 = balanceOf(walletDAO);
            int stockABefore3 = stockOf(goodsDAO, GOODS_A);
            int orderCountBefore3 = orderDAO.findByUserId(TEST_USER).size();
            List<CartItem> badCart = new ArrayList<>();
            badCart.add(new CartItem(GOODS_A, 1));
            badCart.add(new CartItem(GOODS_B, stockOf(goodsDAO, GOODS_B) + 1000));
            try {
                srv.checkout(TEST_USER, badCart);
                check(false, "库存不足时应抛出业务异常");
            } catch (ShopException e) {
                System.out.println("按预期失败：" + e.getStatusCode() + " " + e.getMessage());
                check(IConstant.STATUS_STOCK_NOT_ENOUGH.equals(e.getStatusCode()), "状态码应为库存不足");
            }
            check(stockOf(goodsDAO, GOODS_A) == stockABefore3,
                    "回滚后商品A库存不应变化：" + stockABefore3 + " -> " + stockOf(goodsDAO, GOODS_A));
            check(balanceOf(walletDAO).compareTo(balanceBefore3) == 0, "回滚后余额不应变化");
            check(orderDAO.findByUserId(TEST_USER).size() == orderCountBefore3, "回滚后不应产生订单");

            // ============ 4. 余额不足应整单回滚 ============
            System.out.println();
            System.out.println("=== 4. 余额不足 → 整单回滚 ===");
            BigDecimal balanceBefore4 = balanceOf(walletDAO);
            int stockABefore4 = stockOf(goodsDAO, GOODS_A);
            int orderCountBefore4 = orderDAO.findByUserId(TEST_USER).size();
            List<CartItem> poorCart = new ArrayList<>();
            poorCart.add(new CartItem(GOODS_A, 1));
            poorCart.add(new CartItem(GOODS_B, 50)); // 50 × 100 = 5000 元，远超余额
            try {
                srv.checkout(TEST_USER, poorCart);
                check(false, "余额不足时应抛出业务异常");
            } catch (ShopException e) {
                System.out.println("按预期失败：" + e.getStatusCode() + " " + e.getMessage());
                check(IConstant.STATUS_BALANCE_NOT_ENOUGH.equals(e.getStatusCode()), "状态码应为余额不足");
            }
            check(stockOf(goodsDAO, GOODS_A) == stockABefore4, "回滚后商品A库存不应变化");
            check(balanceOf(walletDAO).compareTo(balanceBefore4) == 0, "回滚后余额不应变化");
            check(orderDAO.findByUserId(TEST_USER).size() == orderCountBefore4, "回滚后不应产生订单");

            // ============ 5. 空购物车应被拒绝 ============
            System.out.println();
            System.out.println("=== 5. 空购物车 → 拒绝结算 ===");
            try {
                srv.checkout(TEST_USER, new ArrayList<>());
                check(false, "空购物车应抛出业务异常");
            } catch (ShopException e) {
                System.out.println("按预期失败：" + e.getStatusCode() + " " + e.getMessage());
            }

            // ============ 6. 今日特价 → 按折扣价结算 ============
            System.out.println();
            System.out.println("=== 6. 今日特价 → 按折扣价结算 ===");
            // 给测试商品A建一条"今天"的 5 折活动（weekday 用系统的今天，跑完在清理阶段删掉）
            int today = LocalDate.now().getDayOfWeek().getValue();
            insertPromotion("TP901", GOODS_A, new BigDecimal("0.50"), today, "自测临时活动");
            System.out.println("已创建今日活动：商品A 5 折（weekday=" + today + "）");

            // 商品查询也应带上今日折扣
            List<Goods> queriedGoods = srv.queryGoods("购物车测试商品A", null);
            check(!queriedGoods.isEmpty() && queriedGoods.get(0).hasDiscount(),
                    "查询商品时应附上今日折扣信息");
            if (!queriedGoods.isEmpty()) {
                Goods q = queriedGoods.get(0);
                System.out.println("查询到的商品：" + q.getGoodsName() + " 原价 " + q.getPrice().toPlainString()
                        + " → 特价 " + q.getDiscountPrice().toPlainString() + "（" + q.getDiscountLabel() + "）");
                check(q.getDiscountPrice().compareTo(new BigDecimal("4.95")) == 0,
                        "9.90 打 5 折应为 4.95，实际 " + q.getDiscountPrice());
            }

            BigDecimal expectUnit = new BigDecimal("9.90").multiply(new BigDecimal("0.50"))
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal expectTotal = expectUnit.multiply(BigDecimal.valueOf(2));
            BigDecimal balanceBefore6 = balanceOf(walletDAO);
            int stockABefore6 = stockOf(goodsDAO, GOODS_A);
            List<CartItem> promoCart = new ArrayList<>();
            promoCart.add(new CartItem(GOODS_A, 2));
            Order promoOrder = srv.checkout(TEST_USER, promoCart);
            System.out.println("特价订单：" + promoOrder.getOrderId() + "，金额 " + promoOrder.getTotalAmount().toPlainString()
                    + "（原价应为 " + new BigDecimal("19.80").toPlainString() + "）");
            check(promoOrder.getTotalAmount().compareTo(expectTotal) == 0,
                    "特价订单金额应为 " + expectTotal.toPlainString() + "，实际 " + promoOrder.getTotalAmount().toPlainString());
            check(balanceOf(walletDAO).compareTo(balanceBefore6.subtract(expectTotal)) == 0,
                    "余额应按特价扣减 " + expectTotal.toPlainString() + "，实际剩余 " + balanceOf(walletDAO).toPlainString());
            check(stockOf(goodsDAO, GOODS_A) == stockABefore6 - 2, "特价订单也应正常扣减库存");

            // ============ 7. 清理测试数据 ============
            System.out.println();
            System.out.println("=== 7. 清理测试数据 ===");
            List<String> orderIds = new ArrayList<>();
            orderIds.add(order.getOrderId());
            orderIds.add(promoOrder.getOrderId());
            cleanup(orderIds, balanceBefore, goodsDAO, walletDAO);
            System.out.println("已删除测试订单、测试活动与测试商品，并把余额恢复到 " + balanceBefore.toPlainString());

            System.out.println();
            if (failures == 0) {
                System.out.println("全部断言通过，购物车结算事务自测成功。");
            } else {
                System.out.println("有 " + failures + " 项断言失败，请检查上面的输出。");
            }
        } catch (Exception e) {
            System.out.println("自测过程出现异常：" + e);
            e.printStackTrace();
            failures++;
        }

        System.exit(failures == 0 ? 0 : 1);
    }

    /**
     * 准备测试商品（不存在则插入）。
     *
     * @param goodsDAO 商品数据访问对象
     * @throws SQLException 数据库操作异常
     * @throws IOException  数据库配置文件读取异常
     */
    private static void prepare(GoodsDAO goodsDAO) throws SQLException, java.io.IOException {
        if (goodsDAO.findByGoodsId(GOODS_A) == null) {
            goodsDAO.insert(new Goods(GOODS_A, "购物车测试商品A", "测试类别", new BigDecimal("9.90"), 500));
        }
        if (goodsDAO.findByGoodsId(GOODS_B) == null) {
            goodsDAO.insert(new Goods(GOODS_B, "购物车测试商品B", "测试类别", new BigDecimal("100.00"), 500));
        }
    }

    /**
     * 删除测试订单、测试活动、测试商品，并把测试用户余额恢复到执行前的数值。
     *
     * @param orderIds      本次测试产生的订单号
     * @param balanceBefore 测试前的余额
     * @param goodsDAO      商品数据访问对象
     * @param walletDAO     钱包数据访问对象
     * @throws Exception 清理过程中的异常
     */
    private static void cleanup(List<String> orderIds, BigDecimal balanceBefore, GoodsDAO goodsDAO, WalletDAO walletDAO)
            throws Exception {
        try (Connection conn = DbHelper.getConnection()) {
            for (String orderId : orderIds) {
                try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM tblPurchase WHERE orderId = ?")) {
                    pstmt.setString(1, orderId);
                    pstmt.executeUpdate();
                }
                try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM tblOrder WHERE orderId = ?")) {
                    pstmt.setString(1, orderId);
                    pstmt.executeUpdate();
                }
            }
            // 促销表有外键指向 tblGoods，必须先删活动再删商品
            try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM tblPromotion WHERE goodsId IN (?, ?)")) {
                pstmt.setString(1, GOODS_A);
                pstmt.setString(2, GOODS_B);
                pstmt.executeUpdate();
            }
            try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM tblGoods WHERE goodsId IN (?, ?)")) {
                pstmt.setString(1, GOODS_A);
                pstmt.setString(2, GOODS_B);
                pstmt.executeUpdate();
            }
        }
        BigDecimal now = balanceOf(walletDAO);
        BigDecimal diff = balanceBefore.subtract(now);
        if (diff.compareTo(BigDecimal.ZERO) > 0) {
            walletDAO.recharge(TEST_USER, diff);
        }
    }

    /**
     * 插入一条临时促销活动（自测用，清理阶段会删除）。
     *
     * @param promoId      促销编号
     * @param goodsId      商品编号
     * @param discountRate 折扣率
     * @param weekday      生效星期（1=周一 … 7=周日，0=每天）
     * @param remark       活动说明
     * @throws Exception 数据库操作异常
     */
    private static void insertPromotion(String promoId, String goodsId, BigDecimal discountRate,
                                        int weekday, String remark) throws Exception {
        String sql = "INSERT INTO tblPromotion (promoId, goodsId, discountRate, weekday, remark) VALUES (?, ?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE discountRate = VALUES(discountRate), weekday = VALUES(weekday)";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, promoId);
            pstmt.setString(2, goodsId);
            pstmt.setBigDecimal(3, discountRate);
            pstmt.setInt(4, weekday);
            pstmt.setString(5, remark);
            pstmt.executeUpdate();
        }
    }

    /**
     * 查询商品当前库存。
     *
     * @param goodsDAO 商品数据访问对象
     * @param goodsId  商品编号
     * @return 库存数量；商品不存在时返回 -1
     * @throws Exception 查询异常
     */
    private static int stockOf(GoodsDAO goodsDAO, String goodsId) throws Exception {
        Goods goods = goodsDAO.findByGoodsId(goodsId);
        return goods == null ? -1 : goods.getStock();
    }

    /**
     * 查询商品单价。
     *
     * @param goodsDAO 商品数据访问对象
     * @param goodsId  商品编号
     * @return 单价
     * @throws Exception 查询异常
     */
    private static BigDecimal priceOf(GoodsDAO goodsDAO, String goodsId) throws Exception {
        return goodsDAO.findByGoodsId(goodsId).getPrice();
    }

    /**
     * 查询测试用户的当前余额。
     *
     * @param walletDAO 钱包数据访问对象
     * @return 余额（无钱包记录时返回 0）
     * @throws Exception 查询异常
     */
    private static BigDecimal balanceOf(WalletDAO walletDAO) throws Exception {
        Wallet wallet = walletDAO.findByUserId(TEST_USER);
        return wallet == null || wallet.getBalance() == null ? BigDecimal.ZERO : wallet.getBalance();
    }

    /**
     * 断言辅助方法：打印结果并累计失败次数。
     *
     * @param condition 断言条件
     * @param message   断言说明
     */
    private static void check(boolean condition, String message) {
        if (condition) {
            System.out.println("  [通过] " + message);
        } else {
            System.out.println("  [失败] " + message);
            failures++;
        }
    }
}
