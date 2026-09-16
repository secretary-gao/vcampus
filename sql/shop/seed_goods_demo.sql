-- ============================================================
-- Vcampus 商店模块 —— 商品演示数据（丰富版，22 件商品）
--
-- 用途：给商店补齐一批覆盖各品类的演示商品，让商品商城页签更真实
--       （类别筛选、库存差异、价格区间都有内容可演示）。
-- 特点：全部用 INSERT IGNORE，按 goodsId 幂等，可重复执行；
--       已有商品不会被覆盖，也不会删除任何数据。
-- 图片：22 件商品都配了真实商品图（400×400 JPEG，放在
--       Client/src/vcampus/client/view/assets/store/G0XX.jpg）。
--       脚本末尾还会给"图片路径为空"的商品补上路径，但不会覆盖已有路径，
--       所以管理员在【商品管理】里手工改过的图片不会被冲掉；
--       图片路径为空的商品，界面会自动用"类别色块 + 类别图标"占位。
-- 依赖：已执行过 sql/shop/vcampus_shop.sql（tblGoods 已存在）。
-- 用法：mysql -u root -p --default-character-set=utf8mb4 < sql/shop/seed_goods_demo.sql
-- ============================================================

USE vCampus;

INSERT IGNORE INTO tblGoods (goodsId, goodsName, category, price, stock, imageUrl) VALUES
    -- ===== 饮料 =====
    ('G001', '农夫山泉',         '饮料',     2.00, 100, 'Client/src/vcampus/client/view/assets/store/G001.jpg'),
    ('G002', '可口可乐',         '饮料',     3.50,  80, 'Client/src/vcampus/client/view/assets/store/G002.jpg'),
    ('G012', '蒙牛纯牛奶',       '饮料',     3.00,  90, 'Client/src/vcampus/client/view/assets/store/G012.jpg'),
    ('G013', '雀巢咖啡',         '饮料',     6.00,  40, 'Client/src/vcampus/client/view/assets/store/G013.jpg'),
    ('G014', '冰红茶',           '饮料',     3.00,  70, 'Client/src/vcampus/client/view/assets/store/G014.jpg'),
    -- ===== 食品 =====
    ('G003', '面包',             '食品',     5.00,  50, 'Client/src/vcampus/client/view/assets/store/G003.jpg'),
    ('G008', '康师傅红烧牛肉面', '食品',     5.50,  60, 'Client/src/vcampus/client/view/assets/store/G008.jpg'),
    ('G009', '奥利奥饼干',       '食品',     9.90,  45, 'Client/src/vcampus/client/view/assets/store/G009.jpg'),
    ('G010', '乐事薯片',         '食品',     6.50,  55, 'Client/src/vcampus/client/view/assets/store/G010.jpg'),
    ('G011', '士力架',           '食品',     4.00,  80, 'Client/src/vcampus/client/view/assets/store/G011.jpg'),
    -- ===== 文具 =====
    ('G004', '笔记本',           '文具',     8.00, 200, 'Client/src/vcampus/client/view/assets/store/G004.jpg'),
    ('G005', '中性笔',           '文具',     2.00, 300, 'Client/src/vcampus/client/view/assets/store/G005.jpg'),
    ('G015', '晨光橡皮',         '文具',     1.50, 150, 'Client/src/vcampus/client/view/assets/store/G015.jpg'),
    ('G016', '得力订书机',       '文具',    12.00,  25, 'Client/src/vcampus/client/view/assets/store/G016.jpg'),
    ('G017', '便利贴',           '文具',     4.50, 120, 'Client/src/vcampus/client/view/assets/store/G017.jpg'),
    -- ===== 生活用品 =====
    ('G007', '洗衣液',           '生活用品', 15.00, 40, 'Client/src/vcampus/client/view/assets/store/G007.jpg'),
    ('G018', '抽纸',             '生活用品',  6.90,  65, 'Client/src/vcampus/client/view/assets/store/G018.jpg'),
    ('G019', '洗发水',           '生活用品', 29.90,  20, 'Client/src/vcampus/client/view/assets/store/G019.jpg'),
    -- ===== 数码 =====
    ('G006', 'U盘64G',           '数码',    45.00,  30, 'Client/src/vcampus/client/view/assets/store/G006.jpg'),
    ('G020', '无线鼠标',         '数码',    39.00,  18, 'Client/src/vcampus/client/view/assets/store/G020.jpg'),
    ('G021', '数据线',           '数码',    15.00, 100, 'Client/src/vcampus/client/view/assets/store/G021.jpg'),
    ('G022', '充电宝',           '数码',    89.00,   5, 'Client/src/vcampus/client/view/assets/store/G022.jpg');

-- ------------------------------------------------------------
-- 补图片路径：只给"还没有图片"的商品补上（已有路径的不动，
--   避免覆盖管理员在【商品管理】里自定义的图片）
-- ------------------------------------------------------------
UPDATE tblGoods SET imageUrl = 'Client/src/vcampus/client/view/assets/store/G001.jpg' WHERE goodsId = 'G001' AND (imageUrl IS NULL OR imageUrl = '');
UPDATE tblGoods SET imageUrl = 'Client/src/vcampus/client/view/assets/store/G002.jpg' WHERE goodsId = 'G002' AND (imageUrl IS NULL OR imageUrl = '');
UPDATE tblGoods SET imageUrl = 'Client/src/vcampus/client/view/assets/store/G003.jpg' WHERE goodsId = 'G003' AND (imageUrl IS NULL OR imageUrl = '');
UPDATE tblGoods SET imageUrl = 'Client/src/vcampus/client/view/assets/store/G004.jpg' WHERE goodsId = 'G004' AND (imageUrl IS NULL OR imageUrl = '');
UPDATE tblGoods SET imageUrl = 'Client/src/vcampus/client/view/assets/store/G005.jpg' WHERE goodsId = 'G005' AND (imageUrl IS NULL OR imageUrl = '');
UPDATE tblGoods SET imageUrl = 'Client/src/vcampus/client/view/assets/store/G006.jpg' WHERE goodsId = 'G006' AND (imageUrl IS NULL OR imageUrl = '');
UPDATE tblGoods SET imageUrl = 'Client/src/vcampus/client/view/assets/store/G007.jpg' WHERE goodsId = 'G007' AND (imageUrl IS NULL OR imageUrl = '');
UPDATE tblGoods SET imageUrl = 'Client/src/vcampus/client/view/assets/store/G008.jpg' WHERE goodsId = 'G008' AND (imageUrl IS NULL OR imageUrl = '');
UPDATE tblGoods SET imageUrl = 'Client/src/vcampus/client/view/assets/store/G009.jpg' WHERE goodsId = 'G009' AND (imageUrl IS NULL OR imageUrl = '');
UPDATE tblGoods SET imageUrl = 'Client/src/vcampus/client/view/assets/store/G010.jpg' WHERE goodsId = 'G010' AND (imageUrl IS NULL OR imageUrl = '');
UPDATE tblGoods SET imageUrl = 'Client/src/vcampus/client/view/assets/store/G011.jpg' WHERE goodsId = 'G011' AND (imageUrl IS NULL OR imageUrl = '');
UPDATE tblGoods SET imageUrl = 'Client/src/vcampus/client/view/assets/store/G012.jpg' WHERE goodsId = 'G012' AND (imageUrl IS NULL OR imageUrl = '');
UPDATE tblGoods SET imageUrl = 'Client/src/vcampus/client/view/assets/store/G013.jpg' WHERE goodsId = 'G013' AND (imageUrl IS NULL OR imageUrl = '');
UPDATE tblGoods SET imageUrl = 'Client/src/vcampus/client/view/assets/store/G014.jpg' WHERE goodsId = 'G014' AND (imageUrl IS NULL OR imageUrl = '');
UPDATE tblGoods SET imageUrl = 'Client/src/vcampus/client/view/assets/store/G015.jpg' WHERE goodsId = 'G015' AND (imageUrl IS NULL OR imageUrl = '');
UPDATE tblGoods SET imageUrl = 'Client/src/vcampus/client/view/assets/store/G016.jpg' WHERE goodsId = 'G016' AND (imageUrl IS NULL OR imageUrl = '');
UPDATE tblGoods SET imageUrl = 'Client/src/vcampus/client/view/assets/store/G017.jpg' WHERE goodsId = 'G017' AND (imageUrl IS NULL OR imageUrl = '');
UPDATE tblGoods SET imageUrl = 'Client/src/vcampus/client/view/assets/store/G018.jpg' WHERE goodsId = 'G018' AND (imageUrl IS NULL OR imageUrl = '');
UPDATE tblGoods SET imageUrl = 'Client/src/vcampus/client/view/assets/store/G019.jpg' WHERE goodsId = 'G019' AND (imageUrl IS NULL OR imageUrl = '');
UPDATE tblGoods SET imageUrl = 'Client/src/vcampus/client/view/assets/store/G020.jpg' WHERE goodsId = 'G020' AND (imageUrl IS NULL OR imageUrl = '');
UPDATE tblGoods SET imageUrl = 'Client/src/vcampus/client/view/assets/store/G021.jpg' WHERE goodsId = 'G021' AND (imageUrl IS NULL OR imageUrl = '');
UPDATE tblGoods SET imageUrl = 'Client/src/vcampus/client/view/assets/store/G022.jpg' WHERE goodsId = 'G022' AND (imageUrl IS NULL OR imageUrl = '');

-- 核对：按类别统计商品数量与库存
SELECT category AS '类别', COUNT(*) AS '商品数', SUM(stock) AS '总库存'
FROM tblGoods GROUP BY category ORDER BY category;

SELECT COUNT(*) AS '商品总数',
       SUM(CASE WHEN imageUrl IS NULL OR imageUrl = '' THEN 1 ELSE 0 END) AS '缺图商品数'
FROM tblGoods;
