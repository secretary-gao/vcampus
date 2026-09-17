USE vCampus;

CREATE TABLE IF NOT EXISTS tblCourseScore (
    scoreId VARCHAR(24) NOT NULL,
    studentId VARCHAR(10) NOT NULL,
    courseId VARCHAR(20) NOT NULL,
    teachingClassId VARCHAR(32) NOT NULL,
    teacher VARCHAR(60) NOT NULL,
    score DECIMAL(5,2) NOT NULL,
    status VARCHAR(12) NOT NULL DEFAULT 'PENDING',
    submittedAt DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewedAt DATETIME NULL,
    PRIMARY KEY (scoreId),
    UNIQUE KEY uk_course_score_student_class (studentId, teachingClassId),
    CONSTRAINT fk_course_score_student FOREIGN KEY (studentId)
        REFERENCES tblStudent(studentId) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_course_score_class FOREIGN KEY (teachingClassId)
        REFERENCES tblTeachingClass(teachingClassId) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_course_score_class_course FOREIGN KEY (teachingClassId, courseId)
        REFERENCES tblTeachingClass(teachingClassId, courseId) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_course_score_value CHECK (score >= 0 AND score <= 100),
    CONSTRAINT chk_course_score_status CHECK (status IN ('PENDING','APPROVED','REJECTED')),
    INDEX idx_course_score_student_status (studentId, status),
    INDEX idx_course_score_status (status),
    INDEX idx_course_score_teacher_class (teacher, teachingClassId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程成绩：教师提交、管理员审核后发布';
