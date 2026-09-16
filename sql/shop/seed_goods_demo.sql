-- ============================================================
-- Vcampus 商店模块 —— 商品演示数据（丰富版，22 件商品）
--
-- 用途：给商店补齐一批覆盖各品类的演示商品，让商品商城页签更真实
--       （类别筛选、库存差异、价格区间都有内容可演示）。
-- 特点：全部用 INSERT IGNORE，按 goodsId 幂等，可重复执行；
--       已有商品（G001~G007）不会被覆盖，也不会删除任何数据。
-- 图片：G001~G007 带真实商品图；G008 以后 imageUrl 留空，
--       界面会自动用"类别色块 + 类别图标"占位；
--       管理员也可以在【商品管理】里填"图片路径"随时补图。
-- 依赖：已执行过 sql/shop/vcampus_shop.sql（tblGoods 已存在）。
-- 用法：mysql -u root -p --default-character-set=utf8mb4 < sql/shop/seed_goods_demo.sql
-- ============================================================

USE vCampus;

INSERT IGNORE INTO tblGoods (goodsId, goodsName, category, price, stock, imageUrl) VALUES
    -- ===== 饮料 =====
    ('G001', '农夫山泉',         '饮料',     2.00, 100, 'Client/src/vcampus/client/view/assets/store/G001.jpg'),
    ('G002', '可口可乐',         '饮料',     3.50,  80, 'Client/src/vcampus/client/view/assets/store/G002.jpg'),
    ('G012', '蒙牛纯牛奶',       '饮料',     3.00,  90, NULL),
    ('G013', '雀巢咖啡',         '饮料',     6.00,  40, NULL),
    ('G014', '冰红茶',           '饮料',     3.00,  70, NULL),
    -- ===== 食品 =====
    ('G003', '面包',             '食品',     5.00,  50, 'Client/src/vcampus/client/view/assets/store/G003.jpg'),
    ('G008', '康师傅红烧牛肉面', '食品',     5.50,  60, NULL),
    ('G009', '奥利奥饼干',       '食品',     9.90,  45, NULL),
    ('G010', '乐事薯片',         '食品',     6.50,  55, NULL),
    ('G011', '士力架',           '食品',     4.00,  80, NULL),
    -- ===== 文具 =====
    ('G004', '笔记本',           '文具',     8.00, 200, 'Client/src/vcampus/client/view/assets/store/G004.jpg'),
    ('G005', '中性笔',           '文具',     2.00, 300, 'Client/src/vcampus/client/view/assets/store/G005.jpg'),
    ('G015', '晨光橡皮',         '文具',     1.50, 150, NULL),
    ('G016', '得力订书机',       '文具',    12.00,  25, NULL),
    ('G017', '便利贴',           '文具',     4.50, 120, NULL),
    -- ===== 生活用品 =====
    ('G007', '洗衣液',           '生活用品', 15.00, 40, 'Client/src/vcampus/client/view/assets/store/G007.jpg'),
    ('G018', '抽纸',             '生活用品',  6.90,  65, NULL),
    ('G019', '洗发水',           '生活用品', 29.90,  20, NULL),
    -- ===== 数码 =====
    ('G006', 'U盘64G',           '数码',    45.00,  30, 'Client/src/vcampus/client/view/assets/store/G006.jpg'),
    ('G020', '无线鼠标',         '数码',    39.00,  18, NULL),
    ('G021', '数据线',           '数码',    15.00, 100, NULL),
    ('G022', '充电宝',           '数码',    89.00,   5, NULL);

-- 核对：按类别统计商品数量与库存
SELECT category AS '类别', COUNT(*) AS '商品数', SUM(stock) AS '总库存'
FROM tblGoods GROUP BY category ORDER BY category;

SELECT COUNT(*) AS '商品总数' FROM tblGoods;
