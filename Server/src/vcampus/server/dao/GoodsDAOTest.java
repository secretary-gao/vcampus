/*
 * GoodsDAOTest
 *
 * Version 1.0
 *
 * 2026-09-01
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.server.dao;

import vcampus.common.vo.Goods;
import vcampus.common.vo.PurchaseRecord;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * {@link GoodsDAO} 与 {@link PurchaseDAO} 的功能验证程序：模拟一次
 * "插入商品 → 回查刚插入的商品 → 插入购买记录并扣减库存（事务）→ 查询购买记录"
 * 的数据层流程，证明商店模块数据层可以正常写入与读取。
 *
 * <p>运行前请确认：已执行 {@code sql/vcampus_shop.sql} 建表；已正确配置
 * {@code Server/db.properties}；运行时的工作目录为项目根目录。</p>
 */
public class GoodsDAOTest {

    /**
     * 程序入口：执行商店模块数据层自测。
     *
     * @param args 命令行参数（未使用）
     */
    public static void main(String[] args) {
        GoodsDAO goodsDAO = new GoodsDAO();
        PurchaseDAO purchaseDAO = new PurchaseDAO();

        String testGoodsId = "TS001";
        String testUser = "09010101";

        try {
            // 1) 插入商品
            System.out.println("=== 1. 插入商品 ===");
            Goods exist = goodsDAO.findByGoodsId(testGoodsId);
            if (exist != null) {
                System.out.println("商品 [" + testGoodsId + "] 已存在，跳过插入。");
            } else {
                Goods newGoods = new Goods(testGoodsId, "测试商品", "测试类别", new BigDecimal("9.90"), 50);
                boolean ok = goodsDAO.insert(newGoods);
                System.out.println(ok ? "插入商品成功：" + newGoods : "插入商品失败：" + testGoodsId);
            }

            // 2) 回查刚插入的商品
            System.out.println("=== 2. 回查刚插入的商品 ===");
            Goods queried = goodsDAO.findByGoodsId(testGoodsId);
            System.out.println(queried != null ? "查询到刚插入的商品：" + queried : "查询失败：未找到商品 " + testGoodsId);

            // 3) 条件查询（关键字 + 类别）
            System.out.println("=== 3. 条件查询商品 ===");
            List<Goods> byKeyword = goodsDAO.queryByCondition("饮料", null);
            System.out.println("按关键字[饮料]查询到 " + byKeyword.size() + " 条商品");
            List<Goods> all = goodsDAO.queryByCondition(null, null);
            System.out.println("查询全部商品共 " + all.size() + " 条");

            // 4) 插入购买记录 + 扣减库存（同一连接，模拟购买事务）
            System.out.println("=== 4. 插入购买记录并扣减库存（事务） ===");
            int beforeStock = queried.getStock();
            PurchaseRecord record = new PurchaseRecord();
            record.setOrderId("OrderTest" + System.currentTimeMillis());
            record.setUserId(testUser);
            record.setGoodsId(testGoodsId);
            record.setGoodsName(queried.getGoodsName());
            record.setQuantity(2);
            record.setTotalPrice(queried.getPrice().multiply(BigDecimal.valueOf(2)));
            record.setOrderTime(LocalDateTime.now());

            try (Connection conn = DbHelper.getConnection()) {
                conn.setAutoCommit(false);
                try {
                    purchaseDAO.insert(conn, record);
                    int updated = goodsDAO.updateStock(conn, testGoodsId, -record.getQuantity());
                    conn.commit();
                    System.out.println("购买记录插入并扣减库存成功，库存 " + beforeStock + " -> "
                            + (beforeStock - record.getQuantity()) + (updated == 1 ? "（扣减生效）" : "（扣减未生效）"));
                } catch (SQLException e) {
                    conn.rollback();
                    throw e;
                }
            }
            System.out.println("订单号：" + record.getOrderId() + "，总价：" + record.getTotalPrice());

            // 5) 查询购买记录
            System.out.println("=== 5. 查询购买记录 ===");
            List<PurchaseRecord> records = purchaseDAO.findByUserId(testUser);
            System.out.println("查询到用户 [" + testUser + "] 的购买记录 " + records.size() + " 条");
            if (!records.isEmpty()) {
                System.out.println("最近一条：" + records.get(0));
            }

            // 6) 删除商品的校验（存在购买记录时应禁止删除）
            System.out.println("=== 6. 校验删除商品（存在购买记录应禁止） ===");
            boolean hasRecord = purchaseDAO.existsByGoodsId(testGoodsId);
            System.out.println("商品 [" + testGoodsId + "] 是否存在购买记录：" + hasRecord
                    + (hasRecord ? "（按业务规则应禁止删除）" : "（可删除）"));

            System.out.println();
            System.out.println("=== 商店模块数据层自测完成 ===");
        } catch (SQLException | IOException e) {
            System.err.println("数据层自测失败：" + e.getMessage());
            System.err.println("请检查：1) MySQL 服务是否启动；2) Server/db.properties 是否已正确填写；"
                    + "3) 是否已执行 sql/shop/vcampus_shop.sql。");
            e.printStackTrace();
        }
    }
}
