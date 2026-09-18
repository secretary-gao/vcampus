package vcampus.client.biz;

import vcampus.common.constant.StudentProtocol;
import vcampus.common.vo.Student;
import vcampus.common.vo.StudentStatus;
import vcampus.common.vo.User;
import vcampus.server.dao.DbHelper;
import vcampus.server.dao.StudentDAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** 连接统一服务器，验证三种角色的学籍权限和字段范围。 */
public class StudentSocketRoleTest {

    public static void main(String[] args) throws Exception {
        long suffix = System.currentTimeMillis() % 1_000_000L;
        String studentUserId = "S" + String.format("%07d", suffix % 10_000_000L);
        String teacherUserId = "T" + String.format("%07d", suffix % 10_000_000L);
        String adminUserId = "A" + String.format("%07d", suffix % 10_000_000L);
        String unrelatedUserId = "U" + String.format("%07d", suffix % 10_000_000L);
        String studentId = "S" + String.format("%09d", suffix);
        String changedStudentId = "R" + String.format("%09d", suffix);
        String unrelatedStudentId = "U" + String.format("%09d", suffix);
        String teacherName = "师" + String.format("%06d", suffix);
        String courseId = "ROLE-C" + suffix;
        String selectId = "ROLE-S" + suffix;
        String passwordHash = "11111111111111111111111111111111";
        CourseSchemaState schemaState = ensureCourseTables();

        try {
            insertUser(studentUserId, "测试学生", "学生", passwordHash);
            insertUser(teacherUserId, teacherName, "教师", passwordHash);
            insertUser(adminUserId, "测试管理员", "管理员", passwordHash);
            insertUser(unrelatedUserId, "无关学生", "学生", passwordHash);

            Student student = sampleStudent(studentId, studentUserId, suffix);
            new StudentDAO().insert(student);
            Student unrelatedStudent = sampleStudent(
                    unrelatedStudentId, unrelatedUserId, suffix + 1);
            unrelatedStudent.setName("未分配学生");
            new StudentDAO().insert(unrelatedStudent);
            insertCourseSelection(courseId, selectId, teacherName, studentId);

            StudentClientSrv studentClient = new StudentClientSrv(
                    credentials(studentUserId, "学生", passwordHash));
            Student self = studentClient.getMyStudentInfo();
            require(self != null && studentId.equals(self.getStudentId()), "学生未读取到本人学籍");
            expectForbidden(studentClient::findAll, "学生查询了全部学籍");

            StudentClientSrv teacherClient = new StudentClientSrv(
                    credentials(teacherUserId, "教师", passwordHash));
            List<Student> assignedStudents = teacherClient.findAll();
            require(assignedStudents.size() == 1
                            && studentId.equals(assignedStudents.get(0).getStudentId()),
                    "教师列表包含了不属于自己课程的学生");
            List<Student> teacherRows = teacherClient.findByName("权限测试学生");
            require(!teacherRows.isEmpty(), "教师未查询到学生");
            require(teacherRows.get(0).getCampusCardNo() != null
                            && teacherRows.get(0).getUserId() == null,
                    "教师响应未包含学号/一卡通号，或暴露了账号字段");
            require(teacherClient.findByStudentId(unrelatedStudentId) == null,
                    "教师按学号查询到了无关学生");
            require(teacherClient.findByName("未分配学生").isEmpty(),
                    "教师按姓名查询到了无关学生");
            expectForbidden(() -> teacherClient.findByCampusCardNo(student.getCampusCardNo()),
                    "教师按一卡通号完成了查询");
            expectForbidden(() -> teacherClient.updateStudent(student), "教师修改了学籍");

            StudentClientSrv adminClient = new StudentClientSrv(
                    credentials(adminUserId, "管理员", passwordHash));
            Student adminView = adminClient.findByStudentId(studentId);
            require(adminView != null && adminView.getCampusCardNo() != null,
                    "管理员未读取到完整字段");
            adminView.setStatus(StudentStatus.SUSPENDED);
            Student updated = adminClient.updateStudent(adminView);
            require(updated.getStatus() == StudentStatus.SUSPENDED, "管理员审核状态未保存");

            updated.setStudentId(unrelatedStudentId);
            expectConflict(() -> adminClient.updateStudent(studentId, updated),
                    "管理员把学号修改为已有学号");
            updated.setStudentId(changedStudentId);
            updated.setCampusCardNo(unrelatedStudent.getCampusCardNo());
            expectConflict(() -> adminClient.updateStudent(studentId, updated),
                    "管理员把一卡通号修改为已有一卡通号");
            updated.setCampusCardNo(student.getCampusCardNo());
            Student renamed = adminClient.updateStudent(studentId, updated);
            require(changedStudentId.equals(renamed.getStudentId()), "管理员修改学号未保存");
            require(adminClient.findByStudentId(studentId) == null,
                    "学号修改后仍能按原学号查询到档案");
            require(selectionUsesStudentId(selectId, changedStudentId),
                    "学号修改后选课记录未同步更新");
            require(teacherClient.findByStudentId(changedStudentId) != null,
                    "学号修改后教师无法查看该学生学籍");

            System.out.println("Student socket role tests passed");
        } finally {
            cleanup(selectId, courseId, schemaState,
                    List.of(studentId, changedStudentId, unrelatedStudentId),
                    List.of(studentUserId, teacherUserId, adminUserId, unrelatedUserId));
        }
    }

    private static User credentials(String userId, String role, String passwordHash) {
        User user = new User();
        user.setUId(userId);
        user.setUPwd(passwordHash);
        user.setURole(role);
        return user;
    }

    private static Student sampleStudent(String studentId, String userId, long suffix) {
        Student student = new Student();
        student.setStudentId(studentId);
        student.setCampusCardNo("ROLE-CARD-" + suffix);
        student.setUserId(userId);
        student.setName("权限测试学生");
        student.setClassName("计算机一班");
        student.setMajor("计算机科学与技术");
        student.setGrade("2024");
        student.setEnrollmentDate(LocalDate.of(2024, 9, 1));
        student.setStatus(StudentStatus.ENROLLED);
        return student;
    }

    private static void insertUser(String userId, String name, String role, String passwordHash)
            throws Exception {
        String sql = "INSERT INTO tblUser (uId, uName, uPwd, uRole) VALUES (?, ?, ?, ?)";
        try (Connection connection = DbHelper.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, userId);
            statement.setString(2, name);
            statement.setString(3, passwordHash);
            statement.setString(4, role);
            statement.executeUpdate();
        }
    }

    private static CourseSchemaState ensureCourseTables() throws Exception {
        boolean createdCourse = false;
        boolean createdSelection = false;
        try (Connection connection = DbHelper.getConnection();
             Statement statement = connection.createStatement()) {
            if (!tableExists(connection, "tblCourse")) {
                statement.executeUpdate("CREATE TABLE tblCourse ("
                        + "courseId VARCHAR(20) NOT NULL PRIMARY KEY, "
                        + "courseName VARCHAR(50) NOT NULL, teacher VARCHAR(20) NOT NULL, "
                        + "credit INT NOT NULL, capacity INT NOT NULL, "
                        + "selectedCount INT NOT NULL DEFAULT 0) ENGINE=InnoDB");
                createdCourse = true;
            }
            if (!tableExists(connection, "tblSelectCourse")) {
                statement.executeUpdate("CREATE TABLE tblSelectCourse ("
                        + "selectId VARCHAR(20) NOT NULL PRIMARY KEY, "
                        + "studentId VARCHAR(10) NOT NULL, courseId VARCHAR(20) NOT NULL, "
                        + "selectTime DATETIME NOT NULL, "
                        + "UNIQUE KEY uk_test_student_course (studentId, courseId), "
                        + "CONSTRAINT fk_test_select_course FOREIGN KEY (courseId) "
                        + "REFERENCES tblCourse(courseId)) ENGINE=InnoDB");
                createdSelection = true;
            }
            return new CourseSchemaState(createdCourse, createdSelection);
        } catch (Exception exception) {
            dropCreatedCourseTables(createdCourse, createdSelection);
            throw exception;
        }
    }

    private static boolean tableExists(Connection connection, String tableName) throws Exception {
        String sql = "SELECT 1 FROM information_schema.TABLES "
                + "WHERE TABLE_SCHEMA = DATABASE() AND LOWER(TABLE_NAME) = LOWER(?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, tableName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private static void insertCourseSelection(String courseId, String selectId,
                                              String teacherName, String studentId)
            throws Exception {
        try (Connection connection = DbHelper.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO tblCourse "
                            + "(courseId, courseName, teacher, credit, capacity, selectedCount) "
                            + "VALUES (?, '权限测试课程', ?, 1, 10, 1)")) {
                statement.setString(1, courseId);
                statement.setString(2, teacherName);
                statement.executeUpdate();
            }
            if (columnExists(connection, "tblSelectCourse", "teachingClassId")) {
                String teachingClassId = courseId + "-01";
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO tblTeachingClass "
                                + "(teachingClassId, courseId, classNumber, teacher, capacity, selectedCount) "
                                + "VALUES (?, ?, '01', ?, 10, 1)")) {
                    statement.setString(1, teachingClassId);
                    statement.setString(2, courseId);
                    statement.setString(3, teacherName);
                    statement.executeUpdate();
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO tblSelectCourse "
                                + "(selectId, studentId, teachingClassId, courseId, selectTime) "
                                + "VALUES (?, ?, ?, ?, ?)")) {
                    statement.setString(1, selectId);
                    statement.setString(2, studentId);
                    statement.setString(3, teachingClassId);
                    statement.setString(4, courseId);
                    statement.setObject(5, LocalDateTime.now());
                    statement.executeUpdate();
                }
            } else {
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO tblSelectCourse "
                                + "(selectId, studentId, courseId, selectTime) VALUES (?, ?, ?, ?)")) {
                    statement.setString(1, selectId);
                    statement.setString(2, studentId);
                    statement.setString(3, courseId);
                    statement.setObject(4, LocalDateTime.now());
                    statement.executeUpdate();
                }
            }
        }
    }

    private static boolean columnExists(Connection connection, String tableName,
                                        String columnName) throws Exception {
        String sql = "SELECT 1 FROM information_schema.COLUMNS "
                + "WHERE TABLE_SCHEMA = DATABASE() AND LOWER(TABLE_NAME) = LOWER(?) "
                + "AND LOWER(COLUMN_NAME) = LOWER(?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, tableName);
            statement.setString(2, columnName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private static boolean selectionUsesStudentId(String selectId, String studentId)
            throws Exception {
        try (Connection connection = DbHelper.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT 1 FROM tblSelectCourse WHERE selectId = ? AND studentId = ?")) {
            statement.setString(1, selectId);
            statement.setString(2, studentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private static void cleanup(String selectId, String courseId, CourseSchemaState schemaState,
                                List<String> studentIds, List<String> userIds) {
        try (Connection connection = DbHelper.getConnection()) {
            if (tableExists(connection, "tblSelectCourse")) {
                try (PreparedStatement statement = connection.prepareStatement(
                        "DELETE FROM tblSelectCourse WHERE selectId = ?")) {
                    statement.setString(1, selectId);
                    statement.executeUpdate();
                }
            }
            if (tableExists(connection, "tblCourse")) {
                if (tableExists(connection, "tblTeachingClass")) {
                    try (PreparedStatement statement = connection.prepareStatement(
                            "DELETE FROM tblTeachingClass WHERE courseId = ?")) {
                        statement.setString(1, courseId);
                        statement.executeUpdate();
                    }
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "DELETE FROM tblCourse WHERE courseId = ?")) {
                    statement.setString(1, courseId);
                    statement.executeUpdate();
                }
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM tblStudent WHERE studentId = ?")) {
                for (String studentId : studentIds) {
                    statement.setString(1, studentId);
                    statement.addBatch();
                }
                statement.executeBatch();
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM tblUser WHERE uId = ?")) {
                for (String userId : userIds) {
                    statement.setString(1, userId);
                    statement.addBatch();
                }
                statement.executeBatch();
            }
        } catch (Exception exception) {
            System.err.println("清理权限测试数据失败：" + exception.getMessage());
        } finally {
            dropCreatedCourseTables(schemaState.createdCourse(), schemaState.createdSelection());
        }
    }

    private static void dropCreatedCourseTables(boolean createdCourse, boolean createdSelection) {
        try (Connection connection = DbHelper.getConnection();
             Statement statement = connection.createStatement()) {
            if (createdSelection) {
                statement.executeUpdate("DROP TABLE IF EXISTS tblSelectCourse");
            }
            if (createdCourse) {
                statement.executeUpdate("DROP TABLE IF EXISTS tblCourse");
            }
        } catch (Exception exception) {
            System.err.println("清理权限测试临时表失败：" + exception.getMessage());
        }
    }

    private record CourseSchemaState(boolean createdCourse, boolean createdSelection) {
    }

    private static void expectForbidden(CheckedCall call, String error) throws Exception {
        try {
            call.run();
            throw new AssertionError(error);
        } catch (StudentClientException exception) {
            require(StudentProtocol.STATUS_FORBIDDEN.equals(exception.getStatusCode()),
                    "预期权限不足，实际状态码为 " + exception.getStatusCode());
        }
    }

    private static void expectConflict(CheckedCall call, String error) throws Exception {
        try {
            call.run();
            throw new AssertionError(error);
        } catch (StudentClientException exception) {
            require(StudentProtocol.STATUS_CONFLICT.equals(exception.getStatusCode()),
                    "预期数据冲突，实际状态码为 " + exception.getStatusCode());
        }
    }

    private static void require(boolean condition, String error) {
        if (!condition) {
            throw new AssertionError(error);
        }
    }

    @FunctionalInterface
    private interface CheckedCall {
        void run() throws Exception;
    }
}
