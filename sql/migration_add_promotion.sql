-- ============================================================
-- 迁移脚本：商店模块"每日特价/打折"功能 —— 新建促销表 tblPromotion
--
-- 需求：每天有不同的特价商品搞活动、打折。
-- 设计：促销活动按"商品 × 星期"配置，weekday 1=周一 … 7=周日，0=每天特价；
--       服务器在查询商品和结算时按"今天是星期几"取当日活动，
--       **折扣价一律由服务器计算**（客户端只负责显示，改了也没用）。
--       一个商品同一个星期几只允许一条活动（UNIQUE 约束）。
--
-- 幂等：可重复执行。
-- 用法：mysql -u root -p --default-character-set=utf8mb4 < sql/migration_add_promotion.sql
-- 演示数据：再执行一次 sql/shop/seed_promotion_demo.sql（每天都有特价商品）
-- ============================================================

USE vCampus;

CREATE TABLE IF NOT EXISTS tblPromotion (
    promoId      VARCHAR(20)  NOT NULL COMMENT '促销编号（PK）',
    goodsId      VARCHAR(20)  NOT NULL COMMENT '商品编号，外键->tblGoods.goodsId',
    discountRate DECIMAL(3,2) NOT NULL COMMENT '折扣率（0.10~0.95，如 0.80 表示 8 折）',
    weekday      TINYINT      NOT NULL DEFAULT 0 COMMENT '生效星期：1=周一…7=周日，0=每天',
    remark       VARCHAR(50)           COMMENT '活动说明（如"周三饮料日"）',
    PRIMARY KEY (promoId),
    UNIQUE KEY uk_tblPromotion_goods_weekday (goodsId, weekday),
    CONSTRAINT fk_promotion_goods FOREIGN KEY (goodsId) REFERENCES tblGoods(goodsId),
    CONSTRAINT chk_tblPromotion_rate    CHECK (discountRate > 0 AND discountRate < 1),
    CONSTRAINT chk_tblPromotion_weekday CHECK (weekday BETWEEN 0 AND 7)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品促销表（每日特价活动）';

-- 核对
SELECT COUNT(*) AS '促销活动数(tblPromotion)' FROM tblPromotion;
