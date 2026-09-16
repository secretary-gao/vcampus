-- ============================================================
-- Vcampus 商店模块 —— 每日特价活动演示数据
--
-- 规则：weekday 1=周一 … 7=周日，0=每天特价；同一个商品同一天只允许一条活动。
--       服务器按"今天是星期几"取当日活动并据此计价，所以**每天都看得到特价商品**。
-- 说明：22 件商品全覆盖（每天 2~3 件轮换，另有 2 件常年特价），
--       想按别的口径配活动，直接改 tblPromotion 表即可。
-- 幂等：INSERT IGNORE，可重复执行（已存在的 promoId 或"商品+星期"组合会跳过）。
-- 依赖：已执行 sql/migration_add_promotion.sql（或全新安装的 sql/shop/vcampus_shop.sql）。
-- 用法：mysql -u root -p --default-character-set=utf8mb4 < sql/shop/seed_promotion_demo.sql
-- ============================================================

USE vCampus;

INSERT IGNORE INTO tblPromotion (promoId, goodsId, discountRate, weekday, remark) VALUES
    -- ===== 每天特价（weekday = 0）=====
    ('P001', 'G001', 0.90, 0, '日常特价·农夫山泉'),
    ('P002', 'G005', 0.80, 0, '日常特价·中性笔'),
    -- ===== 周一：饮料日 =====
    ('P003', 'G002', 0.70, 1, '周一饮料日'),
    ('P004', 'G012', 0.85, 1, '周一饮料日'),
    ('P005', 'G014', 0.75, 1, '周一饮料日'),
    -- ===== 周二：食品日 =====
    ('P006', 'G003', 0.80, 2, '周二食品日'),
    ('P007', 'G009', 0.70, 2, '周二食品日'),
    ('P008', 'G011', 0.85, 2, '周二食品日'),
    -- ===== 周三：文具日 =====
    ('P009', 'G004', 0.75, 3, '周三文具日'),
    ('P010', 'G015', 0.60, 3, '周三文具日'),
    ('P011', 'G017', 0.80, 3, '周三文具日'),
    -- ===== 周四：数码日 =====
    ('P012', 'G006', 0.85, 4, '周四数码日'),
    ('P013', 'G020', 0.75, 4, '周四数码日'),
    ('P014', 'G021', 0.70, 4, '周四数码日'),
    -- ===== 周五：生活日 =====
    ('P015', 'G007', 0.80, 5, '周五生活日'),
    ('P016', 'G018', 0.85, 5, '周五生活日'),
    ('P017', 'G019', 0.70, 5, '周五生活日'),
    -- ===== 周六：周末特惠 =====
    ('P018', 'G008', 0.80, 6, '周末特惠'),
    ('P019', 'G010', 0.75, 6, '周末特惠'),
    ('P020', 'G013', 0.75, 6, '周末特惠'),
    -- ===== 周日：周末特惠 =====
    ('P021', 'G016', 0.80, 7, '周末特惠'),
    ('P022', 'G022', 0.85, 7, '周末特惠');

-- 核对 1：每天有几件特价商品
SELECT weekday AS '星期(1=周一,0=每天)', COUNT(*) AS '特价商品数'
FROM tblPromotion GROUP BY weekday ORDER BY weekday;

-- 核对 2：今天的特价商品（按当前日期算，WEEKDAY()+1 得到 1=周一…7=周日）
SELECT p.promoId, p.goodsId, g.goodsName AS '商品', g.price AS '原价',
       p.discountRate AS '折扣率',
       ROUND(g.price * p.discountRate, 2) AS '特价',
       p.remark AS '活动'
FROM tblPromotion p JOIN tblGoods g ON g.goodsId = p.goodsId
WHERE p.weekday IN (0, WEEKDAY(CURDATE()) + 1)
ORDER BY p.discountRate, p.goodsId;
