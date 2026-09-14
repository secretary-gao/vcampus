-- ============================================================
-- 迁移脚本：商店模块购物车 —— 订单主表 tblOrder + 购买记录改造为订单明细
--
-- 背景：设计说明书"业务约束"里把"同一订单可包含多个商品"作为开放问题做了简化
--       （一次购买仅针对单一商品）。本迁移配合购物车按"一单多商品"落地：
--         tblOrder    订单主表：一次结算一行（订单号、下单人、订单总金额、下单时间）
--         tblPurchase 订单明细行：一单多行（原来的表，每行一个商品）
--       原来的 tblPurchase 以 orderId 为主键，天然只能一单一行，因此这里把主键
--       换成自增 itemId，orderId 降为外键 + 索引；历史数据会按 orderId 聚合回填
--       一条订单主表记录，数据不丢。
--
-- 幂等：可重复执行（已迁移过会跳过 ALTER）。
-- 用法：mysql -u root -p --default-character-set=utf8mb4 < sql/migration_add_order_group.sql
-- ============================================================

USE vCampus;

-- ------------------------------------------------------------
-- 1. 订单主表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS tblOrder (
    orderId     VARCHAR(32)   NOT NULL COMMENT '订单号（PK）',
    userId      VARCHAR(10)   NOT NULL COMMENT '下单人ID，外键->tblUser.uId',
    totalAmount DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '订单总金额（>=0）',
    orderTime   DATETIME      NOT NULL COMMENT '下单时间',
    PRIMARY KEY (orderId),
    KEY idx_tblOrder_user_time (userId, orderTime),
    CONSTRAINT fk_order_user FOREIGN KEY (userId) REFERENCES tblUser(uId),
    CONSTRAINT chk_tblOrder_total CHECK (totalAmount >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单主表（一次结算一个订单，可含多个商品）';

-- ------------------------------------------------------------
-- 2. 回填历史订单：把原来"一单一行"的购买记录聚合成订单主表记录
--    （已经存在的 orderId 由 INSERT IGNORE 跳过，可重复执行）
-- ------------------------------------------------------------
INSERT IGNORE INTO tblOrder (orderId, userId, totalAmount, orderTime)
SELECT p.orderId, p.userId, SUM(p.totalPrice), MIN(p.orderTime)
FROM tblPurchase p
GROUP BY p.orderId, p.userId;

-- ------------------------------------------------------------
-- 3. tblPurchase 改造为明细表：主键 orderId -> 自增 itemId，orderId 变索引 + 外键
--    （用 information_schema 判断，避免重复执行时报错）
-- ------------------------------------------------------------
SET @has_item := (SELECT COUNT(*) FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = DATABASE()
                    AND TABLE_NAME = 'tblPurchase'
                    AND COLUMN_NAME = 'itemId');

SET @ddl := IF(@has_item = 0,
    'ALTER TABLE tblPurchase DROP PRIMARY KEY, ADD COLUMN itemId BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST, ADD KEY idx_tblPurchase_order (orderId), ADD CONSTRAINT fk_purchase_order FOREIGN KEY (orderId) REFERENCES tblOrder(orderId)',
    'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 列注释同步（MODIFY 天然幂等）
ALTER TABLE tblPurchase
    MODIFY COLUMN orderId    VARCHAR(32)   NOT NULL COMMENT '订单号，外键->tblOrder.orderId',
    MODIFY COLUMN totalPrice DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '本行小计（单价×数量，>=0）';

-- ------------------------------------------------------------
-- 4. 结果核对
-- ------------------------------------------------------------
SELECT COUNT(*) AS '订单数(tblOrder)'   FROM tblOrder;
SELECT COUNT(*) AS '明细行数(tblPurchase)' FROM tblPurchase;
SELECT p.orderId, o.totalAmount AS '订单总额', COUNT(*) AS '明细行数'
FROM tblPurchase p JOIN tblOrder o ON p.orderId = o.orderId
GROUP BY p.orderId, o.totalAmount
ORDER BY p.orderId DESC
LIMIT 5;
