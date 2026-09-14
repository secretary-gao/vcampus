-- ============================================================
-- 迁移脚本：新增商店模块的"校园卡钱包"表 tblWallet（余额 + 充值 + 购买扣款）
-- 用法：mysql -u root -p vCampus < sql/migration_add_wallet.sql
-- 适用场景：你之前已建过商店表，现在补一张钱包表即可，不影响已有数据。
-- 如果提示 "Table 'tblwallet' already exists"，说明已执行过，忽略即可。
-- ============================================================

USE vCampus;

CREATE TABLE IF NOT EXISTS tblWallet (
    userId  VARCHAR(10)   NOT NULL COMMENT '用户ID，外键->tblUser.uId',
    balance DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '余额（>=0）',
    PRIMARY KEY (userId),
    CONSTRAINT fk_wallet_user FOREIGN KEY (userId) REFERENCES tblUser(uId),
    CONSTRAINT chk_tblWallet_balance CHECK (balance >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='校园卡钱包表';

-- 给示例用户补初始余额（幂等）
INSERT IGNORE INTO tblWallet (userId, balance) VALUES
('09010101', 200.00),
('09010102', 150.00),
('09010103', 300.00),
('admin001', 500.00);

SELECT userId, balance FROM tblWallet ORDER BY userId;
