-- ============================================================
-- 迁移脚本：给已存在的 tblUser.uStatus 枚举补上"待审核"这个取值
-- 用法：mysql -u root -p vCampus < sql/migration_add_pending_status.sql
-- 适用场景：之前已经执行过 sql/migration_add_user_status.sql（有了
--   uStatus 字段，但只有 正常/禁用 两个取值），现在要支持"新注册账号
--   需要管理员审核"这个功能，需要给枚举再加一个"待审核"取值。
-- 本脚本只改列的类型定义（加一个新的合法取值），不改已有数据。
-- ============================================================

USE vCampus;

ALTER TABLE tblUser
    MODIFY COLUMN uStatus ENUM('正常', '禁用', '待审核') NOT NULL DEFAULT '正常'
        COMMENT '账号状态：正常/管理员禁用/注册后待审核';
