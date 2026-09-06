-- ============================================================
-- Vcampus 虚拟校园系统 - 数据库初始化脚本
-- 对应 standard/共享说明书.docx 用户管理模块 tblUser 表设计
-- 用法：mysql -u root -p < sql/vcampus_schema.sql
-- ============================================================

CREATE DATABASE IF NOT EXISTS vCampus
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE vCampus;

-- ------------------------------------------------------------
-- tblUser：用户信息表
-- 说明书原始设计：uId varchar(8) 定长(PK) / uName varchar(10) /
--   uAge int(0-100) / uSex ENUM(男|女) / uPwd varchar(8) 定长 /
--   uRole ENUM(学生|教师|管理员) 默认"学生"
-- 本项目密码采用 MD5 摘要存储（32 位十六进制），因此 uPwd 长度由
-- 说明书的 8 位明文调整为 CHAR(32)，其余字段与说明书保持一致。
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS tblUser (
    uId     CHAR(8)      NOT NULL COMMENT '登录ID，定长8位（如学号/工号）',
    uName   VARCHAR(10)           COMMENT '姓名',
    uAge    INT                   COMMENT '年龄',
    uSex    ENUM('男', '女')      COMMENT '性别',
    uPwd    CHAR(32)     NOT NULL COMMENT 'MD5密码摘要（32位十六进制）',
    uRole   ENUM('学生', '教师', '管理员') NOT NULL DEFAULT '学生' COMMENT '用户角色',
    uStatus ENUM('正常', '禁用') NOT NULL DEFAULT '正常' COMMENT '账号状态，管理员可禁用/启用（对应说明书"管理员可注销/禁用账号"）',
    PRIMARY KEY (uId),
    CONSTRAINT chk_tblUser_uAge CHECK (uAge IS NULL OR uAge BETWEEN 0 AND 100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户信息表';