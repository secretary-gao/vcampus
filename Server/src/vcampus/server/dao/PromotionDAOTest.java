/*
 * PromotionDAOTest
 *
 * Version 1.0
 *
 * 2026-09-16
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.server.dao;

import vcampus.common.vo.Goods;
import vcampus.common.vo.Promotion;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

/**
 * {@link PromotionDAO} 的功能验证程序，同时校验"每日特价"相关的实体类计算逻辑。
 *
 * <ol>
 *   <li>查询全部活动：应覆盖 7 天（每天都有特价商品）；</li>
 *   <li>按星期查询：每天的生效活动 = 当天活动 + "每天特价"（weekday=0）；</li>
 *   <li>折扣标签与折扣价计算：0.80 → "8折"、0.85 → "8.5折"，特价 = 原价 × 折扣率（四舍五入到分）；</li>
 *   <li>管理员配置用到的增/改/删：新增 → 回查 → 修改 → 删除，并验证"同一商品同一天只能有一条活动"
 *       由数据库唯一约束兜底（服务器据此转成友好提示）。</li>
 * </ol>
 *
 * <p>运行前请确认：已执行 {@code sql/migration_add_promotion.sql} 与
 * {@code sql/shop/seed_promotion_demo.sql}；已正确配置 {@code Server/db.properties}；
 * 运行时的工作目录为项目根目录。</p>
 */
public class PromotionDAOTest {

    /** 断言失败计数。 */
    private static int failures = 0;

    /**
     * 程序入口：执行促销数据层自测。
     *
     * @param args 命令行参数（未使用）
     */
    public static void main(String[] args) {
        PromotionDAO dao = new PromotionDAO();
        try {
            System.out.println("=== 1. 查询全部促销活动 ===");
            List<Promotion> all = dao.findAll();
            System.out.println("活动总数=" + all.size());
            check(!all.isEmpty(), "应有促销活动数据（没有的话先跑 sql/shop/seed_promotion_demo.sql）");

            System.out.println();
            System.out.println("=== 2. 按星期查询当日生效活动（含每天特价 weekday=0）===");
            String[] names = {"", "周一", "周二", "周三", "周四", "周五", "周六", "周日"};
            for (int weekday = 1; weekday <= 7; weekday++) {
                List<Promotion> today = dao.findByWeekday(weekday);
                StringBuilder sb = new StringBuilder();
                for (Promotion p : today) {
                    sb.append(p.getGoodsName()).append("(").append(p.getDiscountLabel()).append(") ");
                }
                System.out.println("  " + names[weekday] + "：" + today.size() + " 件  " + sb);
                check(!today.isEmpty(), names[weekday] + " 应有特价商品");
                boolean allMatch = true;
                for (Promotion p : today) {
                    allMatch &= (p.getWeekday() == weekday || p.getWeekday() == 0);
                }
                check(allMatch, names[weekday] + " 取到的活动只能是当天活动或每天特价");
            }

            List<Promotion> everyday = dao.findByWeekday(0);
            check(everyday.stream().allMatch(p -> p.getWeekday() == 0), "查询 weekday=0 只应返回每天特价");

            System.out.println();
            System.out.println("=== 3. 折扣标签与折扣价计算 ===");
            Promotion p80 = new Promotion("PX1", "G001", new BigDecimal("0.80"), 1, "测试");
            Promotion p85 = new Promotion("PX2", "G002", new BigDecimal("0.85"), 0, "测试");
            check("8折".equals(p80.getDiscountLabel()), "0.80 的标签应为 8折，实际 " + p80.getDiscountLabel());
            check("8.5折".equals(p85.getDiscountLabel()), "0.85 的标签应为 8.5折，实际 " + p85.getDiscountLabel());
            check("周一".equals(p80.getWeekdayLabel()), "weekday=1 应显示周一");
            check("每天".equals(p85.getWeekdayLabel()), "weekday=0 应显示每天");

            Goods g = new Goods("G001", "农夫山泉", "饮料", new BigDecimal("2.00"), 100);
            check(!g.hasDiscount(), "未附折扣时 hasDiscount 应为 false");
            check(g.getEffectivePrice().compareTo(new BigDecimal("2.00")) == 0, "无折扣时成交价应等于原价");

            g.setDiscountRate(new BigDecimal("0.80"));
            check(g.hasDiscount(), "附上 0.8 折扣后 hasDiscount 应为 true");
            check(g.getDiscountPrice().compareTo(new BigDecimal("1.60")) == 0,
                    "2.00 × 0.8 应为 1.60，实际 " + g.getDiscountPrice());
            check("8折".equals(g.getDiscountLabel()), "折扣标签应为 8折，实际 " + g.getDiscountLabel());

            Goods g2 = new Goods("G003", "面包", "食品", new BigDecimal("9.90"), 50);
            g2.setDiscountRate(new BigDecimal("0.85"));
            check(g2.getDiscountPrice().compareTo(new BigDecimal("8.42")) == 0,
                    "9.90 × 0.85 应四舍五入为 8.42，实际 " + g2.getDiscountPrice());

            System.out.println();
            System.out.println("=== 4. 活动的增 / 改 / 删（管理员配置用）===");
            String tempPromoId = "TPX1";
            // 清掉上次可能残留的测试活动
            if (dao.findByPromoId(tempPromoId) != null) {
                dao.delete(tempPromoId);
            }
            // 新增：G001 在周四打 6.6 折（G001 现有活动是"每天特价"，周四没有，不会撞唯一约束）
            Promotion temp = new Promotion(tempPromoId, "G001", new BigDecimal("0.66"), 4, "自测临时活动");
            check(dao.insert(temp), "新增活动应成功");
            Promotion loaded = dao.findByPromoId(tempPromoId);
            check(loaded != null, "应能按活动编号查到刚新增的活动");
            if (loaded != null) {
                System.out.println("  新增后：" + loaded.getGoodsName() + " " + loaded.getDiscountLabel()
                        + " " + loaded.getWeekdayLabel());
                check(loaded.getDiscountRate().compareTo(new BigDecimal("0.66")) == 0, "折扣率应为 0.66");
                check(loaded.getWeekday() == 4, "生效星期应为 4（周四）");
            }

            // 修改：改成 5.5 折 + 活动说明
            temp.setDiscountRate(new BigDecimal("0.55"));
            temp.setRemark("自测临时活动-已修改");
            check(dao.update(temp) == 1, "修改活动应影响 1 行");
            Promotion updated = dao.findByPromoId(tempPromoId);
            check(updated != null && updated.getDiscountRate().compareTo(new BigDecimal("0.55")) == 0,
                    "修改后折扣率应为 0.55");

            // 同一商品同一天重复配置 → 数据库唯一约束应拦下来（服务器据此返回友好提示）
            try {
                dao.insert(new Promotion("TPX2", "G001", new BigDecimal("0.70"), 4, "故意冲突"));
                check(false, "同一商品同一天重复配置应被唯一约束拦下");
            } catch (java.sql.SQLIntegrityConstraintViolationException e) {
                System.out.println("  按预期被数据库拦下：" + e.getMessage());
                check(true, "同一商品同一天重复配置被唯一约束拦下");
            } catch (SQLException e) {
                check(false, "应抛出唯一约束异常，实际：" + e);
            }

            // 删除
            check(dao.delete(tempPromoId) == 1, "删除活动应影响 1 行");
            check(dao.findByPromoId(tempPromoId) == null, "删除后应查不到该活动");
            check(dao.findAll().size() == all.size(), "增删一轮后活动总数应回到原值 " + all.size());

            System.out.println();
            if (failures == 0) {
                System.out.println("全部断言通过，促销（每日特价）数据层自测成功。");
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
