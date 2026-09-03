CREATE DATABASE IF NOT EXISTS vCampus
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE vCampus;

CREATE TABLE IF NOT EXISTS tblStudent (
    studentId      VARCHAR(10) NOT NULL COMMENT '学号',
    campusCardNo   VARCHAR(20) NOT NULL COMMENT '一卡通号',
    userId         CHAR(8)     NOT NULL COMMENT '关联的用户账号',
    name           VARCHAR(20) NOT NULL COMMENT '姓名',
    className      VARCHAR(40) NOT NULL COMMENT '班级',
    major          VARCHAR(50) NOT NULL COMMENT '专业',
    grade          CHAR(4)     NOT NULL COMMENT '入学年份',
    enrollmentDate DATE                 COMMENT '入学日期',
    status         ENUM('在读', '休学', '毕业', '退学')
                                NOT NULL DEFAULT '在读' COMMENT '学籍状态',
    version        BIGINT      NOT NULL DEFAULT 0 COMMENT '数据版本',
    updatedAt      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP
                                ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',

    PRIMARY KEY (studentId),
    UNIQUE KEY uk_tblStudent_campusCardNo (campusCardNo),
    UNIQUE KEY uk_tblStudent_userId (userId),
    INDEX idx_tblStudent_name (name),

    CONSTRAINT fk_tblStudent_user
        FOREIGN KEY (userId)
        REFERENCES tblUser (uId)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,

    CONSTRAINT chk_tblStudent_grade
        CHECK (grade REGEXP '^[0-9]{4}$'),

    CONSTRAINT chk_tblStudent_version
        CHECK (version >= 0)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COMMENT='学生学籍信息表';