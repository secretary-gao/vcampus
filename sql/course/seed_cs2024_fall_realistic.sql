-- ============================================================
-- 2026-2027 秋季：2024 级计算机科学与技术专业 Reality Track seed
-- 课程、教学班和节次来自需求附件中可见的学校截图。
-- 截图未显示的第 28 门课程不录入；缺失教室写“待补充”。
-- 钟点时间是为兼容 Course 1.0 UI 的 demo 映射，节次字段才是原始事实。
-- 模拟学生均为虚构数据，与真实个人无关。脚本可重复执行。
-- ============================================================
USE vCampus;

-- Demo teacher login for validating the teacher view. The account is synthetic;
-- the display name matches the teacher name visible in the supplied course screenshot.
INSERT IGNORE INTO tblUser (uId,uName,uAge,uSex,uPwd,uRole,uStatus)
VALUES ('TCH00001','陈龙',35,'男',MD5('123456'),'教师','正常');

INSERT IGNORE INTO tblCourse
    (courseId, courseName, credit, courseNature, openingUnit,
     teacher, capacity, selectedCount) VALUES
('B0203750','智能汽车与自动驾驶（全英文）（研讨）',2,'任选','机械工程学院','庄伟超',30,0),
('B0493021','通信电子线路基础（研讨）',2,'任选','信息科学与工程学院','田玲',40,0),
('B09A1111','机器学习(研讨)',2,'任选','计算机科学与工程学院','戴大荣',50,0),
('B09A1131','模式识别(全英文、研讨)',2,'任选','计算机科学与工程学院','耿新',50,0),
('B09D0012','数据库原理',3,'必修','计算机科学与工程学院','倪婕伟',120,0),
('B09D0013','数据库原理(全英文)',3,'必修','计算机科学与工程学院','王帅',60,0),
('B09D1021','大数据处理(研讨)',2,'任选','计算机科学与工程学院','王爽',50,0),
('B09G0011','数字图像处理',3,'必修','计算机科学与工程学院','陈阳',129,0),
('B09G1031','计算机图形学(研讨)',2,'任选','计算机科学与工程学院','杨武',100,0),
('B09G1061','语音信息处理(研讨)',2,'任选','计算机科学与工程学院','周琳',50,0),
('B09H1040','操作系统专题实践',1,'任选','计算机科学与工程学院','张柏礼',200,0),
('B09H1050','计算机系统结构',2,'任选','计算机科学与工程学院','李博睿',50,0),
('B09H1060','Linux内核技术',2,'任选','计算机科学与工程学院','沈典',50,0),
('B09N0014','计算机网络',3,'必修','计算机科学与工程学院','李伟',160,0),
('B09N0015','计算机网络(全英文)',3,'必修','计算机科学与工程学院','童虎',80,0),
('B09N1031','分布计算新技术(研讨)',2,'任选','计算机科学与工程学院','陈龙',50,0),
('B09T1060','数据结构与算法专题实践',1,'任选','计算机科学与工程学院','吕建华',100,0),
('B1604101','能源互联网信息技术（研讨）',2,'任选','电气工程学院','王琦',40,0),
('B58A1032','博弈与决策系统',2,'任选','人工智能学院','王万元、吴巍炜',50,0),
('B58A1042','深度学习',2,'任选','人工智能学院','伍家松',60,0),
('B58A1061','强化学习',2,'任选','人工智能学院','杨绍富',50,0),
('B71S0032','编译原理',4,'必修','软件学院','廉晓芳',120,0),
('B71S0033','编译原理(全英文)',4,'必修','软件学院','张志政',80,0),
('B71S1041','Java设计模式(研讨)',2,'任选','软件学院','朱海林',50,0),
('B71S1061','软件项目管理与实践',2,'任选','软件学院','汤薇',50,0),
('B71S1131','二进制代码分析(研讨)',2,'任选','软件学院','凌振',50,0),
('BJSL0120','批判性思维',1,'必修','计算机类','管艳明',150,0);

INSERT IGNORE INTO tblTeachingClass
    (teachingClassId,courseId,classNumber,teacher,capacity,selectedCount,
     teachingLanguage,remark) VALUES
('B0203750-01','B0203750','01','庄伟超',0,0,'全英文','截图容量为 0，当前不可选'),
('B0203750-02','B0203750','02','魏文鹏',30,0,'全英文','研讨'),
('B0493021-01','B0493021','01','田玲',40,0,NULL,'研讨'),
('B09A1111-01','B09A1111','01','戴大荣',50,0,NULL,'研讨'),
('B09A1131-01','B09A1131','01','耿新',50,0,'全英文','研讨'),
('B09D0012-02','B09D0012','02','倪婕伟',60,0,NULL,NULL),
('B09D0012-03','B09D0012','03','熊志宏',60,0,NULL,NULL),
('B09D0013-01','B09D0013','01','王帅',60,0,'全英文',NULL),
('B09D1021-01','B09D1021','01','王爽',50,0,NULL,'研讨'),
('B09G0011-01','B09G0011','01','陈阳',43,0,NULL,NULL),
('B09G0011-02','B09G0011','02','薛澄',43,0,NULL,NULL),
('B09G0011-03','B09G0011','03','张道坤',43,0,NULL,'包含实验班'),
('B09G1031-02','B09G1031','02','杨武',50,0,NULL,'研讨'),
('B09G1031-03','B09G1031','03','唐慧',50,0,NULL,'研讨'),
('B09G1061-01','B09G1061','01','周琳',50,0,NULL,'研讨'),
('B09H1040-01','B09H1040','01','张柏礼',50,0,NULL,NULL),
('B09H1040-02','B09H1040','02','张竞慧',50,0,NULL,NULL),
('B09H1040-03','B09H1040','03','董恺',50,0,NULL,NULL),
('B09H1040-04','B09H1040','04','陈龙',50,0,NULL,NULL),
('B09H1050-01','B09H1050','01','李博睿',50,0,NULL,NULL),
('B09H1060-01','B09H1060','01','沈典',50,0,NULL,NULL),
('B09N0014-02','B09N0014','02','李伟',40,0,NULL,'包含实验班'),
('B09N0014-04','B09N0014','04','顾晓丹',40,0,NULL,'包含实验班'),
('B09N0014-06','B09N0014','06','刘波',80,0,NULL,'包含实验班'),
('B09N0015-01','B09N0015','01','童虎',80,0,'全英文','包含实验班'),
('B09N1031-01','B09N1031','01','陈龙',50,0,NULL,'研讨'),
('B09T1060-01','B09T1060','01','吕建华',50,0,NULL,NULL),
('B09T1060-02','B09T1060','02','CHAU VINCENT WING-HO',50,0,NULL,NULL),
('B1604101-02','B1604101','02','王琦',40,0,NULL,'研讨'),
('B58A1032-01','B58A1032','01','王万元、吴巍炜',50,0,NULL,NULL),
('B58A1042-01','B58A1042','01','伍家松',60,0,NULL,NULL),
('B58A1061-01','B58A1061','01','杨绍富',50,0,NULL,NULL),
('B71S0032-01','B71S0032','01','廉晓芳',120,0,NULL,NULL),
('B71S0033-01','B71S0033','01','张志政',80,0,'全英文',NULL),
('B71S1041-01','B71S1041','01','朱海林',50,0,NULL,'研讨'),
('B71S1061-01','B71S1061','01','汤薇',50,0,NULL,NULL),
('B71S1131-01','B71S1131','01','凌振',50,0,NULL,'研讨'),
('BJSL0120-01','BJSL0120','01','管艳明',50,0,NULL,NULL),
('BJSL0120-04','BJSL0120','04','管艳明',50,0,NULL,NULL),
('BJSL0120-05','BJSL0120','05','梁宗保',50,0,NULL,NULL);

-- startTime/endTime are demo clock mappings for the authoritative period range.
INSERT IGNORE INTO tblCourseSchedule
    (scheduleId,teachingClassId,courseId,weekStart,weekEnd,dayOfWeek,
     startPeriod,endPeriod,classroom,startTime,endTime) VALUES
('R-S-001','B0203750-01','B0203750',1,16,6,11,12,'待补充','19:00','20:35'),
('R-S-002','B0203750-02','B0203750',1,16,1,6,7,'教二-303','14:00','15:35'),
('R-S-003','B0493021-01','B0493021',1,16,4,3,4,'教七-205','10:00','11:35'),
('R-S-004','B09A1111-01','B09A1111',1,8,5,3,5,'教二-40','10:00','12:25'),
('R-S-005','B09A1111-01','B09A1111',9,16,2,3,4,'教三-403','10:00','11:35'),
('R-S-006','B09A1131-01','B09A1131',3,6,2,8,10,'教七-21','16:00','18:25'),
('R-S-007','B09A1131-01','B09A1131',3,6,4,8,10,'教七-21','16:00','18:25'),
('R-S-008','B09A1131-01','B09A1131',3,6,5,6,9,'教七-210','14:00','17:35'),
('R-S-009','B09D0012-02','B09D0012',1,14,3,3,5,'教三-403','10:00','12:25'),
('R-S-010','B09D0012-03','B09D0012',1,14,3,3,5,'教三-404','10:00','12:25'),
('R-S-011','B09D0013-01','B09D0013',1,14,3,3,5,'教三-502','10:00','12:25'),
('R-S-012','B09D1021-01','B09D1021',1,10,1,3,4,'教三-30','10:00','11:35'),
('R-S-013','B09D1021-01','B09D1021',1,10,4,8,9,'教三-304','16:00','17:35'),
('R-S-014','B09G0011-01','B09G0011',1,12,4,3,5,'教四-20','10:00','12:25'),
('R-S-015','B09G0011-02','B09G0011',1,12,4,3,5,'教四-40','10:00','12:25'),
('R-S-016','B09G0011-03','B09G0011',1,12,4,3,5,'教四-30','10:00','12:25'),
('R-S-017','B09G0011-03','B09G0011',13,14,4,3,4,'教二-402','10:00','11:35'),
('R-S-018','B09G1031-02','B09G1031',1,13,2,3,5,'教四-30','10:00','12:25'),
('R-S-019','B09G1031-02','B09G1031',14,14,2,3,3,'教四-301','10:00','10:45'),
('R-S-020','B09G1031-03','B09G1031',1,13,1,6,8,'教四-10','14:00','16:45'),
('R-S-021','B09G1031-03','B09G1031',14,14,1,6,6,'教四-102','14:00','14:45'),
('R-S-022','B09G1061-01','B09G1061',1,8,5,6,8,'教四-10','14:00','16:45'),
('R-S-023','B09G1061-01','B09G1061',9,16,5,6,7,'教四-101','14:00','15:35'),
('R-S-024','B09H1040-01','B09H1040',9,16,1,9,12,'待补充','16:50','20:35'),
('R-S-025','B09H1040-02','B09H1040',9,16,2,9,12,'待补充','16:50','20:35'),
('R-S-026','B09H1040-03','B09H1040',9,16,4,9,12,'待补充','16:50','20:35'),
('R-S-027','B09H1040-04','B09H1040',9,16,5,9,12,'待补充','16:50','20:35'),
('R-S-028','B09H1050-01','B09H1050',1,16,1,6,7,'教二-305','14:00','15:35'),
('R-S-029','B09H1060-01','B09H1060',1,8,2,8,10,'教四-20','16:00','18:25'),
('R-S-030','B09H1060-01','B09H1060',9,16,1,8,9,'教四-201','16:00','17:35'),
('R-S-031','B09N0014-02','B09N0014',1,12,3,3,5,'教二-30','10:00','12:25'),
('R-S-032','B09N0014-02','B09N0014',13,14,3,3,4,'教二-302','10:00','11:35'),
('R-S-033','B09N0014-04','B09N0014',1,12,3,3,5,'教二-40','10:00','12:25'),
('R-S-034','B09N0014-04','B09N0014',13,14,3,3,4,'教二-401','10:00','11:35'),
('R-S-035','B09N0014-06','B09N0014',1,12,3,3,5,'教四-10','10:00','12:25'),
('R-S-036','B09N0014-06','B09N0014',13,14,3,3,4,'教四-102','10:00','11:35'),
('R-S-037','B09N0015-01','B09N0015',1,12,3,3,5,'教四-40','10:00','12:25'),
('R-S-038','B09N0015-01','B09N0015',13,14,3,3,4,'教四-403','10:00','11:35'),
('R-S-039','B09N1031-01','B09N1031',1,16,1,6,8,'教三-304','14:00','16:45'),
('R-S-040','B09T1060-01','B09T1060',1,8,4,9,12,'教四-304','16:50','20:35'),
('R-S-041','B09T1060-02','B09T1060',1,8,2,9,12,'教四-203','16:50','20:35'),
('R-S-042','B1604101-02','B1604101',1,16,2,6,7,'教二-507','14:00','15:35'),
('R-S-043','B58A1032-01','B58A1032',1,8,5,6,7,'教三-30','14:00','15:35'),
('R-S-044','B58A1032-01','B58A1032',9,16,5,6,8,'教三-302','14:00','16:45'),
('R-S-045','B58A1042-01','B58A1042',1,12,1,6,8,'教三-303','14:00','16:45'),
('R-S-046','B58A1061-01','B58A1061',1,16,5,6,7,'教三-304','14:00','15:35'),
('R-S-047','B71S0032-01','B71S0032',1,8,4,8,9,'教四-20','16:00','17:35'),
('R-S-048','B71S0032-01','B71S0032',1,8,4,3,5,'教四-20','10:00','12:25'),
('R-S-049','B71S0032-01','B71S0032',6,6,3,11,13,'教四-20（截图后缀待补）','19:00','21:25'),
('R-S-050','B71S0032-01','B71S0032',11,11,3,11,13,'教四-20（截图后缀待补）','19:00','21:25'),
('R-S-051','B71S0033-01','B71S0033',1,14,4,3,4,'教三-50','10:00','11:35'),
('R-S-052','B71S0033-01','B71S0033',1,14,5,3,5,'教三-50','10:00','12:25'),
('R-S-053','B71S0033-01','B71S0033',15,15,2,3,4,'教三-501','10:00','11:35'),
('R-S-054','B71S1041-01','B71S1041',1,12,4,8,10,'教二-20','16:00','18:25'),
('R-S-055','B71S1041-01','B71S1041',13,14,4,8,9,'教二-207','16:00','17:35'),
('R-S-056','B71S1061-01','B71S1061',1,12,2,8,9,'教二-30','16:00','17:35'),
('R-S-057','B71S1061-01','B71S1061',13,14,2,8,9,'教二-305','16:00','17:35'),
('R-S-058','B71S1131-01','B71S1131',1,12,1,6,8,'教二-20','14:00','16:45'),
('R-S-059','B71S1131-01','B71S1131',13,14,1,6,7,'教二-207','14:00','15:35'),
('R-S-060','BJSL0120-01','BJSL0120',1,8,5,1,2,'教一-102','08:00','09:35'),
('R-S-061','BJSL0120-04','BJSL0120',1,8,5,6,7,'教一-202','14:00','15:35'),
('R-S-062','BJSL0120-05','BJSL0120',1,8,1,6,7,'教一-408','14:00','15:35');

-- 150 synthetic users and students: C2400001..C2400150 / 2024000001..2024000150.
INSERT IGNORE INTO tblUser (uId,uName,uAge,uSex,uPwd,uRole,uStatus)
WITH RECURSIVE seq(n) AS (SELECT 1 UNION ALL SELECT n+1 FROM seq WHERE n<150)
SELECT CONCAT('C24',LPAD(n,5,'0')), CONCAT('模拟学生',LPAD(n,3,'0')),
       20, IF(MOD(n,2)=0,'女','男'), MD5('123456'), '学生', '正常'
FROM seq;

INSERT IGNORE INTO tblStudent
    (studentId,campusCardNo,userId,name,className,major,grade,enrollmentDate,status)
WITH RECURSIVE seq(n) AS (SELECT 1 UNION ALL SELECT n+1 FROM seq WHERE n<150)
SELECT CONCAT('2024',LPAD(n,6,'0')), CONCAT('VC24',LPAD(n,6,'0')),
       CONCAT('C24',LPAD(n,5,'0')), CONCAT('模拟学生',LPAD(n,3,'0')),
       CONCAT('计科240',MOD(n-1,5)+1), '计算机科学与技术', '2024',
       '2024-09-01', '在读'
FROM seq;

-- Rebuild only the synthetic students' selections so this seed remains idempotent
-- after its enrollment strategy changes. Normal demo and business data are untouched.
DELETE sc FROM tblSelectCourse sc
JOIN tblStudent s ON s.studentId=sc.studentId
JOIN tblUser u ON u.uId=s.userId
WHERE s.studentId REGEXP '^2024[0-9]{6}$'
  AND u.uId REGEXP '^C24[0-9]{5}$'
  AND s.major='计算机科学与技术'
  AND s.grade='2024';

-- Seven selections per synthetic student. The 15 templates were validated against
-- every Schedule row with inclusive week/period overlap semantics. Cycling through
-- them preserves varied hot/cold demand while preventing timetable conflicts.
INSERT INTO tblSelectCourse
    (selectId,studentId,teachingClassId,courseId,selectTime)
WITH RECURSIVE seq(n) AS (
    SELECT 1 UNION ALL SELECT n+1 FROM seq WHERE n<150
), templates(templateId,class1,class2,class3,class4,class5,class6,class7) AS (
    SELECT 1,'BJSL0120-01','B09N0014-06','B71S0032-01','B09N1031-01','B58A1061-01','B09H1040-04','B09G1031-02' UNION ALL
    SELECT 2,'BJSL0120-01','B09D0012-03','B09G0011-02','B09H1060-01','B71S1041-01','B09H1050-01','B09G1061-01' UNION ALL
    SELECT 3,'BJSL0120-05','B09N0014-02','B09G0011-03','B09T1060-02','B09A1111-01','B58A1032-01','B09H1040-03' UNION ALL
    SELECT 4,'BJSL0120-05','B09D0012-02','B09G0011-01','B09D1021-01','B1604101-02','B71S1061-01','B09H1040-01' UNION ALL
    SELECT 5,'BJSL0120-04','B09N0015-01','B71S0033-01','B09H1040-02','B58A1042-01','B09T1060-01','B09G1031-02' UNION ALL
    SELECT 6,'BJSL0120-04','B09N0014-04','B71S0032-01','B0203750-02','B09H1060-01','B09H1040-02','B09A1111-01' UNION ALL
    SELECT 7,'BJSL0120-04','B09D0013-01','B71S0033-01','B09G1031-03','B09H1040-04','B09T1060-02','B71S1041-01' UNION ALL
    SELECT 8,'BJSL0120-05','B09N0015-01','B71S0032-01','B58A1061-01','B09H1040-01','B71S1061-01','B1604101-02' UNION ALL
    SELECT 9,'BJSL0120-04','B09N0014-06','B09G0011-03','B71S1131-01','B09T1060-01','B09H1040-03','B71S1061-01' UNION ALL
    SELECT 10,'BJSL0120-05','B09D0012-03','B09G0011-01','B09A1131-01','B09H1040-03','B09G1031-02','B1604101-02' UNION ALL
    SELECT 11,'BJSL0120-01','B09D0012-02','B71S0032-01','B58A1042-01','B58A1032-01','B09A1111-01','B09H1040-02' UNION ALL
    SELECT 12,'BJSL0120-01','B09D0013-01','B09G0011-02','B09D1021-01','B09N1031-01','B09G1061-01','B09T1060-02' UNION ALL
    SELECT 13,'BJSL0120-04','B09N0014-04','B71S0033-01','B09H1050-01','B09H1040-04','B09T1060-01','B09H1060-01' UNION ALL
    SELECT 14,'BJSL0120-05','B09N0015-01','B71S0032-01','B09G1061-01','B09H1040-01','B71S1061-01','B09A1111-01' UNION ALL
    SELECT 15,'BJSL0120-01','B09N0014-02','B71S0033-01','B09G1031-03','B09A1131-01','B09H1040-01','B1604101-02'
), templateSlots AS (
    SELECT templateId,1 slot,class1 teachingClassId FROM templates UNION ALL
    SELECT templateId,2,class2 FROM templates UNION ALL
    SELECT templateId,3,class3 FROM templates UNION ALL
    SELECT templateId,4,class4 FROM templates UNION ALL
    SELECT templateId,5,class5 FROM templates UNION ALL
    SELECT templateId,6,class6 FROM templates UNION ALL
    SELECT templateId,7,class7 FROM templates
), chosen AS (
    SELECT seq.n,ts.slot,tc.courseId,tc.teachingClassId
    FROM seq
    JOIN templateSlots ts ON ts.templateId=MOD(seq.n-1,15)+1
    JOIN tblTeachingClass tc ON tc.teachingClassId=ts.teachingClassId
), numbered AS (
    SELECT *, ROW_NUMBER() OVER (ORDER BY n,slot) sequenceNumber FROM chosen
)
SELECT CONCAT('REAL',LPAD(sequenceNumber,16,'0')),
       CONCAT('2024',LPAD(n,6,'0')),teachingClassId,courseId,
       TIMESTAMP('2026-09-01 08:00:00') + INTERVAL sequenceNumber SECOND
FROM numbered;

UPDATE tblTeachingClass tc
SET selectedCount=(SELECT COUNT(*) FROM tblSelectCourse sc
                   WHERE sc.teachingClassId=tc.teachingClassId);

UPDATE tblCourse c
SET teacher=COALESCE((SELECT tc.teacher FROM tblTeachingClass tc
                      WHERE tc.courseId=c.courseId ORDER BY tc.classNumber LIMIT 1),teacher),
    capacity=COALESCE((SELECT SUM(tc.capacity) FROM tblTeachingClass tc
                       WHERE tc.courseId=c.courseId),capacity),
    selectedCount=COALESCE((SELECT SUM(tc.selectedCount) FROM tblTeachingClass tc
                            WHERE tc.courseId=c.courseId),selectedCount);
