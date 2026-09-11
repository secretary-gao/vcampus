package vcampus.server.dao;

import java.sql.ResultSet;
import java.sql.Statement;

/** Read-only verification for Course 1.0 selection migration and global consistency. */
public class SelectionMigrationTest {
    public static void main(String[] args) throws Exception {
        try (var conn = DbHelper.getConnection(); Statement statement = conn.createStatement()) {
            require(count(statement, "SELECT COUNT(*) FROM tblSelectCourse sc "
                    + "LEFT JOIN tblTeachingClass tc ON tc.teachingClassId=sc.teachingClassId "
                    + "LEFT JOIN tblStudent s ON s.studentId=sc.studentId "
                    + "WHERE tc.teachingClassId IS NULL OR s.studentId IS NULL "
                    + "OR tc.courseId<>sc.courseId") == 0, "选课外键与 Course 一致");
            require(count(statement, "SELECT COUNT(*) FROM (SELECT studentId,courseId,COUNT(*) n "
                    + "FROM tblSelectCourse GROUP BY studentId,courseId HAVING n>1) d") == 0,
                    "无同学生同 Course 多教学班记录");
            require(count(statement, "SELECT COUNT(*) FROM tblTeachingClass tc "
                    + "WHERE tc.selectedCount<>(SELECT COUNT(*) FROM tblSelectCourse sc "
                    + "WHERE sc.teachingClassId=tc.teachingClassId)") == 0,
                    "教学班 selectedCount 与选课记录一致");
            require(count(statement, "SELECT COUNT(*) FROM tblTeachingClass "
                    + "WHERE selectedCount>capacity") == 0, "无教学班超容量");
            require(count(statement, "SELECT COUNT(*) FROM ("
                    + "SELECT DISTINCT a.studentId,a.teachingClassId classA,"
                    + "b.teachingClassId classB FROM tblSelectCourse a "
                    + "JOIN tblSelectCourse b ON b.studentId=a.studentId "
                    + "AND a.teachingClassId<b.teachingClassId "
                    + "JOIN tblCourseSchedule sa ON sa.teachingClassId=a.teachingClassId "
                    + "JOIN tblCourseSchedule sb ON sb.teachingClassId=b.teachingClassId "
                    + "WHERE sa.dayOfWeek=sb.dayOfWeek "
                    + "AND sa.weekStart<=sb.weekEnd AND sa.weekEnd>=sb.weekStart "
                    + "AND sa.startPeriod<=sb.endPeriod "
                    + "AND sa.endPeriod>=sb.startPeriod) conflicts") == 0,
                    "无学生已选教学班时间冲突");
            require(count(statement, "SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS "
                    + "WHERE CONSTRAINT_SCHEMA=DATABASE() AND TABLE_NAME='tblSelectCourse' "
                    + "AND CONSTRAINT_NAME='fk_tblSelectCourse_class_course'") == 1,
                    "数据库约束保证选课的教学班与 Course 一致");
        }
        System.out.println("SELECTION_MIGRATION_TEST=PASS");
        System.out.println("SELECTION_MIGRATION_TEST_RESIDUE=0");
    }

    private static int count(Statement statement, String sql) throws Exception {
        try (ResultSet rs = statement.executeQuery(sql)) { rs.next(); return rs.getInt(1); }
    }

    private static void require(boolean value, String message) {
        if (!value) throw new IllegalStateException("FAIL: " + message);
        System.out.println("PASS: " + message);
    }
}
