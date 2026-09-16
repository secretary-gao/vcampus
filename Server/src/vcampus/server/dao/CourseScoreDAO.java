package vcampus.server.dao;

import vcampus.common.vo.CourseScore;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/** Persistence for the Course score approval workflow. */
public class CourseScoreDAO {
    private static final String COLUMNS = "cs.scoreId,cs.studentId,s.name studentName,cs.courseId,"
            + "c.courseName,cs.teachingClassId,cs.teacher,cs.score,cs.status,"
            + "cs.submittedAt,cs.reviewedAt ";
    private static final String JOIN = " FROM tblCourseScore cs JOIN tblStudent s ON s.studentId=cs.studentId "
            + "JOIN tblCourse c ON c.courseId=cs.courseId ";

    public List<CourseScore> findByStudent(String studentId, boolean approvedOnly)
            throws SQLException, IOException {
        String sql = "SELECT " + COLUMNS + JOIN + "WHERE cs.studentId=? "
                + (approvedOnly ? "AND cs.status='APPROVED' " : "")
                + "ORDER BY cs.submittedAt DESC";
        try (Connection conn = DbHelper.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, studentId);
            try (ResultSet rs = ps.executeQuery()) { return readAll(rs); }
        }
    }

    public List<CourseScore> findByTeacher(String teacher) throws SQLException, IOException {
        String sql = "SELECT " + COLUMNS + JOIN + "WHERE cs.teacher=? ORDER BY cs.teachingClassId,cs.studentId";
        try (Connection conn = DbHelper.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, teacher);
            try (ResultSet rs = ps.executeQuery()) { return readAll(rs); }
        }
    }

    public List<CourseScore> findPending() throws SQLException, IOException {
        String sql = "SELECT " + COLUMNS + JOIN + "WHERE cs.status='PENDING' ORDER BY cs.submittedAt";
        try (Connection conn = DbHelper.getConnection(); PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) { return readAll(rs); }
    }

    public boolean upsert(Connection conn, CourseScore value) throws SQLException {
        String sql = "INSERT INTO tblCourseScore(scoreId,studentId,courseId,teachingClassId,teacher,score,status,submittedAt) "
                + "VALUES(?,?,?,?,?,?,'PENDING',NOW()) ON DUPLICATE KEY UPDATE teacher=VALUES(teacher),score=VALUES(score),status='PENDING',submittedAt=NOW(),reviewedAt=NULL";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, value.getScoreId()); ps.setString(2, value.getStudentId());
            ps.setString(3, value.getCourseId()); ps.setString(4, value.getTeachingClassId());
            ps.setString(5, value.getTeacher()); ps.setInt(6, value.getScore());
            return ps.executeUpdate() > 0;
        }
    }

    public CourseScore findById(Connection conn, String scoreId) throws SQLException {
        String sql = "SELECT " + COLUMNS + JOIN + "WHERE cs.scoreId=? FOR UPDATE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, scoreId); try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? read(rs) : null;
            }
        }
    }

    public boolean review(Connection conn, String scoreId, String status) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE tblCourseScore SET status=?,reviewedAt=NOW() WHERE scoreId=?")) {
            ps.setString(1, status); ps.setString(2, scoreId); return ps.executeUpdate() == 1;
        }
    }

    private List<CourseScore> readAll(ResultSet rs) throws SQLException {
        List<CourseScore> result = new ArrayList<>(); while (rs.next()) result.add(read(rs)); return result;
    }
    private CourseScore read(ResultSet rs) throws SQLException {
        CourseScore v = new CourseScore();
        v.setScoreId(rs.getString("scoreId")); v.setStudentId(rs.getString("studentId"));
        v.setStudentName(rs.getString("studentName")); v.setCourseId(rs.getString("courseId"));
        v.setCourseName(rs.getString("courseName")); v.setTeachingClassId(rs.getString("teachingClassId"));
        v.setTeacher(rs.getString("teacher")); v.setScore(rs.getInt("score")); v.setStatus(rs.getString("status"));
        Timestamp submitted = rs.getTimestamp("submittedAt"), reviewed = rs.getTimestamp("reviewedAt");
        v.setSubmittedAt(submitted == null ? null : submitted.toLocalDateTime());
        v.setReviewedAt(reviewed == null ? null : reviewed.toLocalDateTime()); return v;
    }
}
