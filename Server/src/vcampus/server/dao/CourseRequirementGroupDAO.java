package vcampus.server.dao;

import vcampus.common.vo.CourseRequirementGroup;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Persistence and enrollment checks for equivalent-course groups. */
public class CourseRequirementGroupDAO {

    /** Returns all groups with their member course IDs. */
    public List<CourseRequirementGroup> findAll() throws SQLException, IOException {
        String sql = "SELECT g.groupId,g.groupName,g.rule,m.courseId "
                + "FROM tblCourseRequirementGroup g "
                + "LEFT JOIN tblCourseRequirementGroupMember m ON m.groupId=g.groupId "
                + "ORDER BY g.groupId,m.courseId";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement statement = conn.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            Map<String, CourseRequirementGroup> groups = new LinkedHashMap<>();
            while (rs.next()) {
                CourseRequirementGroup group = groups.computeIfAbsent(rs.getString("groupId"),
                        ignored -> new CourseRequirementGroup(rsValue(rs, "groupId"),
                                rsValue(rs, "groupName"), rsValue(rs, "rule"),
                                new ArrayList<>()));
                String courseId = rs.getString("courseId");
                if (courseId != null) group.getCourseIds().add(courseId);
            }
            return new ArrayList<>(groups.values());
        }
    }

    /** Returns the already selected equivalent course that blocks the candidate. */
    public BlockingSelection findBlockingSelection(Connection conn, String studentId,
                                                    String candidateCourseId)
            throws SQLException {
        String sql = "SELECT g.groupName,c.courseName FROM tblCourseRequirementGroupMember candidate "
                + "JOIN tblCourseRequirementGroup g ON g.groupId=candidate.groupId "
                + "JOIN tblCourseRequirementGroupMember chosen ON chosen.groupId=g.groupId "
                + "JOIN tblSelectCourse sc ON sc.courseId=chosen.courseId AND sc.studentId=? "
                + "JOIN tblCourse c ON c.courseId=sc.courseId "
                + "WHERE candidate.courseId=? AND chosen.courseId<>candidate.courseId "
                + "AND g.rule=? LIMIT 1";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, studentId);
            statement.setString(2, candidateCourseId);
            statement.setString(3, CourseRequirementGroup.CHOOSE_ONE);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? new BlockingSelection(
                        rs.getString("groupName"), rs.getString("courseName")) : null;
            }
        }
    }

    private static String rsValue(ResultSet rs, String column) {
        try {
            return rs.getString(column);
        } catch (SQLException exception) {
            throw new IllegalStateException(exception);
        }
    }

    public record BlockingSelection(String groupName, String selectedCourseName) { }
}
