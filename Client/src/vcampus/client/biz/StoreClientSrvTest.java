/*
 * StoreClientSrvTest
 *
 * Version 1.0
 *
 * 2026-09-01
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.client.biz;

import vcampus.common.vo.Goods;
import vcampus.common.vo.Message;

import java.math.BigDecimal;
import java.util.List;

/**
 * {@link StoreClientSrv} 的端到端验证程序：不涉及界面，直接通过 Socket
 * 向服务器依次发起"查询商品 → 购买商品 → 查询购买记录 → 新增商品 → 删除商品
 * → 修改商品"请求，打印服务器返回的响应，用于确认"客户端 → Socket → 服务器
 * → 数据库"整条链路是否打通。
 *
 * <p>运行前请先启动 {@code vcampus.server.srv.Server}。</p>
 */
public class StoreClientSrvTest {

    /**
     * 程序入口：依次测试商店模块的六个请求。
     *
     * @param args 命令行参数（未使用）
     */
    public static void main(String[] args) {
        IStoreClientSrv storeClientSrv = new StoreClientSrv();
        String testUser = "09010101";

        try {
            System.out.println("=== 1. 测试查询全部商品 ===");
            Message q = storeClientSrv.queryGoods(null, null);
            System.out.println("查询响应：statusCode=" + q.getStatusCode()
                    + ", data类型=" + (q.getData() == null ? "null" : q.getData().getClass().getSimpleName()));
            if (q.getData() instanceof List<?> list) {
                System.out.println("商品总数=" + list.size());
                if (!list.isEmpty()) {
                    System.out.println("第一条=" + list.get(0));
                }
            }

            System.out.println("=== 2. 测试按关键字查询（饮料）===");
            Message q2 = storeClientSrv.queryGoods("饮料", null);
            System.out.println("关键字[饮料]查询响应：statusCode=" + q2.getStatusCode() + ", data=" + q2.getData());

            System.out.println("=== 3. 测试购买商品（G001 买2件）===");
            Message p = storeClientSrv.purchaseGoods(testUser, "G001", 2);
            System.out.println("购买响应：statusCode=" + p.getStatusCode() + ", data=" + p.getData());

            System.out.println("=== 4. 测试查询购买记录 ===");
            Message r = storeClientSrv.queryPurchaseRecords(testUser);
            System.out.println("记录响应：statusCode=" + r.getStatusCode() + ", data=" + r.getData());

            System.out.println("=== 5. 测试新增商品（管理员）===");
            Goods newGoods = new Goods("ST001", "客户端测试商品", "测试类别", new BigDecimal("19.90"), 10);
            Message a = storeClientSrv.addGoods(newGoods);
            System.out.println("新增响应：statusCode=" + a.getStatusCode() + ", data=" + a.getData());

            System.out.println("=== 6. 测试删除商品（G001 已有购买记录，应禁止）===");
            Message d = storeClientSrv.deleteGoods("G001");
            System.out.println("删除G001响应：statusCode=" + d.getStatusCode() + ", data=" + d.getData());

            System.out.println("=== 7. 测试修改商品（ST001）===");
            Goods update = new Goods("ST001", "客户端测试商品-改", "测试类别", new BigDecimal("29.90"), 20);
            Message u = storeClientSrv.updateGoods(update);
            System.out.println("修改响应：statusCode=" + u.getStatusCode() + ", data=" + u.getData());

            System.out.println();
            System.out.println("=== 商店模块 Socket 端到端自测完成 ===");
        } catch (Exception e) {
            System.err.println("商店客户端通信测试失败：" + e.getMessage());
            System.err.println("请确认服务器 vcampus.server.srv.Server 是否已启动。");
            e.printStackTrace();
        }
    }
}
