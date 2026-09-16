package vcampus.server.dao;

import vcampus.common.vo.StudentFocus;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** 教师关注名单数据访问。表不存在时自动创建，便于旧数据库平滑升级。 */
public class StudentFocusDAO {

    private static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS tblTeacherStudentFocus ("
            + "teacherUserId CHAR(8) NOT NULL, studentId VARCHAR(10) NOT NULL, "
            + "tags VARCHAR(200) NULL, "
            + "note VARCHAR(200) NULL, createdAt DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, "
            + "PRIMARY KEY (teacherUserId, studentId), "
            + "CONSTRAINT fk_focus_teacher FOREIGN KEY (teacherUserId) REFERENCES tblUser(uId) ON DELETE CASCADE, "
            + "CONSTRAINT fk_focus_student FOREIGN KEY (studentId) REFERENCES tblStudent(studentId) ON UPDATE CASCADE ON DELETE CASCADE"
            + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";

    private void ensureTable(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(CREATE_TABLE)) {
            statement.executeUpdate();
        }
        ensureStudentForeignKeyCascade(connection);
        String columnSql = "SELECT COUNT(*) FROM information_schema.columns "
                + "WHERE table_schema = DATABASE() AND table_name = 'tblTeacherStudentFocus' "
                + "AND column_name = 'tags'";
        try (PreparedStatement statement = connection.prepareStatement(columnSql);
             ResultSet rs = statement.executeQuery()) {
            if (rs.next() && rs.getInt(1) == 0) {
                try (PreparedStatement alter = connection.prepareStatement(
                        "ALTER TABLE tblTeacherStudentFocus ADD COLUMN tags VARCHAR(200) NULL")) {
                    alter.executeUpdate();
                }
            }
        }
    }

    /** 将旧版本关注表的学号外键升级为级联更新，支持修改学号。 */
    private void ensureStudentForeignKeyCascade(Connection connection) throws SQLException {
        String ruleSql = "SELECT rc.UPDATE_RULE FROM information_schema.REFERENTIAL_CONSTRAINTS rc "
                + "WHERE rc.CONSTRAINT_SCHEMA = DATABASE() "
                + "AND rc.TABLE_NAME = 'tblTeacherStudentFocus' "
                + "AND rc.CONSTRAINT_NAME = 'fk_focus_student'";
        String updateRule = null;
        try (PreparedStatement statement = connection.prepareStatement(ruleSql);
             ResultSet rs = statement.executeQuery()) {
            if (rs.next()) updateRule = rs.getString(1);
        }
        if (updateRule == null || "CASCADE".equalsIgnoreCase(updateRule)) return;
        try (PreparedStatement alter = connection.prepareStatement(
                "ALTER TABLE tblTeacherStudentFocus DROP FOREIGN KEY fk_focus_student")) {
            alter.executeUpdate();
        }
        try (PreparedStatement alter = connection.prepareStatement(
                "ALTER TABLE tblTeacherStudentFocus ADD CONSTRAINT fk_focus_student "
                        + "FOREIGN KEY (studentId) REFERENCES tblStudent(studentId) "
                        + "ON UPDATE CASCADE ON DELETE CASCADE")) {
            alter.executeUpdate();
        }
    }

    public List<StudentFocus> findByTeacher(String teacherUserId) throws SQLException, IOException {
        try (Connection connection = DbHelper.getConnection()) {
            ensureTable(connection);
            String sql = "SELECT teacherUserId, studentId, tags, note, createdAt "
                    + "FROM tblTeacherStudentFocus WHERE teacherUserId=? ORDER BY createdAt DESC";
            List<StudentFocus> result = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, teacherUserId);
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) result.add(map(rs));
                }
            }
            return result;
        }
    }

    public void add(String teacherUserId, String studentId, String tags, String note)
            throws SQLException, IOException {
        try (Connection connection = DbHelper.getConnection()) {
            ensureTable(connection);
            String sql = "INSERT INTO tblTeacherStudentFocus (teacherUserId, studentId, tags, note) "
                    + "VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE tags=VALUES(tags), note=VALUES(note)";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, teacherUserId);
                statement.setString(2, studentId);
                statement.setString(3, tags == null || tags.isBlank() ? null : tags.trim());
                statement.setString(4, note == null || note.isBlank() ? null : note.trim());
                statement.executeUpdate();
            }
        }
    }

    public void remove(String teacherUserId, String studentId)
            throws SQLException, IOException {
        try (Connection connection = DbHelper.getConnection()) {
            ensureTable(connection);
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM tblTeacherStudentFocus WHERE teacherUserId=? AND studentId=?")) {
                statement.setString(1, teacherUserId);
                statement.setString(2, studentId);
                statement.executeUpdate();
            }
        }
    }

    private static StudentFocus map(ResultSet rs) throws SQLException {
        Timestamp timestamp = rs.getTimestamp("createdAt");
        LocalDateTime createdAt = timestamp == null ? null : timestamp.toLocalDateTime();
        return new StudentFocus(rs.getString("teacherUserId"), rs.getString("studentId"),
                rs.getString("tags"), rs.getString("note"), createdAt);
    }
}
