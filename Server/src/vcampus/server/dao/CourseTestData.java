/*
 * CourseTestData
 *
 * Version 1.0
 *
 * 2026-09-04
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.server.dao;

import vcampus.common.vo.Course;
import vcampus.common.vo.TeachingClass;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Course 自测共用的学生夹具工具。只使用明确的测试 ID，并按外键顺序创建和清理
 * {@code tblUser}/{@code tblStudent} 数据。
 */
public final class CourseTestData {

    private CourseTestData() {
    }

    /** Creates a Course 1.0-shaped fixture plus its normalized class 01. */
    public static boolean prepareCourse(Course course) throws SQLException, IOException {
        try (Connection conn = DbHelper.getConnection()) {
            conn.setAutoCommit(false);
            try {
                boolean inserted = new CourseDAO().insertCourse(conn, course);
                TeachingClass teachingClass = new TeachingClass(course.getCourseId() + "-01",
                        course.getCourseId(), "01", course.getTeacher(), course.getCapacity(),
                        course.getSelectedCount(), null, "test fixture");
                new TeachingClassDAO().insert(conn, teachingClass);
                conn.commit();
                return inserted;
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /** Deletes normalized teaching classes before deleting the fixture Course. */
    public static void cleanupCourse(String courseId) throws SQLException, IOException {
        try (Connection conn = DbHelper.getConnection()) {
            for (TeachingClass teachingClass : new TeachingClassDAO()
                    .findByCourseId(conn, courseId)) {
                new TeachingClassDAO().delete(conn, teachingClass.getTeachingClassId());
            }
            new CourseDAO().deleteCourse(conn, courseId);
        }
    }

    /**
     * 准备一个可被选课外键引用的测试学生。
     *
     * @param userId    八位测试用户 ID
     * @param studentId 十位测试学号
     * @throws SQLException 数据库操作失败
     * @throws IOException  数据库配置读取失败
     */
    public static void prepareStudent(String userId, String studentId)
            throws SQLException, IOException {
        cleanupStudent(userId, studentId);
        try (Connection conn = DbHelper.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement userStmt = conn.prepareStatement(
                    "INSERT INTO tblUser (uId, uName, uAge, uSex, uPwd, uRole) "
                            + "VALUES (?, '课程测试', 20, '男', ?, '学生')");
                 PreparedStatement studentStmt = conn.prepareStatement(
                         "INSERT INTO tblStudent (studentId, campusCardNo, userId, name, "
                                 + "className, major, grade, enrollmentDate, status) "
                                 + "VALUES (?, ?, ?, '课程测试', '测试班', '计算机科学与技术', "
                                 + "'2026', '2026-09-01', '在读')")) {
                userStmt.setString(1, userId);
                userStmt.setString(2, "00000000000000000000000000000000");
                userStmt.executeUpdate();

                studentStmt.setString(1, studentId);
                studentStmt.setString(2, "CARD-" + studentId);
                studentStmt.setString(3, userId);
                studentStmt.executeUpdate();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /**
     * 按外键顺序清理一个测试学生及其测试用户。
     *
     * @param userId    八位测试用户 ID
     * @param studentId 十位测试学号
     * @throws SQLException 数据库操作失败
     * @throws IOException  数据库配置读取失败
     */
    public static void cleanupStudent(String userId, String studentId)
            throws SQLException, IOException {
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement studentStmt = conn.prepareStatement(
                     "DELETE FROM tblStudent WHERE studentId = ?");
             PreparedStatement userStmt = conn.prepareStatement(
                     "DELETE FROM tblUser WHERE uId = ?")) {
            studentStmt.setString(1, studentId);
            studentStmt.executeUpdate();
            userStmt.setString(1, userId);
            userStmt.executeUpdate();
        }
    }

    /**
     * 统计指定测试学生和用户的残留数。
     *
     * @param userId    八位测试用户 ID
     * @param studentId 十位测试学号
     * @return 两张表中的残留总数
     * @throws SQLException 数据库操作失败
     * @throws IOException  数据库配置读取失败
     */
    public static int countResidue(String userId, String studentId)
            throws SQLException, IOException {
        String sql = "SELECT (SELECT COUNT(*) FROM tblUser WHERE uId = ?) + "
                + "(SELECT COUNT(*) FROM tblStudent WHERE studentId = ?)";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            pstmt.setString(2, studentId);
            try (ResultSet rs = pstmt.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }
}
