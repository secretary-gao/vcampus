-- ============================================================
-- VCampus Course 模块演示数据（幂等，可重复执行）
-- 依赖顺序：vcampus_schema.sql -> Student/BuildTbl.sql ->
--           course/vcampus_course.sql -> seed_demo_data.sql -> 本文件
-- 演示密码统一为：123456
-- ============================================================

USE vCampus;

-- 教务管理员，以及共享演示学生账号 09010101 对应的正式学籍。
INSERT IGNORE INTO tblUser (uId, uName, uAge, uSex, uPwd, uRole) VALUES
('ADMIN001', '教务员', 35, '女', MD5('123456'), '管理员');

INSERT IGNORE INTO tblStudent
    (studentId, campusCardNo, userId, name, className, major, grade,
     enrollmentDate, status)
VALUES
    ('2026000001', 'CARD-DEMO-001', '09010101', '张三', '计科一班',
     '计算机科学与技术', '2026', '2026-09-01', '在读');

INSERT IGNORE INTO tblCourse
    (courseId, courseName, teacher, credit, capacity, selectedCount)
VALUES
    ('CSE1001', 'Java程序设计', '王老师', 3, 60, 0),
    ('CSE1002', '数据结构', '李老师', 4, 60, 0),
    ('MATH1001', '离散数学', '陈老师', 3, 80, 0);

INSERT IGNORE INTO tblCourseSchedule
    (scheduleId, courseId, classroom, dayOfWeek, startTime, endTime)
VALUES
    ('DEMO-SCH-001', 'CSE1001', '教一-101', 1, '08:00:00', '09:40:00'),
    ('DEMO-SCH-002', 'CSE1002', '教二-202', 3, '10:00:00', '11:40:00'),
    ('DEMO-SCH-003', 'MATH1001', '教三-303', 5, '14:00:00', '15:40:00');
