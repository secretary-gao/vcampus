USE vCampus;

CREATE TABLE IF NOT EXISTS tblTeacherStudentFocus (
    teacherUserId CHAR(8) NOT NULL COMMENT '教师账号',
    studentId VARCHAR(10) NOT NULL COMMENT '学生学号',
    tags VARCHAR(200) NULL COMMENT '教师标签，多个标签用逗号分隔',
    note VARCHAR(200) NULL COMMENT '教师备注',
    createdAt DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (teacherUserId, studentId),
    CONSTRAINT fk_focus_teacher FOREIGN KEY (teacherUserId)
        REFERENCES tblUser(uId) ON DELETE CASCADE,
    CONSTRAINT fk_focus_student FOREIGN KEY (studentId)
        REFERENCES tblStudent(studentId) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
