package vcampus.server.srv;

import vcampus.common.vo.Course;
import vcampus.server.dao.CourseDAO;
import vcampus.server.dao.CourseTestData;
import vcampus.server.dao.DbHelper;
import vcampus.server.dao.SelectCourseDAO;

import java.sql.Connection;
import java.sql.PreparedStatement;

/** Transaction-level verification of CHOOSE_ONE enrollment rules. */
public class CourseRequirementGroupServerSrvTest {
    private static final String USER = "CRGT0913";
    private static final String STUDENT = "CRG0913001";
    private static final String FIRST = "T_CRG_FIRST";
    private static final String SECOND = "T_CRG_SECOND";
    private static final String OTHER = "T_CRG_OTHER";
    private static final String GROUP = "T_CRG_0913";

    public static void main(String[] args) throws Exception {
        CourseServerSrv service = new CourseServerSrv();
        cleanup();
        try {
            CourseTestData.prepareStudent(USER, STUDENT);
            prepareCourse(FIRST, "等价课程甲");
            prepareCourse(SECOND, "等价课程乙");
            prepareCourse(OTHER, "其它课程");
            prepareGroup();

            require(service.selectCourse(STUDENT, FIRST), "同组第一门可选");
            expectFailure(() -> service.selectCourse(STUDENT, SECOND), "已选择同组课程");
            require(new SelectCourseDAO().findByStudentId(STUDENT).size() == 1,
                    "拒绝后不新增记录");
            require(service.selectCourse(STUDENT, OTHER), "不同组互不影响");
            require(service.dropCourse(STUDENT, FIRST), "退掉第一门");
            require(service.selectCourse(STUDENT, SECOND), "退课后同组第二门允许");
            System.out.println("COURSE_REQUIREMENT_GROUP_SERVER_SRV_TEST=PASS");
        } finally {
            cleanup();
            require(residue() == 0, "测试数据清理完成");
            System.out.println("COURSE_REQUIREMENT_GROUP_SERVER_SRV_TEST_RESIDUE=0");
        }
    }

    private static void prepareCourse(String id, String name) throws Exception {
        Course course = new Course(id, name, "规则测试教师", 2, 2, 0);
        course.setCourseNature("任选");
        course.setOpeningUnit("测试学院");
        CourseTestData.prepareCourse(course);
    }

    private static void prepareGroup() throws Exception {
        try (Connection conn = DbHelper.getConnection()) {
            try (PreparedStatement group = conn.prepareStatement(
                    "INSERT INTO tblCourseRequirementGroup(groupId,groupName,rule) VALUES(?,?,?)")) {
                group.setString(1, GROUP);
                group.setString(2, "测试二选一");
                group.setString(3, "CHOOSE_ONE");
                group.executeUpdate();
            }
            try (PreparedStatement member = conn.prepareStatement(
                    "INSERT INTO tblCourseRequirementGroupMember(groupId,courseId) VALUES(?,?)")) {
                for (String courseId : new String[]{FIRST, SECOND}) {
                    member.setString(1, GROUP);
                    member.setString(2, courseId);
                    member.addBatch();
                }
                member.executeBatch();
            }
        }
    }

    private static void cleanup() throws Exception {
        try (Connection conn = DbHelper.getConnection()) {
            try (PreparedStatement select = conn.prepareStatement(
                    "DELETE FROM tblSelectCourse WHERE studentId=?")) {
                select.setString(1, STUDENT);
                select.executeUpdate();
            }
            try (PreparedStatement members = conn.prepareStatement(
                    "DELETE FROM tblCourseRequirementGroupMember WHERE groupId=?")) {
                members.setString(1, GROUP);
                members.executeUpdate();
            }
            try (PreparedStatement group = conn.prepareStatement(
                    "DELETE FROM tblCourseRequirementGroup WHERE groupId=?")) {
                group.setString(1, GROUP);
                group.executeUpdate();
            }
        }
        for (String id : new String[]{FIRST, SECOND, OTHER}) {
            if (new CourseDAO().findById(id) != null) CourseTestData.cleanupCourse(id);
        }
        CourseTestData.cleanupStudent(USER, STUDENT);
    }

    private static int residue() throws Exception {
        try (Connection conn = DbHelper.getConnection(); PreparedStatement statement = conn.prepareStatement(
                "SELECT (SELECT COUNT(*) FROM tblCourseRequirementGroup WHERE groupId=?) + "
                        + "(SELECT COUNT(*) FROM tblCourse WHERE courseId IN (?,?,?)) + "
                        + "(SELECT COUNT(*) FROM tblSelectCourse WHERE studentId=?)")) {
            statement.setString(1, GROUP);
            statement.setString(2, FIRST);
            statement.setString(3, SECOND);
            statement.setString(4, OTHER);
            statement.setString(5, STUDENT);
            try (var rs = statement.executeQuery()) { rs.next(); return rs.getInt(1); }
        }
    }

    private static void expectFailure(Action action, String text) throws Exception {
        try {
            action.run();
            throw new IllegalStateException("FAIL: 应拒绝同组课程");
        } catch (CourseServiceException expected) {
            require(expected.getMessage().contains(text), "返回明确二选一原因");
        }
    }

    private static void require(boolean value, String text) {
        if (!value) throw new IllegalStateException("FAIL: " + text);
        System.out.println("PASS: " + text);
    }

    @FunctionalInterface private interface Action { void run() throws Exception; }
}
