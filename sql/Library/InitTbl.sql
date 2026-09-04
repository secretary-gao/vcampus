-- 插入测试图书
INSERT INTO `tblBook` (`bookId`, `bookName`, `author`, `isbn`, `category`, `totalCount`, `availableCount`) VALUES
('B001', 'Java核心技术 卷I', 'Cay S. Horstmann', '978-7-111-12345-6', '编程语言', 5, 3),
('B002', '深入理解Java虚拟机', '周志明', '978-7-111-23456-7', '编程语言', 3, 2),
('B003', '数据结构与算法分析', 'Mark Allen Weiss', '978-7-111-34567-8', '算法', 4, 4),
('B004', '计算机网络', 'James F. Kurose', '978-7-111-45678-9', '网络', 2, 1),
('B005', '数据库系统概念', 'Abraham Silberschatz', '978-7-111-56789-0', '数据库', 3, 0);

-- 插入测试借阅记录（userId 要和组长 tblUser 里的 ID 匹配）
INSERT INTO `tblBorrow` (`recordId`, `userId`, `bookId`, `borrowDate`, `dueDate`, `returnDate`, `status`) VALUES
('R001', 'zhangsan', 'B001', '2026-08-20', '2026-09-03', NULL, '借阅中'),
('R002', 'lisi', 'B002', '2026-08-15', '2026-08-29', '2026-08-27', '已归还'),
('R003', 'wangwu', 'B004', '2026-07-25', '2026-08-08', NULL, '已逾期'),
('R004', 'zhangsan', 'B005', '2026-08-22', '2026-09-05', NULL, '借阅中');