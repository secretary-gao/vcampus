package vcampus.server.dao;

import vcampus.common.vo.Student;
import vcampus.common.vo.StudentStatus;

import java.io.IOException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** 封装 tblStudent 表的增、删、改、查操作。 */
public class StudentDAO {

    private static final String SELECT_COLUMNS = "studentId, campusCardNo, userId, name, "
            + "className, major, grade, enrollmentDate, status, version, updatedAt";

    /** 查询全部学生，供界面刷新表格使用。 */
    public List<Student> findAll() throws SQLException, IOException {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM tblStudent ORDER BY studentId";
        List<Student> students = new ArrayList<>();
        try (Connection connection = DbHelper.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                students.add(mapRow(resultSet));
            }
        }
        return students;
    }

    /** 按学号精确查询；未找到时返回 null。 */
    public Student findByStudentId(String studentId) throws SQLException, IOException {
        String sql = "SELECT " + SELECT_COLUMNS
                + " FROM tblStudent WHERE studentId = ?";
        return findOne(sql, studentId);
    }

    /** 按一卡通号精确查询；未找到时返回 null。 */
    public Student findByCampusCardNo(String campusCardNo) throws SQLException, IOException {
        String sql = "SELECT " + SELECT_COLUMNS
                + " FROM tblStudent WHERE campusCardNo = ?";
        return findOne(sql, campusCardNo);
    }

    /** 按姓名精确查询。姓名可能重复，因此返回列表。 */
    public List<Student> findByName(String name) throws SQLException, IOException {
        String sql = "SELECT " + SELECT_COLUMNS
                + " FROM tblStudent WHERE name = ? ORDER BY studentId";
        List<Student> students = new ArrayList<>();
        try (Connection connection = DbHelper.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    students.add(mapRow(resultSet));
                }
            }
        }
        return students;
    }

    /** 检查学生关联的用户账号是否存在。 */
    public boolean userExists(String userId) throws SQLException, IOException {
        String sql = "SELECT 1 FROM tblUser WHERE uId = ?";
        try (Connection connection = DbHelper.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    /** 新增学生，数据库自动生成 version 和 updatedAt。 */
    public boolean insert(Student student) throws SQLException, IOException {
        String sql = "INSERT INTO tblStudent "
                + "(studentId, campusCardNo, userId, name, className, major, grade, "
                + "enrollmentDate, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = DbHelper.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindInsertValues(statement, student);
            return statement.executeUpdate() == 1;
        }
    }

    /** 修改学生并递增版本号。记录不存在或版本已变化时返回 false。 */
    public boolean update(Student student) throws SQLException, IOException {
        String sql = "UPDATE tblStudent SET campusCardNo = ?, userId = ?, name = ?, "
                + "className = ?, major = ?, grade = ?, enrollmentDate = ?, status = ?, "
                + "version = version + 1 WHERE studentId = ? AND version = ?";
        try (Connection connection = DbHelper.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindUpdateValues(statement, student);
            int affectedRows = statement.executeUpdate();
            if (affectedRows == 1) {
                student.setVersion(student.getVersion() + 1);
                return true;
            }
            return false;
        }
    }

    /** 按学号删除学生。 */
    public boolean deleteByStudentId(String studentId) throws SQLException, IOException {
        String sql = "DELETE FROM tblStudent WHERE studentId = ?";
        try (Connection connection = DbHelper.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, studentId);
            return statement.executeUpdate() == 1;
        }
    }

    private Student findOne(String sql, String value) throws SQLException, IOException {
        try (Connection connection = DbHelper.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, value);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? mapRow(resultSet) : null;
            }
        }
    }

    private static void bindInsertValues(PreparedStatement statement, Student student)
            throws SQLException {
        statement.setString(1, student.getStudentId());
        statement.setString(2, student.getCampusCardNo());
        statement.setString(3, student.getUserId());
        statement.setString(4, student.getName());
        statement.setString(5, student.getClassName());
        statement.setString(6, student.getMajor());
        statement.setString(7, student.getGrade());
        setLocalDate(statement, 8, student.getEnrollmentDate());
        statement.setString(9, statusValue(student.getStatus()));
    }

    private static void bindUpdateValues(PreparedStatement statement, Student student)
            throws SQLException {
        statement.setString(1, student.getCampusCardNo());
        statement.setString(2, student.getUserId());
        statement.setString(3, student.getName());
        statement.setString(4, student.getClassName());
        statement.setString(5, student.getMajor());
        statement.setString(6, student.getGrade());
        setLocalDate(statement, 7, student.getEnrollmentDate());
        statement.setString(8, statusValue(student.getStatus()));
        statement.setString(9, student.getStudentId());
        statement.setLong(10, student.getVersion());
    }

    private static String statusValue(StudentStatus status) {
        return (status == null ? StudentStatus.ENROLLED : status).getDatabaseValue();
    }

    private static void setLocalDate(PreparedStatement statement, int index, LocalDate value)
            throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.DATE);
        } else {
            statement.setDate(index, Date.valueOf(value));
        }
    }

    private static Student mapRow(ResultSet resultSet) throws SQLException {
        Student student = new Student();
        student.setStudentId(resultSet.getString("studentId"));
        student.setCampusCardNo(resultSet.getString("campusCardNo"));
        student.setUserId(resultSet.getString("userId"));
        student.setName(resultSet.getString("name"));
        student.setClassName(resultSet.getString("className"));
        student.setMajor(resultSet.getString("major"));
        student.setGrade(resultSet.getString("grade"));

        Date enrollmentDate = resultSet.getDate("enrollmentDate");
        if (enrollmentDate != null) {
            student.setEnrollmentDate(enrollmentDate.toLocalDate());
        }

        try {
            student.setStatus(StudentStatus.fromDatabaseValue(resultSet.getString("status")));
        } catch (IllegalArgumentException exception) {
            throw new SQLException(exception.getMessage(), exception);
        }
        student.setVersion(resultSet.getLong("version"));

        Timestamp updatedAt = resultSet.getTimestamp("updatedAt");
        if (updatedAt != null) {
            student.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        return student;
    }
}
