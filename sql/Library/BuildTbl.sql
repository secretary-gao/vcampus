-- ======================================================
-- 图书馆模块：赵夏巍 负责
-- 只建自己模块的两张表
-- ======================================================

-- 图书信息表
DROP TABLE IF EXISTS `tblBook`;
CREATE TABLE `tblBook` (
    `bookId` VARCHAR(20) PRIMARY KEY COMMENT '索书号/图书编号',
    `bookName` VARCHAR(100) NOT NULL COMMENT '书名',
    `author` VARCHAR(50) DEFAULT NULL COMMENT '作者',
    `isbn` VARCHAR(20) DEFAULT NULL COMMENT 'ISBN号',
    `category` VARCHAR(30) DEFAULT NULL COMMENT '图书类别',
    `totalCount` INT DEFAULT 0 CHECK (`totalCount` >= 0) COMMENT '馆藏总数',
    `availableCount` INT DEFAULT 0 CHECK (`availableCount` >= 0) COMMENT '可借数量'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图书信息表';

-- 借阅记录表
DROP TABLE IF EXISTS `tblBorrow`;
CREATE TABLE `tblBorrow` (
    `recordId` VARCHAR(20) PRIMARY KEY COMMENT '借阅记录号',
    `userId` VARCHAR(10) NOT NULL COMMENT '借阅人ID（关联tblUser）',
    `bookId` VARCHAR(20) NOT NULL COMMENT '图书编号（关联tblBook）',
    `borrowDate` DATE NOT NULL COMMENT '借阅日期',
    `dueDate` DATE NOT NULL COMMENT '应还日期',
    `returnDate` DATE DEFAULT NULL COMMENT '归还日期（NULL表示未归还）',
    `status` ENUM('借阅中', '已归还', '已逾期') DEFAULT '借阅中' COMMENT '借阅状态'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图书借阅记录表';