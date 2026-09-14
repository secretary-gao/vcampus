-- Course 1.0 -> Reality Track Phase 1 migration (MySQL 8.0).
-- Existing rows are preserved and mapped to teaching class 01.
USE vCampus;

DROP PROCEDURE IF EXISTS course_phase1_migrate;
DELIMITER $$
CREATE PROCEDURE course_phase1_migrate()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = DATABASE() AND table_name = 'tblCourse'
                     AND column_name = 'courseNature') THEN
        ALTER TABLE tblCourse ADD COLUMN courseNature VARCHAR(20) NULL AFTER credit;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = DATABASE() AND table_name = 'tblCourse'
                     AND column_name = 'openingUnit') THEN
        ALTER TABLE tblCourse ADD COLUMN openingUnit VARCHAR(80) NULL AFTER courseNature;
    END IF;

    CREATE TABLE IF NOT EXISTS tblTeachingClass (
        teachingClassId VARCHAR(32) NOT NULL,
        courseId VARCHAR(20) NOT NULL,
        classNumber VARCHAR(10) NOT NULL,
        teacher VARCHAR(60) NOT NULL,
        capacity INT NOT NULL,
        selectedCount INT NOT NULL DEFAULT 0,
        teachingLanguage VARCHAR(20) NULL,
        remark VARCHAR(120) NULL,
        PRIMARY KEY (teachingClassId),
        UNIQUE KEY uk_tblTeachingClass_course_number (courseId, classNumber),
        UNIQUE KEY uk_tblTeachingClass_id_course (teachingClassId, courseId),
        CONSTRAINT fk_tblTeachingClass_course FOREIGN KEY (courseId)
            REFERENCES tblCourse(courseId) ON DELETE RESTRICT ON UPDATE CASCADE,
        CONSTRAINT chk_tblTeachingClass_capacity CHECK (capacity >= 0),
        CONSTRAINT chk_tblTeachingClass_selected
            CHECK (selectedCount >= 0 AND selectedCount <= capacity),
        INDEX idx_tblTeachingClass_teacher (teacher),
        INDEX idx_tblTeachingClass_course (courseId)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

    IF EXISTS (SELECT 1 FROM information_schema.table_constraints
               WHERE constraint_schema = DATABASE() AND table_name = 'tblTeachingClass'
                 AND constraint_name = 'chk_tblTeachingClass_capacity') THEN
        ALTER TABLE tblTeachingClass DROP CHECK chk_tblTeachingClass_capacity;
    END IF;
    ALTER TABLE tblTeachingClass ADD CONSTRAINT chk_tblTeachingClass_capacity
        CHECK (capacity >= 0);
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics
                   WHERE table_schema = DATABASE() AND table_name = 'tblTeachingClass'
                     AND index_name = 'uk_tblTeachingClass_id_course') THEN
        ALTER TABLE tblTeachingClass ADD CONSTRAINT uk_tblTeachingClass_id_course
            UNIQUE (teachingClassId, courseId);
    END IF;

    INSERT IGNORE INTO tblTeachingClass
        (teachingClassId, courseId, classNumber, teacher, capacity, selectedCount, remark)
    SELECT CONCAT(c.courseId, '-01'), c.courseId, '01', c.teacher, c.capacity,
           c.selectedCount, 'Course 1.0 migration'
    FROM tblCourse c
    WHERE NOT EXISTS (SELECT 1 FROM tblTeachingClass tc WHERE tc.courseId=c.courseId);

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = DATABASE() AND table_name = 'tblSelectCourse'
                     AND column_name = 'teachingClassId') THEN
        ALTER TABLE tblSelectCourse ADD COLUMN teachingClassId VARCHAR(32) NULL AFTER studentId;
    END IF;
    UPDATE tblSelectCourse sc
    JOIN tblTeachingClass tc ON tc.courseId = sc.courseId AND tc.classNumber = '01'
    SET sc.teachingClassId = tc.teachingClassId
    WHERE sc.teachingClassId IS NULL;
    UPDATE tblTeachingClass tc
    SET tc.selectedCount=(SELECT COUNT(*) FROM tblSelectCourse sc
                          WHERE sc.teachingClassId=tc.teachingClassId);
    UPDATE tblCourse c
    SET c.selectedCount=(SELECT COALESCE(SUM(tc.selectedCount),0)
                         FROM tblTeachingClass tc WHERE tc.courseId=c.courseId),
        c.capacity=(SELECT COALESCE(SUM(tc.capacity),c.capacity)
                    FROM tblTeachingClass tc WHERE tc.courseId=c.courseId);
    ALTER TABLE tblSelectCourse MODIFY teachingClassId VARCHAR(32) NOT NULL;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics
                   WHERE table_schema = DATABASE() AND table_name = 'tblSelectCourse'
                     AND index_name = 'uk_tblSelectCourse_student_class') THEN
        ALTER TABLE tblSelectCourse ADD CONSTRAINT uk_tblSelectCourse_student_class
            UNIQUE (studentId, teachingClassId);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints
                   WHERE constraint_schema = DATABASE() AND table_name = 'tblSelectCourse'
                     AND constraint_name = 'fk_tblSelectCourse_class') THEN
        ALTER TABLE tblSelectCourse ADD CONSTRAINT fk_tblSelectCourse_class
            FOREIGN KEY (teachingClassId) REFERENCES tblTeachingClass(teachingClassId)
            ON DELETE RESTRICT ON UPDATE CASCADE;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints
                   WHERE constraint_schema = DATABASE() AND table_name = 'tblSelectCourse'
                     AND constraint_name = 'fk_tblSelectCourse_class_course') THEN
        ALTER TABLE tblSelectCourse ADD CONSTRAINT fk_tblSelectCourse_class_course
            FOREIGN KEY (teachingClassId, courseId)
            REFERENCES tblTeachingClass(teachingClassId, courseId)
            ON DELETE RESTRICT ON UPDATE CASCADE;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = DATABASE() AND table_name = 'tblCourseSchedule'
                     AND column_name = 'teachingClassId') THEN
        ALTER TABLE tblCourseSchedule ADD COLUMN teachingClassId VARCHAR(32) NULL AFTER scheduleId;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = DATABASE() AND table_name = 'tblCourseSchedule'
                     AND column_name = 'weekStart') THEN
        ALTER TABLE tblCourseSchedule
            ADD COLUMN weekStart TINYINT NOT NULL DEFAULT 1 AFTER courseId,
            ADD COLUMN weekEnd TINYINT NOT NULL DEFAULT 16 AFTER weekStart,
            ADD COLUMN startPeriod TINYINT NOT NULL DEFAULT 1 AFTER dayOfWeek,
            ADD COLUMN endPeriod TINYINT NOT NULL DEFAULT 2 AFTER startPeriod;
    END IF;
    UPDATE tblCourseSchedule s
    JOIN tblTeachingClass tc ON tc.courseId = s.courseId AND tc.classNumber = '01'
    SET s.teachingClassId = tc.teachingClassId
    WHERE s.teachingClassId IS NULL;
    UPDATE tblCourseSchedule s
    JOIN tblTeachingClass tc ON tc.teachingClassId=s.teachingClassId
                            AND tc.remark='Course 1.0 migration'
    SET s.weekStart=1, s.weekEnd=16,
        s.startPeriod=CASE
            WHEN s.startTime<'09:50' THEN 1 WHEN s.startTime<'11:30' THEN 3
            WHEN s.startTime<'13:50' THEN 5 WHEN s.startTime<'14:50' THEN 6
            WHEN s.startTime<'16:00' THEN 7 WHEN s.startTime<'16:50' THEN 8
            WHEN s.startTime<'18:30' THEN 9 WHEN s.startTime<'19:00' THEN 10
            ELSE 11 END,
        s.endPeriod=CASE
            WHEN s.endTime<='09:50' THEN 2 WHEN s.endTime<='11:50' THEN 4
            WHEN s.endTime<='12:30' THEN 5 WHEN s.endTime<='15:50' THEN 7
            WHEN s.endTime<='17:50' THEN 9 WHEN s.endTime<='18:30' THEN 10
            WHEN s.endTime<='20:40' THEN 12 ELSE 13 END;
    ALTER TABLE tblCourseSchedule MODIFY teachingClassId VARCHAR(32) NOT NULL;
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints
                   WHERE constraint_schema = DATABASE() AND table_name = 'tblCourseSchedule'
                     AND constraint_name = 'fk_tblCourseSchedule_class') THEN
        ALTER TABLE tblCourseSchedule ADD CONSTRAINT fk_tblCourseSchedule_class
            FOREIGN KEY (teachingClassId) REFERENCES tblTeachingClass(teachingClassId)
            ON DELETE RESTRICT ON UPDATE CASCADE;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints
                   WHERE constraint_schema = DATABASE() AND table_name = 'tblCourseSchedule'
                     AND constraint_name = 'fk_tblCourseSchedule_class_course') THEN
        ALTER TABLE tblCourseSchedule ADD CONSTRAINT fk_tblCourseSchedule_class_course
            FOREIGN KEY (teachingClassId, courseId)
            REFERENCES tblTeachingClass(teachingClassId, courseId)
            ON DELETE RESTRICT ON UPDATE CASCADE;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.statistics
               WHERE table_schema = DATABASE() AND table_name = 'tblCourseSchedule'
                 AND index_name = 'uk_tblCourseSchedule_course_slot') THEN
        ALTER TABLE tblCourseSchedule DROP INDEX uk_tblCourseSchedule_course_slot;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics
                   WHERE table_schema = DATABASE() AND table_name = 'tblCourseSchedule'
                     AND index_name = 'uk_tblCourseSchedule_class_slot') THEN
        ALTER TABLE tblCourseSchedule ADD CONSTRAINT uk_tblCourseSchedule_class_slot
            UNIQUE (teachingClassId,weekStart,weekEnd,dayOfWeek,startPeriod,endPeriod);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints
                   WHERE constraint_schema = DATABASE() AND table_name = 'tblCourseSchedule'
                     AND constraint_name = 'chk_tblCourseSchedule_week') THEN
        ALTER TABLE tblCourseSchedule ADD CONSTRAINT chk_tblCourseSchedule_week
            CHECK (weekStart BETWEEN 1 AND 30 AND weekEnd BETWEEN weekStart AND 30);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints
                   WHERE constraint_schema = DATABASE() AND table_name = 'tblCourseSchedule'
                     AND constraint_name = 'chk_tblCourseSchedule_period') THEN
        ALTER TABLE tblCourseSchedule ADD CONSTRAINT chk_tblCourseSchedule_period
            CHECK (startPeriod BETWEEN 1 AND 13 AND endPeriod BETWEEN startPeriod AND 13);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics
                   WHERE table_schema = DATABASE() AND table_name = 'tblCourseSchedule'
                     AND index_name = 'idx_tblCourseSchedule_room_period') THEN
        ALTER TABLE tblCourseSchedule ADD INDEX idx_tblCourseSchedule_room_period
            (dayOfWeek,classroom,weekStart,weekEnd,startPeriod,endPeriod);
    END IF;
END$$
DELIMITER ;

CALL course_phase1_migrate();
DROP PROCEDURE course_phase1_migrate;
