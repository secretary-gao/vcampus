package vcampus.server.dao;

import java.sql.ResultSet;
import java.sql.Statement;

/** Read-only verification for Course 1.0 schedule migration. */
public class ScheduleMigrationTest {
    public static void main(String[] args) throws Exception {
        try (var conn = DbHelper.getConnection(); Statement statement = conn.createStatement()) {
            require(count(statement, "SELECT COUNT(*) FROM tblCourseSchedule s "
                    + "LEFT JOIN tblTeachingClass tc ON tc.teachingClassId=s.teachingClassId "
                    + "WHERE tc.teachingClassId IS NULL OR tc.courseId<>s.courseId") == 0,
                    "全部排课关联有效且 Course 一致");
            require(count(statement, "SELECT COUNT(*) FROM tblCourseSchedule "
                    + "WHERE weekStart<1 OR weekEnd<weekStart OR startPeriod<1 "
                    + "OR endPeriod<startPeriod") == 0, "周次与节次迁移值有效");
            require(count(statement, "SELECT COUNT(*) FROM tblCourseSchedule "
                    + "WHERE scheduleId LIKE 'DEMO-%' AND "
                    + "(teachingClassId IS NULL OR weekStart<>1 OR weekEnd<>16)") == 0,
                    "Course 1.0 demo 排课已映射到默认教学班");
            require(count(statement, "SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS "
                    + "WHERE CONSTRAINT_SCHEMA=DATABASE() AND TABLE_NAME='tblCourseSchedule' "
                    + "AND CONSTRAINT_NAME='fk_tblCourseSchedule_class_course'") == 1,
                    "数据库约束保证排课的教学班与 Course 一致");
        }
        System.out.println("SCHEDULE_MIGRATION_TEST=PASS");
        System.out.println("SCHEDULE_MIGRATION_TEST_RESIDUE=0");
    }

    private static int count(Statement statement, String sql) throws Exception {
        try (ResultSet rs = statement.executeQuery(sql)) { rs.next(); return rs.getInt(1); }
    }

    private static void require(boolean value, String message) {
        if (!value) throw new IllegalStateException("FAIL: " + message);
        System.out.println("PASS: " + message);
    }
}
