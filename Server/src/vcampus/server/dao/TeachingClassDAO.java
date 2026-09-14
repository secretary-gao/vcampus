/*
 * TeachingClassDAO
 *
 * Version 1.0
 *
 * 2026-09-09
 */
package vcampus.server.dao;

import vcampus.common.vo.TeachingClass;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Persistence operations for concrete teaching classes. */
public class TeachingClassDAO {

    private static final String FIELDS = "SELECT teachingClassId, courseId, classNumber, "
            + "teacher, capacity, selectedCount, teachingLanguage, remark FROM tblTeachingClass ";

    public boolean insert(Connection conn, TeachingClass value) throws SQLException {
        String sql = "INSERT INTO tblTeachingClass (teachingClassId, courseId, classNumber, "
                + "teacher, capacity, selectedCount, teachingLanguage, remark) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            bind(statement, value);
            return statement.executeUpdate() == 1;
        }
    }

    public boolean insert(TeachingClass value) throws SQLException, IOException {
        try (Connection conn = DbHelper.getConnection()) {
            return insert(conn, value);
        }
    }

    public boolean update(Connection conn, TeachingClass value) throws SQLException {
        String sql = "UPDATE tblTeachingClass SET classNumber=?, teacher=?, capacity=?, "
                + "teachingLanguage=?, remark=? WHERE teachingClassId=?";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, value.getClassNumber());
            statement.setString(2, value.getTeacher());
            statement.setInt(3, value.getCapacity());
            statement.setString(4, value.getTeachingLanguage());
            statement.setString(5, value.getRemark());
            statement.setString(6, value.getTeachingClassId());
            return statement.executeUpdate() == 1;
        }
    }

    public boolean delete(Connection conn, String teachingClassId) throws SQLException {
        try (PreparedStatement statement = conn.prepareStatement(
                "DELETE FROM tblTeachingClass WHERE teachingClassId=?")) {
            statement.setString(1, teachingClassId);
            return statement.executeUpdate() == 1;
        }
    }

    public TeachingClass findById(String id) throws SQLException, IOException {
        try (Connection conn = DbHelper.getConnection()) {
            return findById(conn, id, false);
        }
    }

    public TeachingClass findById(Connection conn, String id, boolean forUpdate)
            throws SQLException {
        String sql = FIELDS + "WHERE teachingClassId=?" + (forUpdate ? " FOR UPDATE" : "");
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public List<TeachingClass> findByCourseId(String courseId) throws SQLException, IOException {
        try (Connection conn = DbHelper.getConnection()) {
            return findByCourseId(conn, courseId);
        }
    }

    public List<TeachingClass> findByCourseId(Connection conn, String courseId)
            throws SQLException {
        try (PreparedStatement statement = conn.prepareStatement(
                FIELDS + "WHERE courseId=? ORDER BY classNumber")) {
            statement.setString(1, courseId);
            try (ResultSet rs = statement.executeQuery()) {
                return list(rs);
            }
        }
    }

    public List<TeachingClass> findByKeyword(String keyword) throws SQLException, IOException {
        String normalized = keyword == null ? "" : keyword.trim();
        String sql = "SELECT tc.teachingClassId, tc.courseId, tc.classNumber, tc.teacher, "
                + "tc.capacity, tc.selectedCount, tc.teachingLanguage, tc.remark "
                + "FROM tblTeachingClass tc JOIN tblCourse c ON c.courseId=tc.courseId "
                + "WHERE ?='' OR tc.teachingClassId LIKE ? OR tc.teacher LIKE ? "
                + "OR c.courseId LIKE ? OR c.courseName LIKE ? "
                + "ORDER BY c.courseId, tc.classNumber";
        String like = "%" + normalized + "%";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, normalized);
            for (int index = 2; index <= 5; index++) {
                statement.setString(index, like);
            }
            try (ResultSet rs = statement.executeQuery()) {
                return list(rs);
            }
        }
    }

    public int countSelections(Connection conn, String teachingClassId) throws SQLException {
        try (PreparedStatement statement = conn.prepareStatement(
                "SELECT COUNT(*) FROM tblSelectCourse WHERE teachingClassId=?")) {
            statement.setString(1, teachingClassId);
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public int countSchedules(Connection conn, String teachingClassId) throws SQLException {
        try (PreparedStatement statement = conn.prepareStatement(
                "SELECT COUNT(*) FROM tblCourseSchedule WHERE teachingClassId=?")) {
            statement.setString(1, teachingClassId);
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public boolean incrementSelected(Connection conn, String id) throws SQLException {
        try (PreparedStatement statement = conn.prepareStatement(
                "UPDATE tblTeachingClass SET selectedCount=selectedCount+1 "
                        + "WHERE teachingClassId=? AND selectedCount < capacity")) {
            statement.setString(1, id);
            return statement.executeUpdate() == 1;
        }
    }

    public boolean decrementSelected(Connection conn, String id) throws SQLException {
        try (PreparedStatement statement = conn.prepareStatement(
                "UPDATE tblTeachingClass SET selectedCount=selectedCount-1 "
                        + "WHERE teachingClassId=? AND selectedCount > 0")) {
            statement.setString(1, id);
            return statement.executeUpdate() == 1;
        }
    }

    private void bind(PreparedStatement statement, TeachingClass value) throws SQLException {
        statement.setString(1, value.getTeachingClassId());
        statement.setString(2, value.getCourseId());
        statement.setString(3, value.getClassNumber());
        statement.setString(4, value.getTeacher());
        statement.setInt(5, value.getCapacity());
        statement.setInt(6, value.getSelectedCount());
        statement.setString(7, value.getTeachingLanguage());
        statement.setString(8, value.getRemark());
    }

    private List<TeachingClass> list(ResultSet rs) throws SQLException {
        List<TeachingClass> result = new ArrayList<>();
        while (rs.next()) {
            result.add(map(rs));
        }
        return result;
    }

    private TeachingClass map(ResultSet rs) throws SQLException {
        return new TeachingClass(rs.getString("teachingClassId"), rs.getString("courseId"),
                rs.getString("classNumber"), rs.getString("teacher"),
                rs.getInt("capacity"), rs.getInt("selectedCount"),
                rs.getString("teachingLanguage"), rs.getString("remark"));
    }
}
