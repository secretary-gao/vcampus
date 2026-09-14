-- Only groups that are explicit Chinese/full-English alternatives in Reality Track data.
USE vCampus;

INSERT INTO tblCourseRequirementGroup (groupId, groupName, rule) VALUES
('REQ-DATABASE', '数据库原理二选一', 'CHOOSE_ONE'),
('REQ-NETWORK', '计算机网络二选一', 'CHOOSE_ONE'),
('REQ-COMPILER', '编译原理二选一', 'CHOOSE_ONE')
ON DUPLICATE KEY UPDATE groupName=VALUES(groupName), rule=VALUES(rule);

INSERT IGNORE INTO tblCourseRequirementGroupMember (groupId, courseId) VALUES
('REQ-DATABASE', 'B09D0012'), ('REQ-DATABASE', 'B09D0013'),
('REQ-NETWORK', 'B09N0014'), ('REQ-NETWORK', 'B09N0015'),
('REQ-COMPILER', 'B71S0032'), ('REQ-COMPILER', 'B71S0033');
