-- ============================================================
-- 迁移脚本：给已存在的 tblUser 表补上 uStatus（账号状态）字段
-- 用法：mysql -u root -p vCampus < sql/migration_add_user_status.sql
-- 适用场景：你之前已经执行过 sql/vcampus_schema.sql 建好了 tblUser，
--   现在只需要补这一个新字段，不用重新建库建表、不会丢已有数据。
-- 如果提示 "Duplicate column name 'uStatus'"，说明你已经执行过一次，
--   忽略即可，不影响使用。
-- ============================================================

USE vCampus;

ALTER TABLE tblUser
    ADD COLUMN uStatus ENUM('正常', '禁用') NOT NULL DEFAULT '正常'
        COMMENT '账号状态，管理员可禁用/启用' AFTER uRole;
