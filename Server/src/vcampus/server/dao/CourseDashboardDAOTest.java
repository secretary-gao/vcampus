package vcampus.server.dao;

import vcampus.common.vo.CourseDashboardStats;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/** Verifies dashboard aggregates against independent SQL. */
public class CourseDashboardDAOTest {
    public static void main(String[] args) throws Exception {
        CourseDashboardStats stats = new CourseDashboardDAO().load();
        try (Connection conn = DbHelper.getConnection(); PreparedStatement statement =
                     conn.prepareStatement("SELECT (SELECT COUNT(*) FROM tblCourse),"
                             + "(SELECT COUNT(*) FROM tblTeachingClass),"
                             + "(SELECT COUNT(*) FROM tblSelectCourse),"
                             + "(SELECT COUNT(DISTINCT studentId) FROM tblSelectCourse),"
                             + "(SELECT COALESCE(SUM(selectedCount),0) FROM tblTeachingClass),"
                             + "(SELECT COALESCE(SUM(capacity),0) FROM tblTeachingClass)");
             ResultSet rs = statement.executeQuery()) {
            rs.next();
            require(stats.getCourseCount() == rs.getInt(1), "课程数与 SQL 一致");
            require(stats.getTeachingClassCount() == rs.getInt(2), "教学班数与 SQL 一致");
            require(stats.getSelectionCount() == rs.getInt(3), "选课记录数与 SQL 一致");
            require(stats.getStudentCount() == rs.getInt(4), "选课学生数与 SQL 一致");
            double expected = rs.getInt(6) == 0 ? 0 : (double) rs.getInt(5) / rs.getInt(6);
            require(Math.abs(stats.getCapacityUtilization() - expected) < 0.0001,
                    "容量利用率与 SQL 一致");
            require(stats.getAllClasses().size() == stats.getTeachingClassCount(),
                    "导出明细覆盖全部教学班");
        }
        System.out.println("COURSE_DASHBOARD_DAO_TEST=PASS");
    }

    private static void require(boolean value, String message) {
        if (!value) throw new IllegalStateException("FAIL: " + message);
        System.out.println("PASS: " + message);
    }
}
