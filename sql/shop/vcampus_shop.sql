-- ============================================================
-- Vcampus 虚拟校园系统 - 商店模块数据库脚本
-- 对应 standard/共享说明书.docx 商店模块 tblGoods / tblPurchase 表设计
-- 用法：mysql -u root -p --default-character-set=utf8mb4 < sql/shop/vcampus_shop.sql
-- 依赖：先执行过 sql/vcampus_schema.sql（已建 vCampus 库与 tblUser 表）；
--       购买/拿订单演示需要示例用户，请先执行 sql/seed_demo_data.sql
-- ============================================================

CREATE DATABASE IF NOT EXISTS vCampus
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE vCampus;

-- ------------------------------------------------------------
-- tblGoods：商品信息表
-- 说明书：goodsId varchar(20) 主键 / goodsName varchar(50) /
--   category varchar(30) / price decimal(8,2) >=0 / stock int >=0
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS tblGoods (
    goodsId   VARCHAR(20)   NOT NULL COMMENT '商品编号（PK）',
    goodsName VARCHAR(50)   NOT NULL COMMENT '商品名称',
    category  VARCHAR(30)           COMMENT '商品类别',
    price     DECIMAL(8,2)  NOT NULL DEFAULT 0 COMMENT '单价（>=0）',
    stock     INT           NOT NULL DEFAULT 0 COMMENT '库存数量（>=0）',
    imageUrl  VARCHAR(255)          COMMENT '商品图片地址（可空）',
    PRIMARY KEY (goodsId),
    CONSTRAINT chk_tblGoods_price CHECK (price >= 0),
    CONSTRAINT chk_tblGoods_stock CHECK (stock >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品信息表';

-- ------------------------------------------------------------
-- tblPurchase：商品购买记录表
-- 说明书：orderId varchar(20) 主键（本项目用 ORDER+时间戳+随机数生成，
--   长度可能超过20，故用 varchar(32)）/ userId varchar(10) 外键 tblUser /
--   goodsId varchar(20) 外键 tblGoods / quantity int >0 /
--   totalPrice decimal(10,2) >=0 / orderTime datetime
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS tblPurchase (
    orderId    VARCHAR(32)   NOT NULL COMMENT '订单号（PK）',
    userId     VARCHAR(10)   NOT NULL COMMENT '购买人ID，外键->tblUser.uId',
    goodsId    VARCHAR(20)   NOT NULL COMMENT '商品编号，外键->tblGoods.goodsId',
    quantity   INT           NOT NULL DEFAULT 1 COMMENT '购买数量（>0）',
    totalPrice DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '订单总价（>=0）',
    orderTime  DATETIME      NOT NULL COMMENT '下单时间',
    PRIMARY KEY (orderId),
    CONSTRAINT fk_purchase_user  FOREIGN KEY (userId)  REFERENCES tblUser(uId),
    CONSTRAINT fk_purchase_goods FOREIGN KEY (goodsId) REFERENCES tblGoods(goodsId),
    CONSTRAINT chk_tblPurchase_qty   CHECK (quantity > 0),
    CONSTRAINT chk_tblPurchase_total CHECK (totalPrice >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品购买记录表';

-- ------------------------------------------------------------
-- 示例数据（商品）
-- ------------------------------------------------------------
INSERT IGNORE INTO tblGoods (goodsId, goodsName, category, price, stock, imageUrl) VALUES
('G001', '农夫山泉', '饮料', 2.00, 100, 'Client/src/vcampus/client/view/assets/store/G001.png'),
('G002', '可口可乐', '饮料', 3.50, 80, 'Client/src/vcampus/client/view/assets/store/G002.png'),
('G003', '面包', '食品', 5.00, 50, 'Client/src/vcampus/client/view/assets/store/G003.png'),
('G004', '笔记本', '文具', 8.00, 200, 'Client/src/vcampus/client/view/assets/store/G004.png'),
('G005', '中性笔', '文具', 2.00, 300, 'Client/src/vcampus/client/view/assets/store/G005.png'),
('G006', 'U盘64G', '数码', 45.00, 30, 'Client/src/vcampus/client/view/assets/store/G006.png'),
('G007', '洗衣液', '生活用品', 15.00, 40, 'Client/src/vcampus/client/view/assets/store/G007.png');
