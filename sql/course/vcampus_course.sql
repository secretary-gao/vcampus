-- ============================================================
-- Vcampus 虚拟校园系统 - 选课模块数据库脚本
-- 对应 standard/共享说明书.docx 选课模块 tblCourse / tblSelectCourse 表设计
-- 用法：mysql -u root -p --default-character-set=utf8mb4 < sql/course/vcampus_course.sql
-- ============================================================

CREATE DATABASE IF NOT EXISTS vCampus
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE vCampus;

-- ------------------------------------------------------------
-- tblCourse：课程信息表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS tblCourse (
    courseId      VARCHAR(20) NOT NULL COMMENT '课程号（PK）',
    courseName    VARCHAR(50) NOT NULL COMMENT '课程名称',
    teacher       VARCHAR(20) NOT NULL COMMENT '授课教师',
    credit        INT         NOT NULL COMMENT '学分（>0）',
    capacity      INT         NOT NULL COMMENT '课程容量（>0）',
    selectedCount INT         NOT NULL DEFAULT 0 COMMENT '已选人数（0..capacity）',
    PRIMARY KEY (courseId),
    CONSTRAINT chk_tblCourse_credit CHECK (credit > 0),
    CONSTRAINT chk_tblCourse_capacity CHECK (capacity > 0),
    CONSTRAINT chk_tblCourse_selectedCount
        CHECK (selectedCount >= 0 AND selectedCount <= capacity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程信息表';

-- ------------------------------------------------------------
-- tblSelectCourse：学生选课记录表
--
-- 依赖：先执行 sql/Student/BuildTbl.sql 创建正式 tblStudent。
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS tblSelectCourse (
    selectId   VARCHAR(20) NOT NULL COMMENT '选课记录号（PK）',
    studentId  VARCHAR(10) NOT NULL COMMENT '学号，外键->tblStudent.studentId',
    courseId   VARCHAR(20) NOT NULL COMMENT '课程号，外键->tblCourse.courseId',
    selectTime DATETIME    NOT NULL COMMENT '选课时间',
    PRIMARY KEY (selectId),
    CONSTRAINT uk_tblSelectCourse_student_course UNIQUE (studentId, courseId),
    CONSTRAINT fk_tblSelectCourse_course FOREIGN KEY (courseId)
        REFERENCES tblCourse(courseId)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    CONSTRAINT fk_tblSelectCourse_student FOREIGN KEY (studentId)
        REFERENCES tblStudent(studentId)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生选课记录表';

-- ------------------------------------------------------------
-- tblCourseSchedule：课程排课表
-- 教师信息以 tblCourse.teacher 为唯一来源，不在本表重复保存。
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS tblCourseSchedule (
    scheduleId VARCHAR(20) NOT NULL COMMENT '排课记录号（PK）',
    courseId   VARCHAR(20) NOT NULL COMMENT '课程号，外键->tblCourse.courseId',
    classroom  VARCHAR(30) NOT NULL COMMENT '教室',
    dayOfWeek  TINYINT     NOT NULL COMMENT '星期（1=星期一，7=星期日）',
    startTime  TIME        NOT NULL COMMENT '开始时间',
    endTime    TIME        NOT NULL COMMENT '结束时间',
    PRIMARY KEY (scheduleId),
    CONSTRAINT uk_tblCourseSchedule_course_slot
        UNIQUE (courseId, dayOfWeek, startTime, endTime),
    CONSTRAINT fk_tblCourseSchedule_course FOREIGN KEY (courseId)
        REFERENCES tblCourse(courseId)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    CONSTRAINT chk_tblCourseSchedule_day CHECK (dayOfWeek BETWEEN 1 AND 7),
    CONSTRAINT chk_tblCourseSchedule_time CHECK (startTime < endTime),
    INDEX idx_tblCourseSchedule_room_time (dayOfWeek, classroom, startTime, endTime),
    INDEX idx_tblCourseSchedule_course_time (courseId, dayOfWeek, startTime, endTime)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程排课表';
