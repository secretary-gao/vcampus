-- ============================================================
-- 迁移脚本：订单明细增加"下单时的价格快照"（用于展示每单优惠了多少）
--
-- 背景：购物车/每日特价上线后，订单明细只存了成交小计（totalPrice）。
--       由于商品单价可能被管理员改动、每日特价也每天不同，事后无法反推"当时省了多少"，
--       因此在下单时把**当时的原价与折扣率**一起落库（价格快照），
--       这样历史订单永远是"当时成交的样子"，改价、改活动都不会影响它。
--
-- 新增两列（可空，历史数据会按"成交单价 = 原价、无折扣"回填）：
--   originalPrice 下单时原价（单价快照）
--   discountRate  下单时折扣率（0.80 表示 8 折；无活动为 NULL）
--
-- 幂等：可重复执行。
-- 用法：mysql -u root -p vCampus < sql/migration_add_order_price_snapshot.sql
-- ============================================================

USE vCampus;

-- 1) 原价快照列
SET @has_original := (SELECT COUNT(*) FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = DATABASE()
                        AND TABLE_NAME = 'tblPurchase'
                        AND COLUMN_NAME = 'originalPrice');
SET @ddl1 := IF(@has_original = 0,
    'ALTER TABLE tblPurchase ADD COLUMN originalPrice DECIMAL(10,2) NULL COMMENT ''下单时原价快照（单价，>=0）'' AFTER totalPrice',
    'DO 0');
PREPARE stmt1 FROM @ddl1;
EXECUTE stmt1;
DEALLOCATE PREPARE stmt1;

-- 2) 折扣率快照列
SET @has_rate := (SELECT COUNT(*) FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = DATABASE()
                    AND TABLE_NAME = 'tblPurchase'
                    AND COLUMN_NAME = 'discountRate');
SET @ddl2 := IF(@has_rate = 0,
    'ALTER TABLE tblPurchase ADD COLUMN discountRate DECIMAL(3,2) NULL COMMENT ''下单时折扣率（0.10~0.95，无活动为 NULL）'' AFTER originalPrice',
    'DO 0');
PREPARE stmt2 FROM @ddl2;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;

-- 3) 回填历史数据：老订单没有原价快照，按"成交单价 = 原价、无折扣"处理
--    （没有快照就只能这样近似，回填后老订单显示为"无优惠"，不会凭空造出优惠金额）
UPDATE tblPurchase
SET originalPrice = ROUND(totalPrice / quantity, 2)
WHERE originalPrice IS NULL AND quantity > 0;

-- 4) 核对
SELECT COUNT(*) AS '明细行数',
       SUM(CASE WHEN originalPrice IS NULL THEN 1 ELSE 0 END) AS '缺原价快照的行',
       SUM(CASE WHEN discountRate IS NOT NULL THEN 1 ELSE 0 END) AS '带折扣的行'
FROM tblPurchase;
