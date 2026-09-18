package vcampus.client.biz;

import vcampus.common.constant.IConstant;
import vcampus.common.constant.StudentProtocol;
import vcampus.common.util.MD5Util;
import vcampus.common.vo.Message;
import vcampus.common.vo.Student;
import vcampus.common.vo.StudentCampusOverview;
import vcampus.common.vo.StudentFocus;
import vcampus.common.vo.StudentStatus;
import vcampus.common.vo.TeacherCourseEnrollment;
import vcampus.common.vo.User;
import vcampus.server.dao.DbHelper;
import vcampus.server.dao.StudentDAO;
import vcampus.server.dao.UserDAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 使用真实数据库和 Socket 服务覆盖学生管理模块的关键角色、数据和边界流程。 */
public class StudentManagementIntegrationTest {

    private static final String ADMIN_ID = "ADMIN001";
    private static final String PASSWORD = "123456";
    private static final String PASSWORD_HASH = MD5Util.md5(PASSWORD);

    public static void main(String[] args) throws Exception {
        TestData data = TestData.create();
        try {
            data.insert();
            StudentManagementIntegrationTest test = new StudentManagementIntegrationTest(data);
            test.testAdminQueriesAndCohorts();
            test.testTeacherScopeAndFocus();
            test.testTeacherCourseNames();
            test.testStudentScopeAndOverview();
            test.testAdminMutationsAndAccountLinkage();
            test.testPasswordReset();
            System.out.println("STUDENT_MANAGEMENT_INTEGRATION_TEST=PASS");
        } finally {
            data.cleanup();
        }
    }

    private final TestData _data;
    private final User _admin;
    private final User _teacher;
    private final User _student;
    private final StudentClientSrv _adminClient;
    private final StudentClientSrv _teacherClient;
    private final StudentClientSrv _studentClient;

    private StudentManagementIntegrationTest(TestData data) {
        _data = data;
        _admin = credentials(ADMIN_ID, "管理员");
        _teacher = credentials(data.teacherUserId, "教师");
        _student = credentials(data.rows.get(0).getUserId(), "学生");
        _adminClient = new StudentClientSrv(_admin);
        _teacherClient = new StudentClientSrv(_teacher);
        _studentClient = new StudentClientSrv(_student);
    }

    private void testAdminQueriesAndCohorts() throws Exception {
        List<Student> rows = _adminClient.findAll();
        require(containsAll(rows, _data.studentIds), "管理员列表缺少测试学生");
        require(_adminClient.findByStudentId(_data.rows.get(0).getStudentId()) != null,
                "管理员按学号查询失败");
        require(_data.rows.get(2).getStudentId().equals(
                        _adminClient.findByCampusCardNo(_data.rows.get(2).getCampusCardNo())
                                .getStudentId()),
                "管理员按一卡通号查询失败");
        require(_adminClient.findByName("重复姓名").size() == 2,
                "管理员姓名重名查询没有保留两条记录");
        require(_adminClient.findByStudentId("不存在学号") == null,
                "不存在的学号应返回空结果");

        Set<String> grades = new HashSet<>();
        for (Student row : _data.rows) grades.add(row.getGrade());
        require(grades.containsAll(List.of("2023", "2024", "2025", "2026")),
                "测试数据没有覆盖四个年级");
        require(_adminClient.loadOverview(_data.rows.get(0).getStudentId()) != null,
                "管理员读取学生跨模块摘要失败");
    }

    private void testTeacherScopeAndFocus() throws Exception {
        List<Student> rows = _teacherClient.findAll();
        require(rows.size() == 4, "教师列表应只包含两个授课班级的四名学生");
        require(containsAll(rows, _data.assignedStudentIds), "教师列表缺少授课学生");
        require(!containsAny(rows, Set.of(_data.rows.get(4).getStudentId(),
                _data.rows.get(5).getStudentId())), "教师看到了未授课学生");
        require(rows.stream().map(Student::getClassName).distinct().count() == 2,
                "教师班级范围没有保留多个班级");
        require(rows.stream().allMatch(row -> row.getUserId() == null),
                "教师响应暴露了学生关联账号");
        require(_teacherClient.findByName("重复姓名").size() == 2,
                "教师重名查询没有保留授课范围内的两名学生");
        require(_teacherClient.findByStudentId(_data.rows.get(4).getStudentId()) == null,
                "教师按学号越权查询成功");
        expectStatus(() -> _teacherClient.findByCampusCardNo(
                _data.rows.get(0).getCampusCardNo()), StudentProtocol.STATUS_FORBIDDEN,
                "教师不应按一卡通号查询");
        expectStatus(() -> _teacherClient.updateStudent(_data.rows.get(0)),
                StudentProtocol.STATUS_FORBIDDEN, "教师不应修改学籍");

        StudentCampusOverview overview = _teacherClient.loadOverview(
                _data.rows.get(0).getStudentId());
        require(overview != null, "教师无法查看授课学生摘要");
        expectStatus(() -> _teacherClient.loadOverview(_data.rows.get(4).getStudentId()),
                StudentProtocol.STATUS_FORBIDDEN, "教师越权查看未授课学生摘要");

        _teacherClient.addStudentFocus(_data.rows.get(0).getStudentId(), "成绩跟进,测试", "集成测试备注");
        List<StudentFocus> focuses = _teacherClient.listFocusedStudents();
        require(focuses.stream().anyMatch(focus ->
                        _data.rows.get(0).getStudentId().equals(focus.getStudentId())),
                "教师关注名单没有新增记录");
        expectStatus(() -> _teacherClient.addStudentFocus(_data.rows.get(4).getStudentId(), null, null),
                StudentProtocol.STATUS_FORBIDDEN, "教师关注了未授课学生");
        _teacherClient.removeStudentFocus(_data.rows.get(0).getStudentId());
        require(_teacherClient.listFocusedStudents().stream().noneMatch(focus ->
                        _data.rows.get(0).getStudentId().equals(focus.getStudentId())),
                "教师取消关注后记录仍存在");
    }

    private void testTeacherCourseNames() throws Exception {
        List<TeacherCourseEnrollment> rows =
                new CourseClientSrv().queryTeacherCourseEnrollments(_teacher);
        require(rows.stream().anyMatch(row -> "测试课程A".equals(row.getCourseName())),
                "教师课程下拉缺少测试课程A");
        require(rows.stream().anyMatch(row -> "测试课程B".equals(row.getCourseName())),
                "教师课程下拉缺少测试课程B");
        Set<String> courseAStudentIds = rows.stream()
                .filter(row -> "测试课程A".equals(row.getCourseName()))
                .map(TeacherCourseEnrollment::getStudentId)
                .filter(value -> value != null && !value.isBlank())
                .collect(java.util.stream.Collectors.toSet());
        require(courseAStudentIds.size() == 2,
                "选择课程名称后应只关联该课程的选课学生");
    }

    private void testStudentScopeAndOverview() throws Exception {
        Student self = _studentClient.getMyStudentInfo();
        require(self != null && _data.rows.get(0).getStudentId().equals(self.getStudentId()),
                "学生没有读取到本人档案");
        expectStatus(_studentClient::findAll, StudentProtocol.STATUS_FORBIDDEN,
                "学生不应读取全部学生");
        expectStatus(() -> _studentClient.findByStudentId(_data.rows.get(1).getStudentId()),
                StudentProtocol.STATUS_FORBIDDEN, "学生不应查询他人学号");
        require(_studentClient.loadOverview(self.getStudentId()) != null,
                "学生无法查看本人跨模块摘要");
        expectStatus(() -> _studentClient.loadOverview(_data.rows.get(1).getStudentId()),
                StudentProtocol.STATUS_FORBIDDEN, "学生越权查看他人摘要");
    }

    private void testAdminMutationsAndAccountLinkage() throws Exception {
        Student original = _adminClient.findByStudentId(_data.rows.get(0).getStudentId());
        Student changed = copy(original);
        changed.setStatus(StudentStatus.GRADUATED);
        Student graduated = _adminClient.updateStudent(original.getStudentId(), changed);
        require(graduated.getStatus() == StudentStatus.GRADUATED, "管理员修改学籍状态失败");
        require(User.STATUS_DISABLED.equals(new UserDAO().findByUId(original.getUserId()).getUStatus()),
                "毕业状态没有禁用学生账号");

        changed = copy(graduated);
        changed.setStatus(StudentStatus.ENROLLED);
        _adminClient.updateStudent(graduated.getStudentId(), changed);
        require(User.STATUS_NORMAL.equals(new UserDAO().findByUId(original.getUserId()).getUStatus()),
                "恢复在读没有恢复学生账号");

        Student added = new Student();
        added.setStudentId(_data.addedStudentId);
        added.setCampusCardNo(_data.addedCard);
        added.setUserId(_data.addedUserId);
        added.setName("新增测试学生");
        added.setClassName("测试新增班");
        added.setMajor("测试专业新增");
        added.setGrade("2025");
        added.setEnrollmentDate(LocalDate.of(2025, 9, 1));
        added.setStatus(StudentStatus.ENROLLED);
        Student saved = _adminClient.addStudent(added);
        _data.addedPersisted = true;
        require(saved != null && _data.addedStudentId.equals(saved.getStudentId()), "管理员新增学生失败");
        require(new UserDAO().findByUId(_data.addedUserId) != null,
                "新增学生没有自动创建账号");

        Student duplicateId = copy(added);
        duplicateId.setCampusCardNo(_data.addedCard + "-X");
        expectStatus(() -> _adminClient.addStudent(duplicateId), StudentProtocol.STATUS_CONFLICT,
                "重复学号没有被拒绝");
        Student duplicateCard = copy(added);
        duplicateCard.setStudentId(_data.conflictStudentId);
        duplicateCard.setUserId(_data.conflictUserId);
        expectStatus(() -> _adminClient.addStudent(duplicateCard), StudentProtocol.STATUS_CONFLICT,
                "重复一卡通号没有被拒绝");

        Student conflictUpdate = copy(original);
        conflictUpdate.setCampusCardNo(_data.rows.get(2).getCampusCardNo());
        expectStatus(() -> _adminClient.updateStudent(original.getStudentId(), conflictUpdate),
                StudentProtocol.STATUS_CONFLICT, "修改为重复一卡通号没有被拒绝");

        _adminClient.deleteStudent(_data.addedStudentId);
        _data.addedPersisted = false;
        require(_adminClient.findByStudentId(_data.addedStudentId) == null,
                "管理员删除学生失败");
    }

    private void testPasswordReset() throws Exception {
        Student target = _data.rows.get(1);
        Message response = new UserClientSrv().resetStudentPassword(ADMIN_ID, target.getUserId());
        require(IConstant.STATUS_SUCCESS.equals(response.getStatusCode()), "管理员重置密码失败");
        User login = new User();
        login.setUId(target.getUserId());
        login.setUPwd(PASSWORD_HASH);
        Message loginResponse = new UserClientSrv().login(login);
        require(IConstant.STATUS_SUCCESS.equals(loginResponse.getStatusCode()),
                "密码重置后学生无法使用初始密码登录");
    }

    private static User credentials(String userId, String role) {
        User user = new User();
        user.setUId(userId);
        user.setUPwd(PASSWORD_HASH);
        user.setURole(role);
        return user;
    }

    private static Student copy(Student source) {
        Student target = new Student();
        target.setStudentId(source.getStudentId());
        target.setCampusCardNo(source.getCampusCardNo());
        target.setUserId(source.getUserId());
        target.setName(source.getName());
        target.setClassName(source.getClassName());
        target.setMajor(source.getMajor());
        target.setGrade(source.getGrade());
        target.setEnrollmentDate(source.getEnrollmentDate());
        target.setStatus(source.getStatus());
        target.setVersion(source.getVersion());
        target.setUpdatedAt(source.getUpdatedAt());
        return target;
    }

    private static boolean containsAll(List<Student> rows, Set<String> ids) {
        Set<String> actual = new HashSet<>();
        rows.forEach(row -> actual.add(row.getStudentId()));
        return actual.containsAll(ids);
    }

    private static boolean containsAny(List<Student> rows, Set<String> ids) {
        return rows.stream().anyMatch(row -> ids.contains(row.getStudentId()));
    }

    private static void expectStatus(CheckedCall call, String expected, String message)
            throws Exception {
        try {
            call.run();
            throw new AssertionError(message);
        } catch (StudentClientException exception) {
            require(expected.equals(exception.getStatusCode()),
                    message + "，实际状态码：" + exception.getStatusCode());
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    @FunctionalInterface
    private interface CheckedCall {
        void run() throws Exception;
    }

    private static final class TestData {
        private final String token;
        private final String teacherUserId;
        private final String teacherName;
        private final List<Student> rows = new ArrayList<>();
        private final Set<String> studentIds = new HashSet<>();
        private final Set<String> assignedStudentIds = new HashSet<>();
        private final List<String> userIds = new ArrayList<>();
        private final String courseIdA;
        private final String courseIdB;
        private final String teachingClassIdA;
        private final String teachingClassIdB;
        private final String addedStudentId;
        private final String addedCard;
        private final String addedUserId;
        private final String conflictStudentId;
        private final String conflictUserId;
        private boolean addedPersisted;

        private TestData(String token) {
            this.token = token;
            teacherUserId = "T" + token;
            teacherName = "测试教师" + token.substring(2);
            courseIdA = "ZTC" + token + "A";
            courseIdB = "ZTC" + token + "B";
            teachingClassIdA = "ZCLASS" + token + "A";
            teachingClassIdB = "ZCLASS" + token + "B";
            addedStudentId = "Z" + token + "9";
            addedCard = "ZCARD" + token + "9";
            addedUserId = "A" + token;
            conflictStudentId = "Z" + token + "8";
            conflictUserId = "B" + token;
            addRow("2023", "测试班A", "测试专业A", StudentStatus.ENROLLED, "重复姓名");
            addRow("2023", "测试班A", "测试专业A", StudentStatus.SUSPENDED, "重复姓名");
            addRow("2024", "测试班B", "测试专业B", StudentStatus.GRADUATED, "测试毕业");
            addRow("2025", "测试班B", "测试专业B", StudentStatus.WITHDRAWN, "测试退学");
            addRow("2026", "未授课班", "测试专业C", StudentStatus.ENROLLED, "未授课学生");
            addRow("2024", "", "", StudentStatus.ENROLLED, "边界学生");
            for (int index = 0; index < 4; index++) assignedStudentIds.add(rows.get(index).getStudentId());
        }

        private static TestData create() {
            long value = Math.abs(System.currentTimeMillis() % 1_000_000L);
            return new TestData(String.format("%07d", value));
        }

        private void addRow(String grade, String className, String major,
                            StudentStatus status, String name) {
            int index = rows.size();
            String suffix = token + String.valueOf(index);
            Student student = new Student();
            student.setStudentId("Z" + String.format("%09d", Long.parseLong(suffix)));
            student.setCampusCardNo("ZCARD" + suffix);
            student.setUserId("Y" + token.substring(0, 6) + index);
            student.setName(name);
            student.setClassName(className);
            student.setMajor(major);
            student.setGrade(grade);
            student.setEnrollmentDate(LocalDate.of(Integer.parseInt(grade), 9, 1));
            student.setStatus(status);
            rows.add(student);
            studentIds.add(student.getStudentId());
            userIds.add(student.getUserId());
        }

        private void insert() throws Exception {
            insertUser(teacherUserId, teacherName, "教师");
            for (Student row : rows) insertUser(row.getUserId(), row.getName(), "学生");
            StudentDAO studentDAO = new StudentDAO();
            for (Student row : rows) require(studentDAO.insert(row), "插入测试学生失败");
            try (Connection connection = DbHelper.getConnection()) {
                execute(connection, "INSERT INTO tblCourse "
                        + "(courseId, courseName, teacher, credit, capacity, selectedCount) "
                        + "VALUES (?, ?, ?, 1, 100, 4)", courseIdA, "测试课程A", teacherName);
                execute(connection, "INSERT INTO tblCourse "
                        + "(courseId, courseName, teacher, credit, capacity, selectedCount) "
                        + "VALUES (?, ?, ?, 1, 100, 4)", courseIdB, "测试课程B", teacherName);
                execute(connection, "INSERT INTO tblTeachingClass "
                        + "(teachingClassId, courseId, classNumber, teacher, capacity, selectedCount) "
                        + "VALUES (?, ?, '01', ?, 100, 2)", teachingClassIdA, courseIdA, teacherName);
                execute(connection, "INSERT INTO tblTeachingClass "
                        + "(teachingClassId, courseId, classNumber, teacher, capacity, selectedCount) "
                        + "VALUES (?, ?, '01', ?, 100, 2)", teachingClassIdB, courseIdB, teacherName);
                for (int index = 0; index < 2; index++) {
                    execute(connection, "INSERT INTO tblSelectCourse "
                                    + "(selectId, studentId, teachingClassId, courseId, selectTime) "
                                    + "VALUES (?, ?, ?, ?, ?)",
                            "ZSELECT" + token + index, rows.get(index).getStudentId(),
                            teachingClassIdA, courseIdA, LocalDateTime.now());
                }
                for (int index = 2; index < 4; index++) {
                    execute(connection, "INSERT INTO tblSelectCourse "
                                    + "(selectId, studentId, teachingClassId, courseId, selectTime) "
                                    + "VALUES (?, ?, ?, ?, ?)",
                            "ZSELECT" + token + index, rows.get(index).getStudentId(),
                            teachingClassIdB, courseIdB, LocalDateTime.now());
                }
            }
        }

        private void cleanup() {
            try (Connection connection = DbHelper.getConnection()) {
                execute(connection, "DELETE FROM tblTeacherStudentFocus WHERE teacherUserId = ?",
                        teacherUserId);
                execute(connection, "DELETE FROM tblSelectCourse WHERE teachingClassId IN (?, ?)",
                        teachingClassIdA, teachingClassIdB);
                execute(connection, "DELETE FROM tblTeachingClass WHERE teachingClassId IN (?, ?)",
                        teachingClassIdA, teachingClassIdB);
                execute(connection, "DELETE FROM tblCourse WHERE courseId IN (?, ?)", courseIdA, courseIdB);
                for (String studentId : studentIds) execute(connection,
                        "DELETE FROM tblStudent WHERE studentId = ?", studentId);
                if (addedPersisted) execute(connection,
                        "DELETE FROM tblStudent WHERE studentId = ?", addedStudentId);
                for (String userId : userIds) execute(connection,
                        "DELETE FROM tblUser WHERE uId = ?", userId);
                execute(connection, "DELETE FROM tblUser WHERE uId IN (?, ?, ?)",
                        teacherUserId, addedUserId, conflictUserId);
            } catch (Exception exception) {
                System.err.println("清理学生管理集成测试数据失败：" + exception.getMessage());
            }
        }

        private void insertUser(String userId, String name, String role) throws Exception {
            try (Connection connection = DbHelper.getConnection()) {
                execute(connection, "INSERT INTO tblUser (uId, uName, uPwd, uRole, uStatus) "
                                + "VALUES (?, ?, ?, ?, '正常')", userId, name, PASSWORD_HASH, role);
            }
        }

        private static void execute(Connection connection, String sql, Object... values)
                throws SQLException {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                for (int index = 0; index < values.length; index++) {
                    statement.setObject(index + 1, values[index]);
                }
                statement.executeUpdate();
            }
        }
    }
}
