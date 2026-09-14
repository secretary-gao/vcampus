-- ============================================================
-- VCampus Course Reality Track - normalized Course schema
-- Fresh database setup. Existing Course 1.0 databases must run
-- migration_course_teaching_class.sql instead of recreating tables.
-- ============================================================

CREATE DATABASE IF NOT EXISTS vCampus
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;
USE vCampus;

CREATE TABLE IF NOT EXISTS tblCourse (
    courseId      VARCHAR(20) NOT NULL COMMENT '课程号（PK）',
    courseName    VARCHAR(80) NOT NULL COMMENT '课程名称',
    credit        INT         NOT NULL COMMENT '学分（>0）',
    courseNature  VARCHAR(20) NULL COMMENT '课程性质；截图缺失时可为空',
    openingUnit   VARCHAR(80) NULL COMMENT '开课单位；截图缺失时可为空',
    teacher       VARCHAR(60) NULL COMMENT 'Course 1.0 兼容快照；权威值在 tblTeachingClass',
    capacity      INT         NULL COMMENT 'Course 1.0 兼容快照；权威值在 tblTeachingClass',
    selectedCount INT         NULL COMMENT 'Course 1.0 兼容快照；权威值在 tblTeachingClass',
    PRIMARY KEY (courseId),
    CONSTRAINT chk_tblCourse_credit CHECK (credit > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程本体';

CREATE TABLE IF NOT EXISTS tblCourseRequirementGroup (
    groupId VARCHAR(32) NOT NULL,
    groupName VARCHAR(80) NOT NULL,
    rule VARCHAR(20) NOT NULL,
    PRIMARY KEY (groupId),
    CONSTRAINT chk_tblCourseRequirementGroup_rule CHECK (rule IN ('CHOOSE_ONE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程等价与培养要求组';

CREATE TABLE IF NOT EXISTS tblCourseRequirementGroupMember (
    groupId VARCHAR(32) NOT NULL,
    courseId VARCHAR(20) NOT NULL,
    PRIMARY KEY (groupId, courseId),
    CONSTRAINT fk_tblCourseRequirementGroupMember_group FOREIGN KEY (groupId)
        REFERENCES tblCourseRequirementGroup(groupId) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_tblCourseRequirementGroupMember_course FOREIGN KEY (courseId)
        REFERENCES tblCourse(courseId) ON DELETE RESTRICT ON UPDATE CASCADE,
    INDEX idx_tblCourseRequirementGroupMember_course (courseId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程要求组成员';

CREATE TABLE IF NOT EXISTS tblTeachingClass (
    teachingClassId VARCHAR(32) NOT NULL COMMENT '教学班 ID（PK）',
    courseId        VARCHAR(20) NOT NULL COMMENT '所属课程',
    classNumber     VARCHAR(10) NOT NULL COMMENT '教学班号，例如 01',
    teacher         VARCHAR(60) NOT NULL COMMENT '授课教师；多人用顿号分隔',
    capacity        INT         NOT NULL COMMENT '容量（>0）',
    selectedCount   INT         NOT NULL DEFAULT 0 COMMENT '已选人数',
    teachingLanguage VARCHAR(20) NULL COMMENT '教学语言；资料缺失时为空',
    remark          VARCHAR(120) NULL COMMENT '实验班等说明；资料缺失时为空',
    PRIMARY KEY (teachingClassId),
    CONSTRAINT uk_tblTeachingClass_course_number UNIQUE (courseId, classNumber),
    CONSTRAINT uk_tblTeachingClass_id_course UNIQUE (teachingClassId, courseId),
    CONSTRAINT fk_tblTeachingClass_course FOREIGN KEY (courseId)
        REFERENCES tblCourse(courseId) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_tblTeachingClass_capacity CHECK (capacity >= 0),
    CONSTRAINT chk_tblTeachingClass_selected
        CHECK (selectedCount >= 0 AND selectedCount <= capacity),
    INDEX idx_tblTeachingClass_teacher (teacher),
    INDEX idx_tblTeachingClass_course (courseId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='可选的具体教学班';

CREATE TABLE IF NOT EXISTS tblSelectCourse (
    selectId       VARCHAR(20) NOT NULL COMMENT '选课记录号（PK）',
    studentId      VARCHAR(10) NOT NULL COMMENT '学号',
    teachingClassId VARCHAR(32) NOT NULL COMMENT '实际选择的教学班',
    courseId       VARCHAR(20) NOT NULL COMMENT '兼容/同课程互斥键，由 Service 与教学班保持一致',
    selectTime     DATETIME    NOT NULL COMMENT '选课时间',
    PRIMARY KEY (selectId),
    CONSTRAINT uk_tblSelectCourse_student_class UNIQUE (studentId, teachingClassId),
    CONSTRAINT uk_tblSelectCourse_student_course UNIQUE (studentId, courseId),
    CONSTRAINT fk_tblSelectCourse_class FOREIGN KEY (teachingClassId)
        REFERENCES tblTeachingClass(teachingClassId) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_tblSelectCourse_class_course FOREIGN KEY (teachingClassId, courseId)
        REFERENCES tblTeachingClass(teachingClassId, courseId)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_tblSelectCourse_course FOREIGN KEY (courseId)
        REFERENCES tblCourse(courseId) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_tblSelectCourse_student FOREIGN KEY (studentId)
        REFERENCES tblStudent(studentId) ON DELETE RESTRICT ON UPDATE CASCADE,
    INDEX idx_tblSelectCourse_class (teachingClassId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生选择具体教学班';

CREATE TABLE IF NOT EXISTS tblCourseSchedule (
    scheduleId      VARCHAR(20) NOT NULL COMMENT '排课记录号（PK）',
    teachingClassId VARCHAR(32) NOT NULL COMMENT '所属教学班',
    courseId        VARCHAR(20) NOT NULL COMMENT '兼容查询字段，由 Service 与教学班保持一致',
    weekStart       TINYINT     NOT NULL COMMENT '起始教学周',
    weekEnd         TINYINT     NOT NULL COMMENT '结束教学周',
    dayOfWeek       TINYINT     NOT NULL COMMENT '星期（1=星期一，7=星期日）',
    startPeriod     TINYINT     NOT NULL COMMENT '起始节次',
    endPeriod       TINYINT     NOT NULL COMMENT '结束节次',
    classroom       VARCHAR(60) NULL COMMENT '教室；真实资料缺失时为空',
    startTime       TIME        NOT NULL COMMENT '兼容现有课表的开始时间',
    endTime         TIME        NOT NULL COMMENT '兼容现有课表的结束时间',
    PRIMARY KEY (scheduleId),
    CONSTRAINT uk_tblCourseSchedule_class_slot
        UNIQUE (teachingClassId, weekStart, weekEnd, dayOfWeek, startPeriod, endPeriod),
    CONSTRAINT fk_tblCourseSchedule_class FOREIGN KEY (teachingClassId)
        REFERENCES tblTeachingClass(teachingClassId) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_tblCourseSchedule_class_course FOREIGN KEY (teachingClassId, courseId)
        REFERENCES tblTeachingClass(teachingClassId, courseId)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_tblCourseSchedule_course FOREIGN KEY (courseId)
        REFERENCES tblCourse(courseId) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_tblCourseSchedule_week
        CHECK (weekStart BETWEEN 1 AND 30 AND weekEnd BETWEEN weekStart AND 30),
    CONSTRAINT chk_tblCourseSchedule_day CHECK (dayOfWeek BETWEEN 1 AND 7),
    CONSTRAINT chk_tblCourseSchedule_period
        CHECK (startPeriod BETWEEN 1 AND 13 AND endPeriod BETWEEN startPeriod AND 13),
    CONSTRAINT chk_tblCourseSchedule_time CHECK (startTime < endTime),
    INDEX idx_tblCourseSchedule_room_period
        (dayOfWeek, classroom, weekStart, weekEnd, startPeriod, endPeriod),
    INDEX idx_tblCourseSchedule_class (teachingClassId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教学班的周次与节次安排';
