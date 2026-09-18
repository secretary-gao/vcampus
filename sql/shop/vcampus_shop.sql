-- ============================================================
-- Vcampus 虚拟校园系统 - 商店模块数据库脚本
-- 对应 standard/共享说明书.docx 商店模块 tblGoods / tblPurchase 表设计
-- 用法：mysql -u root -p --default-character-set=utf8mb4 < sql/shop/vcampus_shop.sql
-- 依赖：先执行过 sql/vcampus_schema.sql（已建 vCampus 库与 tblUser 表）；
--       购买/拿订单演示需要示例用户，请先执行 sql/seed_demo_data.sql
-- 说明：如果你之前已经建过商店表（tblPurchase 以 orderId 为主键），不要重跑本脚本，
--       请执行 sql/migration_add_order_group.sql 把表升级为"订单主表 + 订单明细"结构。
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
-- tblPromotion：商品促销表（每日特价/打折活动）
-- 需求："每天有不同的特价商品搞活动"。设计成"商品 × 星期"配置：
--   weekday 1=周一…7=周日，0=每天特价；服务器按今天是星期几取当日活动，
--   折扣价一律由服务器计算（客户端只显示，改了也没用）。
--   同一商品同一天只允许一条活动（UNIQUE 约束）。
-- 示例活动见 sql/shop/seed_promotion_demo.sql。
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS tblPromotion (
    promoId      VARCHAR(20)  NOT NULL COMMENT '促销编号（PK）',
    goodsId      VARCHAR(20)  NOT NULL COMMENT '商品编号，外键->tblGoods.goodsId',
    discountRate DECIMAL(3,2) NOT NULL COMMENT '折扣率（0.10~0.95，如 0.80 表示 8 折）',
    weekday      TINYINT      NOT NULL DEFAULT 0 COMMENT '生效星期：1=周一…7=周日，0=每天',
    remark       VARCHAR(50)           COMMENT '活动说明（如"周三文具日"）',
    PRIMARY KEY (promoId),
    UNIQUE KEY uk_tblPromotion_goods_weekday (goodsId, weekday),
    CONSTRAINT fk_promotion_goods FOREIGN KEY (goodsId) REFERENCES tblGoods(goodsId),
    CONSTRAINT chk_tblPromotion_rate    CHECK (discountRate > 0 AND discountRate < 1),
    CONSTRAINT chk_tblPromotion_weekday CHECK (weekday BETWEEN 0 AND 7)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品促销表（每日特价活动）';

-- ------------------------------------------------------------
-- tblOrder：订单主表（一次结算 = 一个订单，可含多个商品）
-- 说明书把"同一订单可包含多个商品"作为开放问题简化处理（一次购买仅针对单一商品）；
-- 本模块在购物车功能中按"一单多商品"落地，因此拆出订单主表：
--   orderId varchar(32) 主键 / userId varchar(10) 外键 tblUser /
--   totalAmount decimal(10,2) 订单总金额 / orderTime datetime 下单时间
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
-- tblPurchase：订单明细表（一个订单多行，每行一个商品）
-- 说明书：userId varchar(10) 外键 tblUser / goodsId varchar(20) 外键 tblGoods /
--   quantity int >0 / totalPrice decimal(10,2) >=0（本行小计 = 单价×数量）/
--   orderTime datetime；订单号 orderId 在说明书中是主键，购物车需要"一单多商品"，
--   故改为自增 itemId 主键 + orderId 外键（指向 tblOrder）并建索引。
--   UNIQUE(orderId, goodsId)：保证"一个订单里同一商品只有一行"（结算时按商品编号合并数量），
--   同时让种子脚本里的 INSERT IGNORE 保持幂等（改造前靠 orderId 主键去重，换成自增主键后
--   若不加这个唯一键，重复执行 sql/seed_demo_data.sql 会重复插入购买记录）。
--   originalPrice / discountRate：下单当时的价格快照，用于在订单明细里展示"优惠了多少"；
--   商品单价可能被管理员改、每日特价每天也不同，落快照才能保证历史订单不受事后变动影响。
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS tblPurchase (
    itemId     BIGINT        NOT NULL AUTO_INCREMENT COMMENT '明细行号（PK）',
    orderId    VARCHAR(32)   NOT NULL COMMENT '订单号，外键->tblOrder.orderId',
    userId     VARCHAR(10)   NOT NULL COMMENT '购买人ID，外键->tblUser.uId',
    goodsId    VARCHAR(20)   NOT NULL COMMENT '商品编号，外键->tblGoods.goodsId',
    quantity   INT           NOT NULL DEFAULT 1 COMMENT '购买数量（>0）',
    totalPrice DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '本行小计（成交单价×数量，>=0）',
    originalPrice DECIMAL(10,2) NULL COMMENT '下单时原价快照（单价；历史数据按成交价回填）',
    discountRate  DECIMAL(3,2)  NULL COMMENT '下单时折扣率快照（0.10~0.95，无活动为 NULL）',
    orderTime  DATETIME      NOT NULL COMMENT '下单时间',
    PRIMARY KEY (itemId),
    KEY idx_tblPurchase_order (orderId),
    KEY idx_tblPurchase_user_time (userId, orderTime),
    UNIQUE KEY uk_tblPurchase_order_goods (orderId, goodsId),
    CONSTRAINT fk_purchase_order FOREIGN KEY (orderId) REFERENCES tblOrder(orderId),
    CONSTRAINT fk_purchase_user  FOREIGN KEY (userId)  REFERENCES tblUser(uId),
    CONSTRAINT fk_purchase_goods FOREIGN KEY (goodsId) REFERENCES tblGoods(goodsId),
    CONSTRAINT chk_tblPurchase_qty   CHECK (quantity > 0),
    CONSTRAINT chk_tblPurchase_total CHECK (totalPrice >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单明细表（一个订单可含多个商品）';

-- ------------------------------------------------------------
-- tblWallet：校园卡钱包表（商店模块自建，用于"余额 + 充值 + 购买扣款"）
-- 每个用户一条余额记录；购买时校验并扣减余额，余额不足则下单失败。
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS tblWallet (
    userId  VARCHAR(10)   NOT NULL COMMENT '用户ID，外键->tblUser.uId',
    balance DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '余额（>=0）',
    PRIMARY KEY (userId),
    CONSTRAINT fk_wallet_user FOREIGN KEY (userId) REFERENCES tblUser(uId),
    CONSTRAINT chk_tblWallet_balance CHECK (balance >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='校园卡钱包表';

-- ------------------------------------------------------------
-- 示例数据（商品，22 件，覆盖 5 个类别）
-- 说明：G001~G007 带真实商品图；G008 以后 imageUrl 留空，界面用
--   "类别色块 + 类别图标"占位，管理员可在【商品管理】里补图片路径。
--   只想补商品、不想动表结构的话，也可以单独执行
--   sql/shop/seed_goods_demo.sql（同样幂等）。
-- ------------------------------------------------------------
INSERT IGNORE INTO tblGoods (goodsId, goodsName, category, price, stock, imageUrl) VALUES
    ('G001', '农夫山泉',         '饮料',     2.00, 100, 'Client/src/vcampus/client/view/assets/store/G001.jpg'),
    ('G002', '可口可乐',         '饮料',     3.50,  80, 'Client/src/vcampus/client/view/assets/store/G002.jpg'),
    ('G012', '蒙牛纯牛奶',       '饮料',     3.00,  90, 'Client/src/vcampus/client/view/assets/store/G012.jpg'),
    ('G013', '雀巢咖啡',         '饮料',     6.00,  40, 'Client/src/vcampus/client/view/assets/store/G013.jpg'),
    ('G014', '冰红茶',           '饮料',     3.00,  70, 'Client/src/vcampus/client/view/assets/store/G014.jpg'),
    ('G003', '面包',             '食品',     5.00,  50, 'Client/src/vcampus/client/view/assets/store/G003.jpg'),
    ('G008', '康师傅红烧牛肉面', '食品',     5.50,  60, 'Client/src/vcampus/client/view/assets/store/G008.jpg'),
    ('G009', '奥利奥饼干',       '食品',     9.90,  45, 'Client/src/vcampus/client/view/assets/store/G009.jpg'),
    ('G010', '乐事薯片',         '食品',     6.50,  55, 'Client/src/vcampus/client/view/assets/store/G010.jpg'),
    ('G011', '士力架',           '食品',     4.00,  80, 'Client/src/vcampus/client/view/assets/store/G011.jpg'),
    ('G004', '笔记本',           '文具',     8.00, 200, 'Client/src/vcampus/client/view/assets/store/G004.jpg'),
    ('G005', '中性笔',           '文具',     2.00, 300, 'Client/src/vcampus/client/view/assets/store/G005.jpg'),
    ('G015', '晨光橡皮',         '文具',     1.50, 150, 'Client/src/vcampus/client/view/assets/store/G015.jpg'),
    ('G016', '得力订书机',       '文具',    12.00,  25, 'Client/src/vcampus/client/view/assets/store/G016.jpg'),
    ('G017', '便利贴',           '文具',     4.50, 120, 'Client/src/vcampus/client/view/assets/store/G017.jpg'),
    ('G007', '洗衣液',           '生活用品', 15.00,  40, 'Client/src/vcampus/client/view/assets/store/G007.jpg'),
    ('G018', '抽纸',             '生活用品',  6.90,  65, 'Client/src/vcampus/client/view/assets/store/G018.jpg'),
    ('G019', '洗发水',           '生活用品', 29.90,  20, 'Client/src/vcampus/client/view/assets/store/G019.jpg'),
    ('G006', 'U盘64G',           '数码',    45.00,  30, 'Client/src/vcampus/client/view/assets/store/G006.jpg'),
    ('G020', '无线鼠标',         '数码',    39.00,  18, 'Client/src/vcampus/client/view/assets/store/G020.jpg'),
    ('G021', '数据线',           '数码',    15.00, 100, 'Client/src/vcampus/client/view/assets/store/G021.jpg'),
    ('G022', '充电宝',           '数码',    89.00,   5, 'Client/src/vcampus/client/view/assets/store/G022.jpg');

-- ------------------------------------------------------------
-- 示例余额（依赖 seed_demo_data.sql 里的示例用户）
-- ------------------------------------------------------------
INSERT IGNORE INTO tblWallet (userId, balance) VALUES
('09010101', 200.00),
('09010102', 150.00),
('09010103', 300.00),
('admin001', 500.00);
