-- Idempotent migration for equivalent-course CHOOSE_ONE rules.
USE vCampus;

CREATE TABLE IF NOT EXISTS tblCourseRequirementGroup (
    groupId   VARCHAR(32) NOT NULL,
    groupName VARCHAR(80) NOT NULL,
    rule      VARCHAR(20) NOT NULL,
    PRIMARY KEY (groupId),
    CONSTRAINT chk_tblCourseRequirementGroup_rule CHECK (rule IN ('CHOOSE_ONE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程等价与培养要求组';

CREATE TABLE IF NOT EXISTS tblCourseRequirementGroupMember (
    groupId  VARCHAR(32) NOT NULL,
    courseId VARCHAR(20) NOT NULL,
    PRIMARY KEY (groupId, courseId),
    CONSTRAINT fk_tblCourseRequirementGroupMember_group FOREIGN KEY (groupId)
        REFERENCES tblCourseRequirementGroup(groupId) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_tblCourseRequirementGroupMember_course FOREIGN KEY (courseId)
        REFERENCES tblCourse(courseId) ON DELETE RESTRICT ON UPDATE CASCADE,
    INDEX idx_tblCourseRequirementGroupMember_course (courseId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程要求组成员';
