/*
 * TeacherCourseEnrollmentDAO
 *
 * Version 1.0
 *
 * 2026-09-07
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.server.dao;

import vcampus.common.vo.TeacherCourseEnrollment;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/** 教师按姓名查询本人课程及选课学生名单的数据访问类。 */
public class TeacherCourseEnrollmentDAO {

    /** 查询教师所授全部课程，课程无人选时仍返回一行空学生记录。 */
    public List<TeacherCourseEnrollment> findByTeacher(String teacher)
            throws SQLException, IOException {
        String sql = "SELECT c.courseId, c.courseName, tc.teachingClassId, tc.classNumber, "
                + "s.studentId, s.name, s.className, s.major, sc.selectTime "
                + "FROM tblTeachingClass tc JOIN tblCourse c ON c.courseId=tc.courseId "
                + "LEFT JOIN tblSelectCourse sc ON sc.teachingClassId=tc.teachingClassId "
                + "LEFT JOIN tblStudent s ON s.studentId = sc.studentId "
                + "WHERE BINARY tc.teacher = BINARY ? "
                + "ORDER BY c.courseId, tc.classNumber, s.studentId";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, teacher);
            try (ResultSet rs = pstmt.executeQuery()) {
                List<TeacherCourseEnrollment> rows = new ArrayList<>();
                while (rs.next()) {
                    Timestamp selectTime = rs.getTimestamp("selectTime");
                    TeacherCourseEnrollment row = new TeacherCourseEnrollment(
                            rs.getString("courseId"), rs.getString("courseName"),
                            rs.getString("studentId"), rs.getString("name"),
                            rs.getString("className"), rs.getString("major"),
                            selectTime == null ? null : selectTime.toLocalDateTime());
                    row.setTeachingClassId(rs.getString("teachingClassId"));
                    row.setClassNumber(rs.getString("classNumber"));
                    rows.add(row);
                }
                return rows;
            }
        }
    }
}
