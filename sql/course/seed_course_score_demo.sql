USE vCampus;

-- Teacher account 09010103 (王老师) receives three visible teaching classes.
INSERT IGNORE INTO tblCourse(courseId,courseName,credit,courseNature,openingUnit,teacher,capacity,selectedCount) VALUES
('SCOREDEMO01','数据库原理实践',3,'必修','计算机科学与工程学院','王老师',60,0),
('SCOREDEMO02','机器学习应用',2,'任选','计算机科学与工程学院','王老师',60,0);

INSERT IGNORE INTO tblTeachingClass(teachingClassId,courseId,classNumber,teacher,capacity,selectedCount,teachingLanguage,remark) VALUES
('SCORE_DEMO_DB-01','SCOREDEMO01','01','王老师',60,0,'中文','成绩演示教学班'),
('SCORE_DEMO_DB-02','SCOREDEMO01','02','王老师',60,0,'中文','成绩演示教学班'),
('SCORE_DEMO_ML-01','SCOREDEMO02','01','王老师',60,0,'中文','成绩演示教学班');

INSERT IGNORE INTO tblSelectCourse(selectId,studentId,teachingClassId,courseId,selectTime)
SELECT CONCAT('SDB1',studentId),studentId,'SCORE_DEMO_DB-01','SCOREDEMO01',NOW()
FROM (SELECT studentId FROM tblStudent ORDER BY studentId LIMIT 20) demoStudents;
INSERT IGNORE INTO tblSelectCourse(selectId,studentId,teachingClassId,courseId,selectTime)
SELECT CONCAT('SDB2',studentId),studentId,'SCORE_DEMO_DB-02','SCOREDEMO01',NOW()
FROM (SELECT studentId FROM tblStudent ORDER BY studentId DESC LIMIT 20) demoStudents;
INSERT IGNORE INTO tblSelectCourse(selectId,studentId,teachingClassId,courseId,selectTime)
SELECT CONCAT('SDM1',studentId),studentId,'SCORE_DEMO_ML-01','SCOREDEMO02',NOW()
FROM (SELECT studentId FROM tblStudent ORDER BY studentId LIMIT 20) demoStudents;

UPDATE tblTeachingClass tc SET selectedCount=(SELECT COUNT(*) FROM tblSelectCourse sc WHERE sc.teachingClassId=tc.teachingClassId)
WHERE tc.teachingClassId IN ('SCORE_DEMO_DB-01','SCORE_DEMO_DB-02','SCORE_DEMO_ML-01');

-- 保留约四分之一学生为空白，答辩时可完整展示“未录入 -> 未提交 -> 待审核 -> 已发布”。
DELETE cs FROM tblCourseScore cs
JOIN tblSelectCourse sc ON sc.studentId=cs.studentId AND sc.teachingClassId=cs.teachingClassId
WHERE sc.teachingClassId IN ('SCORE_DEMO_DB-01','SCORE_DEMO_DB-02','SCORE_DEMO_ML-01')
  AND MOD(CRC32(CONCAT(sc.studentId,sc.teachingClassId)),4)=0;

INSERT INTO tblCourseScore(scoreId,studentId,courseId,teachingClassId,teacher,score,status,submittedAt,reviewedAt)
SELECT CONCAT('SS',SUBSTRING(MD5(CONCAT(sc.teachingClassId,sc.studentId)),1,18)),sc.studentId,sc.courseId,sc.teachingClassId,'王老师',70 + MOD(CRC32(sc.studentId),30),
       IF(MOD(CRC32(sc.studentId),3)=0,'PENDING','APPROVED'),NOW(),IF(MOD(CRC32(sc.studentId),3)=0,NULL,NOW())
FROM tblSelectCourse sc WHERE sc.teachingClassId IN ('SCORE_DEMO_DB-01','SCORE_DEMO_DB-02','SCORE_DEMO_ML-01')
  AND MOD(CRC32(CONCAT(sc.studentId,sc.teachingClassId)),4)<>0
ON DUPLICATE KEY UPDATE score=VALUES(score),teacher=VALUES(teacher),status=VALUES(status),submittedAt=VALUES(submittedAt),reviewedAt=VALUES(reviewedAt);
