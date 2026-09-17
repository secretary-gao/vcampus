package vcampus.client.biz;

import vcampus.common.constant.StudentProtocol;
import vcampus.common.vo.Student;
import vcampus.common.vo.StudentStatus;
import vcampus.common.vo.User;
import vcampus.server.dao.DbHelper;
import vcampus.server.srv.ServerThread;

import java.net.ServerSocket;
import java.net.InetAddress;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.UUID;

/** 通过真实选退课客户端与统一服务器验证教师学籍范围；只清理本次创建的数据。 */
public final class StudentCourseEnrollmentTest {

    private static final String PASSWORD_HASH = "11111111111111111111111111111111";

    public static void main(String[] args) throws Exception {
        String token = UUID.randomUUID().toString().replace("-", "").substring(0, 7);
        String teacher = "T" + token;
        String otherTeacher = "Q" + token;
        String studentUser = "S" + token;
        String otherStudentUser = "U" + token;
        String student = "ST" + token;
        String otherStudent = "SU" + token;
        String teacherName = "师" + token;
        String otherTeacherName = "师乙" + token;
        String firstCourse = "SC-A-" + token;
        String secondCourse = "SC-B-" + token;
        String firstClass = firstCourse + "-01";
        String secondClass = secondCourse + "-01";
        String otherClass = firstCourse + "-02";
        List<String> courses = List.of(firstCourse, secondCourse);
        List<String> classes = List.of(firstClass, secondClass, otherClass);
        List<String> students = List.of(student, otherStudent);
        List<String> users = List.of(teacher, otherTeacher, studentUser, otherStudentUser);

        // 系统分配独立的本机端口，不占用正在运行的 vCampus 服务。
        try (ServerSocket listener = new ServerSocket(0, 0, InetAddress.getByName("127.0.0.1"))) {
            Thread server = new Thread(() -> {
                while (!listener.isClosed()) {
                    try {
                        new ServerThread(listener.accept()).run();
                    } catch (Exception error) {
                        if (!listener.isClosed()) {
                            throw new RuntimeException(error);
                        }
                    }
                }
            }, "student-course-test-server");
            server.setDaemon(true);
            server.start();

            createFixture(users, students, courses, classes, teacherName, otherTeacherName);
            try {
                CourseClientSrv courseClient = new CourseClientSrv("127.0.0.1", listener.getLocalPort());
                StudentClientSrv teacherClient = new StudentClientSrv(credentials(teacher, "教师"),
                        "127.0.0.1", listener.getLocalPort());
                StudentClientSrv otherTeacherClient = new StudentClientSrv(
                        credentials(otherTeacher, "教师"), "127.0.0.1", listener.getLocalPort());
                require(student.equals(courseClient.queryStudentId(studentUser)),
                        "选课必须把登录账号映射为正式学号");
                require(teacherClient.findAll().isEmpty(), "选课前不应出现在教师学籍列表");
                require(teacherClient.findByStudentId(student) == null, "选课前不应能查询学籍");

                // 同一课程的两个班由不同教师任教，不能使用 Course 的兼容教师字段授权。
                courseClient.selectCourse(otherStudent, otherClass);
                courseClient.selectCourse(student, firstClass);
                assertOnlyStudent(teacherClient, student);
                assertOnlyStudent(otherTeacherClient, otherStudent);
                require(courseClient.queryTeacherCourseEnrollments(credentials(teacher, "教师")).stream()
                                .anyMatch(row -> student.equals(row.getStudentId())),
                        "选课学生未进入教师课程名单");
                require(courseClient.queryTeacherCourseEnrollments(credentials(otherTeacher, "教师")).stream()
                                .anyMatch(row -> otherStudent.equals(row.getStudentId())),
                        "另一教师未读取到自己的教学班名单");
                expectCourseForbidden(() -> courseClient.queryTeacherCourseEnrollments(
                                credentials(studentUser, "教师")),
                        "学生伪造教师角色后不应读取教学班名单");
                User forgedIdentity = credentials(teacher, "学生");
                forgedIdentity.setUName(otherTeacherName);
                require(courseClient.queryTeacherCourseEnrollments(forgedIdentity).stream()
                                .noneMatch(row -> otherStudent.equals(row.getStudentId())),
                        "教学班名单不能信任客户端伪造的教师姓名和角色");
                User wrongPassword = credentials(teacher, "教师");
                wrongPassword.setUPwd("wrong-password");
                expectCourseForbidden(() -> courseClient.queryTeacherCourseEnrollments(wrongPassword),
                        "密码错误时不应读取教学班名单");
                Student visible = teacherClient.findByStudentId(student);
                require(visible != null && "联调学生".equals(visible.getName())
                                && "2024".equals(visible.getGrade())
                                && visible.getStatus() == StudentStatus.ENROLLED,
                        "教师未读到正确学籍字段");
                require(("CARD-" + student).equals(visible.getCampusCardNo())
                                && visible.getUserId() == null,
                        "教师应获得一卡通号但不应获得登录账号");
                require(teacherClient.findByStudentId(otherStudent) == null,
                        "教师不应看到其他教师的学生");
                require(teacherClient.findByName("联调学生").size() == 1,
                        "按姓名查询必须局限于本人课程学生");
                require(teacherClient.findByStudentId(student.toLowerCase(java.util.Locale.ROOT)) == null,
                        "学号精确查询不能混淆大小写");
                require(teacherClient.findByName("联调学生' OR '1'='1").isEmpty(),
                        "查询条件必须作为普通姓名处理");

                for (String status : List.of(User.STATUS_DISABLED, User.STATUS_PENDING)) {
                    try (Connection connection = DbHelper.getConnection()) {
                        execute(connection, "UPDATE tblUser SET uStatus = ? WHERE uId = ?", status, teacher);
                    }
                    expectForbidden(teacherClient::findAll, "非正常账号不能继续读取学籍列表");
                    expectForbidden(() -> teacherClient.findByStudentId(student), "非正常账号不能打开学籍详情");
                    expectForbidden(() -> teacherClient.findByName("联调学生"), "非正常账号不能按姓名查询");
                    expectCourseForbidden(() -> courseClient.queryTeacherCourseEnrollments(
                                    credentials(teacher, "教师")),
                            "非正常账号不能读取教学班名单");
                }
                try (Connection connection = DbHelper.getConnection()) {
                    execute(connection, "UPDATE tblUser SET uStatus = ? WHERE uId = ?", User.STATUS_NORMAL, teacher);
                    execute(connection, "UPDATE tblUser SET uName = ? WHERE uId = ?", teacherName, otherTeacher);
                }
                require(teacherClient.findAll().isEmpty()
                                && teacherClient.findByStudentId(student) == null
                                && teacherClient.findByName("联调学生").isEmpty(),
                        "存在同名教师时，列表与精确查询均不能授予学籍权限");
                expectCourseForbidden(() -> courseClient.queryTeacherCourseEnrollments(
                                credentials(teacher, "教师")),
                        "存在同名教师时不能读取教学班名单");
                require(otherTeacherClient.findByStudentId(student) == null,
                        "同名教师不能获得另一账号的授课权限");
                try (Connection connection = DbHelper.getConnection()) {
                    execute(connection, "UPDATE tblUser SET uName = ? WHERE uId = ?", otherTeacherName, otherTeacher);
                    execute(connection, "UPDATE tblTeachingClass SET teacher = ? WHERE teachingClassId = ?",
                            otherTeacherName, firstClass);
                }
                require(teacherClient.findByStudentId(student) == null,
                        "更换任课教师后原教师应失去学籍查看权限");
                require(otherTeacherClient.findByStudentId(student) != null,
                        "更换任课教师后新教师应获得学籍查看权限");
                try (Connection connection = DbHelper.getConnection()) {
                    execute(connection, "UPDATE tblTeachingClass SET teacher = ? WHERE teachingClassId = ?",
                            teacherName, firstClass);
                }
                assertOnlyStudent(teacherClient, student);

                try {
                    teacherClient.updateStudent(visible);
                    throw new AssertionError("教师不应修改学籍");
                } catch (StudentClientException expected) {
                    require(StudentProtocol.STATUS_FORBIDDEN.equals(expected.getStatusCode()),
                            "教师写操作应返回无权限");
                }

                try (Connection connection = DbHelper.getConnection()) {
                    execute(connection, "UPDATE tblStudent SET status = '休学' WHERE studentId = ?", student);
                }
                require(teacherClient.findByStudentId(student).getStatus() == StudentStatus.SUSPENDED,
                        "查看学籍应读取最新状态，不能复用选课名单快照");

                expectCourseRejected(() -> courseClient.selectCourse(student, secondClass),
                        "休学学生不应新增选课");
                try (Connection connection = DbHelper.getConnection()) {
                    execute(connection, "UPDATE tblStudent SET status = '在读' WHERE studentId = ?", student);
                }
                courseClient.selectCourse(student, secondClass);
                assertOnlyStudent(teacherClient, student);
                courseClient.dropCourse(student, firstClass);
                assertOnlyStudent(teacherClient, student);
                courseClient.dropCourse(student, secondClass);
                require(teacherClient.findAll().isEmpty(), "退掉全部课程后应从教师学籍列表移除");
                require(teacherClient.findByStudentId(student) == null,
                        "旧选课名单不应在退课后继续授予学籍查看权限");
                require(teacherClient.findByName("联调学生").isEmpty(),
                        "退课后按姓名也不应看到学籍");
                assertOnlyStudent(otherTeacherClient, otherStudent);
                courseClient.selectCourse(student, firstClass);
                assertOnlyStudent(teacherClient, student);
                System.out.println("Student course enrollment tests passed");
            } finally {
                cleanup(users, students, courses);
            }
        }
    }

    private static void createFixture(List<String> users, List<String> students,
                                      List<String> courses, List<String> classes,
                                      String teacher, String otherTeacher)
            throws Exception {
        try (Connection connection = DbHelper.getConnection()) {
            connection.setAutoCommit(false);
            try {
                for (int i = 0; i < users.size(); i++) {
                    String name = i == 0 ? teacher : i == 1 ? otherTeacher : "联调学生";
                    execute(connection, "INSERT INTO tblUser (uId, uName, uPwd, uRole) VALUES (?, ?, ?, ?)",
                            users.get(i), name, PASSWORD_HASH, i < 2 ? "教师" : "学生");
                }
                for (int i = 0; i < students.size(); i++) {
                    execute(connection, "INSERT INTO tblStudent "
                                    + "(studentId, campusCardNo, userId, name, className, major, grade, status) "
                                    + "VALUES (?, ?, ?, '联调学生', '计算机一班', '计算机科学与技术', '2024', '在读')",
                            students.get(i), "CARD-" + students.get(i), users.get(i + 2));
                }
                for (int i = 0; i < courses.size(); i++) {
                    execute(connection, "INSERT INTO tblCourse "
                                    + "(courseId, courseName, teacher, credit, capacity, selectedCount) "
                                    + "VALUES (?, '学籍联调课程', ?, 2, 10, 0)",
                            courses.get(i), teacher);
                }
                for (int i = 0; i < classes.size(); i++) {
                    execute(connection, "INSERT INTO tblTeachingClass "
                                    + "(teachingClassId, courseId, classNumber, teacher, capacity, selectedCount) "
                                    + "VALUES (?, ?, ?, ?, 10, 0)",
                            classes.get(i), courses.get(i == 2 ? 0 : i),
                            i == 2 ? "02" : "01", i == 2 ? otherTeacher : teacher);
                }
                connection.commit();
            } catch (Exception error) {
                connection.rollback();
                throw error;
            }
        }
    }

    private static void cleanup(List<String> users, List<String> students, List<String> courses)
            throws Exception {
        try (Connection connection = DbHelper.getConnection()) {
            connection.setAutoCommit(false);
            try {
                for (String course : courses) {
                    execute(connection, "DELETE FROM tblSelectCourse WHERE courseId = ?", course);
                    execute(connection, "DELETE FROM tblTeachingClass WHERE courseId = ?", course);
                    execute(connection, "DELETE FROM tblCourse WHERE courseId = ?", course);
                }
                for (String student : students) {
                    execute(connection, "DELETE FROM tblStudent WHERE studentId = ?", student);
                }
                for (String user : users) {
                    execute(connection, "DELETE FROM tblUser WHERE uId = ?", user);
                }
                connection.commit();
            } catch (Exception error) {
                connection.rollback();
                throw error;
            }
        }
    }

    private static void execute(Connection connection, String sql, Object... parameters) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < parameters.length; i++) {
                statement.setObject(i + 1, parameters[i]);
            }
            statement.executeUpdate();
        }
    }

    private static User credentials(String id, String role) {
        User user = new User();
        user.setUId(id);
        user.setUPwd(PASSWORD_HASH);
        user.setURole(role);
        return user;
    }

    private static void assertOnlyStudent(StudentClientSrv client, String studentId) throws Exception {
        List<Student> rows = client.findAll();
        require(rows.size() == 1 && studentId.equals(rows.get(0).getStudentId()),
                "教师学籍列表应去重且仅包含自己课程的学生");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void expectForbidden(CheckedOperation operation, String message) throws Exception {
        try {
            operation.run();
            throw new AssertionError(message);
        } catch (StudentClientException expected) {
            require(StudentProtocol.STATUS_FORBIDDEN.equals(expected.getStatusCode()), message);
        }
    }

    private static void expectCourseForbidden(CheckedOperation operation, String message) throws Exception {
        try {
            operation.run();
            throw new AssertionError(message);
        } catch (CourseClientException expected) {
            require(StudentProtocol.STATUS_FORBIDDEN.equals(expected.getStatusCode()), message);
        }
    }

    private static void expectCourseRejected(CheckedOperation operation, String message) throws Exception {
        try {
            operation.run();
            throw new AssertionError(message);
        } catch (CourseClientException expected) {
            require(StudentProtocol.STATUS_BAD_REQUEST.equals(expected.getStatusCode()), message);
        }
    }

    @FunctionalInterface
    private interface CheckedOperation {
        void run() throws Exception;
    }
}
