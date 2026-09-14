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

INSERT IGNORE INTO tblTeachingClass
    (teachingClassId, courseId, classNumber, teacher, capacity, selectedCount, remark)
VALUES
    ('CSE1001-01', 'CSE1001', '01', '王老师', 60, 0, 'Course 1.0 demo'),
    ('CSE1002-01', 'CSE1002', '01', '李老师', 60, 0, 'Course 1.0 demo'),
    ('MATH1001-01', 'MATH1001', '01', '陈老师', 80, 0, 'Course 1.0 demo');

INSERT IGNORE INTO tblCourseSchedule
    (scheduleId, teachingClassId, courseId, weekStart, weekEnd, classroom,
     dayOfWeek, startPeriod, endPeriod, startTime, endTime)
VALUES
    ('DEMO-SCH-001', 'CSE1001-01', 'CSE1001', 1, 16, '教一-101', 1, 1, 2,
     '08:00:00', '09:35:00'),
    ('DEMO-SCH-002', 'CSE1002-01', 'CSE1002', 1, 16, '教二-202', 3, 3, 4,
     '09:50:00', '11:25:00'),
    ('DEMO-SCH-003', 'MATH1001-01', 'MATH1001', 1, 16, '教三-303', 5, 6, 7,
     '14:00:00', '15:35:00');

UPDATE tblCourseSchedule
SET startTime=CASE scheduleId
        WHEN 'DEMO-SCH-001' THEN '08:00:00'
        WHEN 'DEMO-SCH-002' THEN '09:50:00'
        WHEN 'DEMO-SCH-003' THEN '14:00:00' END,
    endTime=CASE scheduleId
        WHEN 'DEMO-SCH-001' THEN '09:35:00'
        WHEN 'DEMO-SCH-002' THEN '11:25:00'
        WHEN 'DEMO-SCH-003' THEN '15:35:00' END
WHERE scheduleId IN ('DEMO-SCH-001','DEMO-SCH-002','DEMO-SCH-003');
