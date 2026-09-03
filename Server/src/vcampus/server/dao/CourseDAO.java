/*
 * CourseDAO
 *
 * Version 1.0
 *
 * 2026-09-03
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.server.dao;

import vcampus.common.vo.Course;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 课程表（tblCourse）的数据访问类，封装课程的新增、删除、修改和查询操作。
 * 对上层业务服务层屏蔽具体的 SQL 语句与数据库细节，连接统一由
 * {@link DbHelper} 提供。
 */
public class CourseDAO {

    /**
     * 插入一门新课程。
     *
     * @param course 待插入的课程对象
     * @return 插入成功返回 {@code true}，否则返回 {@code false}
     * @throws SQLException 数据库操作异常
     * @throws IOException  数据库配置文件读取异常
     */
    public boolean insertCourse(Course course) throws SQLException, IOException {
        String sql = "INSERT INTO tblCourse "
                + "(courseId, courseName, teacher, credit, capacity, selectedCount) "
                + "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, course.getCourseId());
            pstmt.setString(2, course.getCourseName());
            pstmt.setString(3, course.getTeacher());
            pstmt.setInt(4, course.getCredit());
            pstmt.setInt(5, course.getCapacity());
            pstmt.setInt(6, course.getSelectedCount());
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 根据课程号删除课程。
     *
     * @param courseId 课程号
     * @return 删除成功返回 {@code true}；课程不存在时返回 {@code false}
     * @throws SQLException 数据库操作异常
     * @throws IOException  数据库配置文件读取异常
     */
    public boolean deleteCourse(String courseId) throws SQLException, IOException {
        String sql = "DELETE FROM tblCourse WHERE courseId = ?";

        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, courseId);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 修改课程信息，课程号本身不可修改。
     *
     * @param course 待修改的课程对象（以 courseId 定位）
     * @return 修改成功返回 {@code true}；课程不存在时返回 {@code false}
     * @throws SQLException 数据库操作异常
     * @throws IOException  数据库配置文件读取异常
     */
    public boolean updateCourse(Course course) throws SQLException, IOException {
        String sql = "UPDATE tblCourse SET courseName = ?, teacher = ?, credit = ?, "
                + "capacity = ?, selectedCount = ? WHERE courseId = ?";

        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, course.getCourseName());
            pstmt.setString(2, course.getTeacher());
            pstmt.setInt(3, course.getCredit());
            pstmt.setInt(4, course.getCapacity());
            pstmt.setInt(5, course.getSelectedCount());
            pstmt.setString(6, course.getCourseId());
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 根据课程号查询课程。
     *
     * @param courseId 课程号
     * @return 查询到的课程对象；若不存在则返回 {@code null}
     * @throws SQLException 数据库操作异常
     * @throws IOException  数据库配置文件读取异常
     */
    public Course findById(String courseId) throws SQLException, IOException {
        String sql = "SELECT courseId, courseName, teacher, credit, capacity, selectedCount "
                + "FROM tblCourse WHERE courseId = ?";

        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, courseId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    /**
     * 查询全部课程，按课程号升序排列。
     *
     * @return 全部课程组成的列表；没有课程时返回空列表
     * @throws SQLException 数据库操作异常
     * @throws IOException  数据库配置文件读取异常
     */
    public List<Course> findAll() throws SQLException, IOException {
        String sql = "SELECT courseId, courseName, teacher, credit, capacity, selectedCount "
                + "FROM tblCourse ORDER BY courseId";

        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            List<Course> courses = new ArrayList<>();
            while (rs.next()) {
                courses.add(mapRow(rs));
            }
            return courses;
        }
    }

    /**
     * 将结果集当前行映射为 Course 对象。
     *
     * @param rs 指向当前行的结果集
     * @return 映射后的课程对象
     * @throws SQLException 读取结果集时发生异常
     */
    private Course mapRow(ResultSet rs) throws SQLException {
        Course course = new Course();
        course.setCourseId(rs.getString("courseId"));
        course.setCourseName(rs.getString("courseName"));
        course.setTeacher(rs.getString("teacher"));
        course.setCredit(rs.getInt("credit"));
        course.setCapacity(rs.getInt("capacity"));
        course.setSelectedCount(rs.getInt("selectedCount"));
        return course;
    }
}
