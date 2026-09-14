/*
 * SelectCourseDAO
 *
 * Version 1.0
 *
 * 2026-09-04
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.server.dao;

import vcampus.common.vo.SelectCourse;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * 选课记录表（tblSelectCourse）的数据访问类，封装选课记录的新增、删除和查询操作。
 * 对上层业务服务层屏蔽具体的 SQL 语句与数据库细节，连接统一由
 * {@link DbHelper} 提供。
 *
 * <p>本类只负责数据持久化。课程容量、重复选课提示等业务决策不属于 DAO 职责；
 * 同一学生不能重复选择同一课程由数据库 UNIQUE(studentId, courseId) 约束保证。</p>
 */
public class SelectCourseDAO {

    /** 查询选课记录时使用的公共字段列表。 */
    private static final String SELECT_FIELDS =
            "SELECT selectId, studentId, teachingClassId, courseId, selectTime "
                    + "FROM tblSelectCourse ";

    /**
     * Locks the student row for the current transaction. Enrollment changes for one
     * student must take this lock before checking duplicates or schedule conflicts.
     */
    public boolean lockStudent(Connection conn, String studentId) throws SQLException {
        try (PreparedStatement statement = conn.prepareStatement(
                "SELECT 1 FROM tblStudent WHERE studentId=? FOR UPDATE")) {
            statement.setString(1, studentId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * 插入一条选课记录。
     *
     * @param selectCourse 待插入的选课记录
     * @return 插入成功返回 {@code true}，否则返回 {@code false}
     * @throws SQLException 数据库操作异常
     * @throws IOException  数据库配置文件读取异常
     */
    public boolean insertSelectCourse(SelectCourse selectCourse) throws SQLException, IOException {
        try (Connection conn = DbHelper.getConnection()) {
            return insertSelectCourse(conn, selectCourse);
        }
    }

    /**
     * 使用调用方提供的连接插入选课记录，用于跨 DAO 事务。
     *
     * @param conn         当前事务使用的连接
     * @param selectCourse 待插入的选课记录
     * @return 插入成功返回 {@code true}，否则返回 {@code false}
     * @throws SQLException 数据库操作异常
     */
    public boolean insertSelectCourse(Connection conn, SelectCourse selectCourse) throws SQLException {
        String teachingClassId = selectCourse.getTeachingClassId();
        if (teachingClassId == null || teachingClassId.isBlank()) {
            teachingClassId = defaultTeachingClassId(conn, selectCourse.getCourseId());
            selectCourse.setTeachingClassId(teachingClassId);
        } else {
            String courseId = courseIdForTeachingClass(conn, teachingClassId);
            if (selectCourse.getCourseId() != null
                    && !selectCourse.getCourseId().equals(courseId)) {
                throw new SQLException("教学班与课程号不一致");
            }
            selectCourse.setCourseId(courseId);
        }
        String sql = "INSERT INTO tblSelectCourse "
                + "(selectId, studentId, teachingClassId, courseId, selectTime) "
                + "VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, selectCourse.getSelectId());
            pstmt.setString(2, selectCourse.getStudentId());
            pstmt.setString(3, teachingClassId);
            pstmt.setString(4, selectCourse.getCourseId());
            pstmt.setTimestamp(5, Timestamp.valueOf(selectCourse.getSelectTime()));
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 根据学号和课程号删除选课记录。
     *
     * @param studentId 学号
     * @param courseId  课程号
     * @return 删除成功返回 {@code true}；记录不存在时返回 {@code false}
     * @throws SQLException 数据库操作异常
     * @throws IOException  数据库配置文件读取异常
     */
    public boolean deleteSelectCourse(String studentId, String courseId) throws SQLException, IOException {
        try (Connection conn = DbHelper.getConnection()) {
            return deleteSelectCourse(conn, studentId, courseId);
        }
    }

    /**
     * 使用调用方提供的连接删除选课记录，用于跨 DAO 事务。
     *
     * @param conn      当前事务使用的连接
     * @param studentId 学号
     * @param courseId  课程号
     * @return 删除成功返回 {@code true}；记录不存在时返回 {@code false}
     * @throws SQLException 数据库操作异常
     */
    public boolean deleteSelectCourse(Connection conn, String studentId, String courseId)
            throws SQLException {
        String sql = "DELETE FROM tblSelectCourse WHERE studentId = ? AND courseId = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, studentId);
            pstmt.setString(2, courseId);
            return pstmt.executeUpdate() > 0;
        }
    }

    /** Deletes the student's selection of one concrete teaching class. */
    public boolean deleteByTeachingClass(Connection conn, String studentId,
                                         String teachingClassId) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement(
                "DELETE FROM tblSelectCourse WHERE studentId=? AND teachingClassId=?")) {
            pstmt.setString(1, studentId);
            pstmt.setString(2, teachingClassId);
            return pstmt.executeUpdate() == 1;
        }
    }

    /**
     * 根据学号和课程号查询唯一的选课记录。
     *
     * @param studentId 学号
     * @param courseId  课程号
     * @return 查询到的选课记录；若不存在则返回 {@code null}
     * @throws SQLException 数据库操作异常
     * @throws IOException  数据库配置文件读取异常
     */
    public SelectCourse findByStudentAndCourse(String studentId, String courseId)
            throws SQLException, IOException {
        try (Connection conn = DbHelper.getConnection()) {
            return findByStudentAndCourse(conn, studentId, courseId);
        }
    }

    /**
     * 使用调用方提供的连接查询唯一选课记录。
     *
     * @param conn      当前事务使用的连接
     * @param studentId 学号
     * @param courseId  课程号
     * @return 查询到的选课记录；若不存在则返回 {@code null}
     * @throws SQLException 数据库操作异常
     */
    public SelectCourse findByStudentAndCourse(Connection conn, String studentId, String courseId)
            throws SQLException {
        String sql = SELECT_FIELDS + "WHERE studentId = ? AND courseId = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, studentId);
            pstmt.setString(2, courseId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    /** Finds a student's selection for one concrete teaching class. */
    public SelectCourse findByStudentAndTeachingClass(Connection conn, String studentId,
                                                       String teachingClassId)
            throws SQLException {
        String sql = SELECT_FIELDS + "WHERE studentId = ? AND teachingClassId = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, studentId);
            pstmt.setString(2, teachingClassId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    /**
     * 查询某个学生的全部选课记录，按选课时间倒序排列。
     *
     * @param studentId 学号
     * @return 该学生的选课记录列表；没有记录时返回空列表
     * @throws SQLException 数据库操作异常
     * @throws IOException  数据库配置文件读取异常
     */
    public List<SelectCourse> findByStudentId(String studentId) throws SQLException, IOException {
        String sql = SELECT_FIELDS
                + "WHERE studentId = ? ORDER BY selectTime DESC, selectId DESC";

        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, studentId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return mapList(rs);
            }
        }
    }

    /**
     * 查询某门课程的全部选课记录，按选课时间倒序排列。
     *
     * @param courseId 课程号
     * @return 该课程的选课记录列表；没有记录时返回空列表
     * @throws SQLException 数据库操作异常
     * @throws IOException  数据库配置文件读取异常
     */
    public List<SelectCourse> findByCourseId(String courseId) throws SQLException, IOException {
        try (Connection conn = DbHelper.getConnection()) {
            return findByCourseId(conn, courseId);
        }
    }

    /** 使用调用方连接查询某门课程的全部选课记录。 */
    public List<SelectCourse> findByCourseId(Connection conn, String courseId)
            throws SQLException {
        String sql = SELECT_FIELDS
                + "WHERE courseId = ? ORDER BY selectTime DESC, selectId DESC";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, courseId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return mapList(rs);
            }
        }
    }

    /**
     * 将结果集中的全部行映射为 SelectCourse 列表。
     *
     * @param rs 结果集
     * @return 选课记录列表
     * @throws SQLException 读取结果集时发生异常
     */
    private List<SelectCourse> mapList(ResultSet rs) throws SQLException {
        List<SelectCourse> records = new ArrayList<>();
        while (rs.next()) {
            records.add(mapRow(rs));
        }
        return records;
    }

    /**
     * 将结果集当前行映射为 SelectCourse 对象。
     *
     * @param rs 指向当前行的结果集
     * @return 映射后的选课记录
     * @throws SQLException 读取结果集时发生异常
     */
    private SelectCourse mapRow(ResultSet rs) throws SQLException {
        SelectCourse selectCourse = new SelectCourse();
        selectCourse.setSelectId(rs.getString("selectId"));
        selectCourse.setStudentId(rs.getString("studentId"));
        selectCourse.setCourseId(rs.getString("courseId"));
        selectCourse.setTeachingClassId(rs.getString("teachingClassId"));
        Timestamp selectTime = rs.getTimestamp("selectTime");
        if (selectTime != null) {
            selectCourse.setSelectTime(selectTime.toLocalDateTime());
        }
        return selectCourse;
    }

    private String defaultTeachingClassId(Connection conn, String courseId) throws SQLException {
        try (PreparedStatement statement = conn.prepareStatement(
                "SELECT teachingClassId FROM tblTeachingClass WHERE courseId=? "
                        + "ORDER BY classNumber LIMIT 1")) {
            statement.setString(1, courseId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("课程没有可用教学班：" + courseId);
                }
                return rs.getString(1);
            }
        }
    }

    private String courseIdForTeachingClass(Connection conn, String teachingClassId)
            throws SQLException {
        try (PreparedStatement statement = conn.prepareStatement(
                "SELECT courseId FROM tblTeachingClass WHERE teachingClassId=?")) {
            statement.setString(1, teachingClassId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) throw new SQLException("教学班不存在：" + teachingClassId);
                return rs.getString(1);
            }
        }
    }
}
