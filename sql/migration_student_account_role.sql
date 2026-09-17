-- 修复学生学籍绑定账号的角色冲突（幂等）。
-- 旧演示数据曾把 12345678 配置为教师，但它同时绑定了学生学籍 22301002，
-- 导致学生无法用学籍账号登录。执行前请先使用 vcampus_schema.sql 建库。

USE vCampus;

UPDATE tblUser u
JOIN tblStudent s ON s.userId = u.uId
SET u.uRole = '学生',
    u.uPwd = MD5('123456'),
    u.uStatus = '正常',
    u.uName = s.name
WHERE u.uId = '12345678'
  AND s.studentId = '22301002';
