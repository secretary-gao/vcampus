-- ============================================================
-- 迁移脚本：给医生表 tblDoctor 补上"擅长领域"字段 skill
--
-- 背景：DoctorDAO / Doctor VO 这次合并已经改成读写 skill 字段，
--       但建表脚本和历史迁移里都没有加这一列，导致查询医生直接报
--       "Unknown column 'skill' in 'field list'"。
--
-- 幂等：用 information_schema 判断列是否已存在，可重复执行。
-- 用法：mysql -u root -p --default-character-set=utf8mb4 < sql/hospital/migration_add_doctor_skill.sql
-- ============================================================

USE vCampus;

SET @has_skill := (SELECT COUNT(*) FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE()
                     AND TABLE_NAME = 'tblDoctor'
                     AND COLUMN_NAME = 'skill');

SET @ddl := IF(@has_skill = 0,
    'ALTER TABLE tblDoctor ADD COLUMN skill VARCHAR(100) NULL COMMENT ''擅长领域'' AFTER title',
    'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 核对
DESCRIBE tblDoctor;
