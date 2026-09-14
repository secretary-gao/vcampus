-- ============================================================
-- 文献库（PDF）表
-- ============================================================

USE vCampus;

DROP TABLE IF EXISTS `tblPaper`;
CREATE TABLE `tblPaper` (
    `paperId` VARCHAR(20)  PRIMARY KEY COMMENT '文献编号（内部用，界面不显示）',
    `title`   VARCHAR(200) NOT NULL    COMMENT '标题',
    `author`  VARCHAR(100)             COMMENT '作者',
    `pdfName` VARCHAR(255) NOT NULL    COMMENT 'PDF文件名（存在服务器 Papers 目录）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文献库';
