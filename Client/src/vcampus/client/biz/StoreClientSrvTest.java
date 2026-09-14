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

import vcampus.common.vo.CartItem;
import vcampus.common.vo.Goods;
import vcampus.common.vo.Message;
import vcampus.common.vo.Order;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link StoreClientSrv} 的端到端验证程序：不涉及界面，直接通过 Socket
 * 向服务器依次发起"查询商品 → 购买商品 → 查询购买记录 → 新增商品 → 删除商品
 * → 修改商品 → 购物车结算 → 查询订单"请求，打印服务器返回的响应，用于确认
 * "客户端 → Socket → 服务器 → 数据库"整条链路是否打通。
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

            System.out.println("=== 3. 测试查询校园卡余额 ===");
            Message b1 = storeClientSrv.queryBalance(testUser);
            System.out.println("余额查询响应：statusCode=" + b1.getStatusCode() + ", 余额=" + b1.getData());

            System.out.println("=== 4. 测试充值 50 元 ===");
            Message rc = storeClientSrv.recharge(testUser, new BigDecimal("50.00"));
            System.out.println("充值响应：statusCode=" + rc.getStatusCode() + ", 充值后余额=" + rc.getData());

            System.out.println("=== 5. 测试购买商品（G001 买2件，会扣余额）===");
            Message p = storeClientSrv.purchaseGoods(testUser, "G001", 2);
            System.out.println("购买响应：statusCode=" + p.getStatusCode() + ", data=" + p.getData());

            System.out.println("=== 6. 购买后再查余额（应已扣款）===");
            Message b2 = storeClientSrv.queryBalance(testUser);
            System.out.println("余额查询响应：statusCode=" + b2.getStatusCode() + ", 余额=" + b2.getData());

            System.out.println("=== 7. 测试查询购买记录 ===");
            Message r = storeClientSrv.queryPurchaseRecords(testUser);
            System.out.println("记录响应：statusCode=" + r.getStatusCode() + ", data=" + r.getData());

            System.out.println("=== 8. 测试新增商品（管理员）===");
            Goods newGoods = new Goods("ST001", "客户端测试商品", "测试类别", new BigDecimal("19.90"), 10);
            Message a = storeClientSrv.addGoods(newGoods);
            System.out.println("新增响应：statusCode=" + a.getStatusCode() + ", data=" + a.getData());

            System.out.println("=== 9. 测试删除商品（G001 已有购买记录，应禁止）===");
            Message d = storeClientSrv.deleteGoods("G001");
            System.out.println("删除G001响应：statusCode=" + d.getStatusCode() + ", data=" + d.getData());

            System.out.println("=== 10. 测试修改商品（ST001）===");
            Goods update = new Goods("ST001", "客户端测试商品-改", "测试类别", new BigDecimal("29.90"), 20);
            Message u = storeClientSrv.updateGoods(update);
            System.out.println("修改响应：statusCode=" + u.getStatusCode() + ", data=" + u.getData());

            System.out.println("=== 11. 购物车结算前的余额 ===");
            Message b3 = storeClientSrv.queryBalance(testUser);
            System.out.println("结算前余额：statusCode=" + b3.getStatusCode() + ", 余额=" + b3.getData());

            System.out.println("=== 12. 测试购物车结算（一单多商品：G004×2 + G005×3）===");
            List<CartItem> cart = new ArrayList<>();
            cart.add(new CartItem("G004", 2));
            cart.add(new CartItem("G005", 3));
            Message co = storeClientSrv.checkout(testUser, cart);
            System.out.println("结算响应：statusCode=" + co.getStatusCode());
            if (co.getData() instanceof Order order) {
                System.out.println("生成的订单：" + order);
                for (vcampus.common.vo.PurchaseRecord item : order.getItems()) {
                    System.out.println("    明细：" + item.getGoodsName() + " × " + item.getQuantity()
                            + " = " + item.getTotalPrice().toPlainString());
                }
            } else {
                System.out.println("结算返回数据：" + co.getData());
            }

            System.out.println("=== 13. 结算后再查余额（应扣减整单金额）===");
            Message b4 = storeClientSrv.queryBalance(testUser);
            System.out.println("结算后余额：statusCode=" + b4.getStatusCode() + ", 余额=" + b4.getData());

            System.out.println("=== 14. 测试查询订单（含明细）===");
            Message oq = storeClientSrv.queryOrders(testUser);
            System.out.println("订单查询响应：statusCode=" + oq.getStatusCode());
            if (oq.getData() instanceof List<?> orders) {
                System.out.println("订单数=" + orders.size());
                for (Object obj : orders) {
                    if (obj instanceof Order order) {
                        System.out.println("    订单 " + order.getOrderId() + "：" + order.getItemCount()
                                + " 种 / " + order.getTotalQuantity() + " 件，共 "
                                + order.getTotalAmount().toPlainString() + " 元");
                    }
                }
            }

            System.out.println("=== 15. 测试库存不足的结算（G006 买 9999 件，应整单失败）===");
            List<CartItem> badCart = new ArrayList<>();
            badCart.add(new CartItem("G004", 1));
            badCart.add(new CartItem("G006", 9999));
            Message bad = storeClientSrv.checkout(testUser, badCart);
            System.out.println("结算响应：statusCode=" + bad.getStatusCode() + ", data=" + bad.getData());

            System.out.println("=== 16. 清理：删除本次测试新增的商品 ST001（应删除成功）===");
            Message clean = storeClientSrv.deleteGoods("ST001");
            System.out.println("删除ST001响应：statusCode=" + clean.getStatusCode() + ", data=" + clean.getData());

            System.out.println();
            System.out.println("=== 商店模块 Socket 端到端自测完成 ===");
        } catch (Exception e) {
            System.err.println("商店客户端通信测试失败：" + e.getMessage());
            System.err.println("请确认服务器 vcampus.server.srv.Server 是否已启动。");
            e.printStackTrace();
        }
    }
}
