/*
 * CourseScheduleDAO
 *
 * Version 1.0
 *
 * 2026-09-04
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.server.dao;

import vcampus.common.vo.CourseSchedule;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/** 课程排课表（tblCourseSchedule）的数据访问类。 */
public class CourseScheduleDAO {

    private static final String SELECT_FIELDS =
            "SELECT scheduleId, courseId, classroom, dayOfWeek, startTime, endTime "
                    + "FROM tblCourseSchedule ";

    /** 插入排课记录。 */
    public boolean insertSchedule(CourseSchedule schedule) throws SQLException, IOException {
        try (Connection conn = DbHelper.getConnection()) {
            return insertSchedule(conn, schedule);
        }
    }

    /** 使用调用方连接插入排课记录。 */
    public boolean insertSchedule(Connection conn, CourseSchedule schedule) throws SQLException {
        String sql = "INSERT INTO tblCourseSchedule "
                + "(scheduleId, courseId, classroom, dayOfWeek, startTime, endTime) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            bindSchedule(pstmt, schedule);
            return pstmt.executeUpdate() > 0;
        }
    }

    /** 修改排课记录。 */
    public boolean updateSchedule(Connection conn, CourseSchedule schedule) throws SQLException {
        String sql = "UPDATE tblCourseSchedule SET courseId = ?, classroom = ?, "
                + "dayOfWeek = ?, startTime = ?, endTime = ? WHERE scheduleId = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, schedule.getCourseId());
            pstmt.setString(2, schedule.getClassroom());
            pstmt.setInt(3, schedule.getDayOfWeek());
            pstmt.setTime(4, Time.valueOf(schedule.getStartTime()));
            pstmt.setTime(5, Time.valueOf(schedule.getEndTime()));
            pstmt.setString(6, schedule.getScheduleId());
            return pstmt.executeUpdate() > 0;
        }
    }

    /** 按记录号删除排课。 */
    public boolean deleteSchedule(String scheduleId) throws SQLException, IOException {
        String sql = "DELETE FROM tblCourseSchedule WHERE scheduleId = ?";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, scheduleId);
            return pstmt.executeUpdate() > 0;
        }
    }

    /** 按记录号查询排课。 */
    public CourseSchedule findById(String scheduleId) throws SQLException, IOException {
        try (Connection conn = DbHelper.getConnection()) {
            return findById(conn, scheduleId);
        }
    }

    /** 使用调用方连接按记录号查询排课。 */
    public CourseSchedule findById(Connection conn, String scheduleId) throws SQLException {
        String sql = SELECT_FIELDS + "WHERE scheduleId = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, scheduleId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    /** 查询全部排课。 */
    public List<CourseSchedule> findAll() throws SQLException, IOException {
        String sql = SELECT_FIELDS + "ORDER BY dayOfWeek, startTime, scheduleId";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            return mapList(rs);
        }
    }

    /** 查询指定课程集合对应的全部排课。 */
    public List<CourseSchedule> findByCourseIds(Collection<String> courseIds)
            throws SQLException, IOException {
        try (Connection conn = DbHelper.getConnection()) {
            return findByCourseIds(conn, courseIds);
        }
    }

    /** 使用调用方连接查询指定课程集合对应的全部排课。 */
    public List<CourseSchedule> findByCourseIds(Connection conn, Collection<String> courseIds)
            throws SQLException {
        if (courseIds == null || courseIds.isEmpty()) {
            return new ArrayList<>();
        }
        String placeholders = String.join(", ",
                Collections.nCopies(courseIds.size(), "?"));
        String sql = SELECT_FIELDS + "WHERE courseId IN (" + placeholders + ") "
                + "ORDER BY dayOfWeek, startTime, scheduleId";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            int index = 1;
            for (String courseId : courseIds) {
                pstmt.setString(index++, courseId);
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                return mapList(rs);
            }
        }
    }

    /** 判断同一教室是否存在时间重叠，并锁定匹配范围。 */
    public boolean hasClassroomConflict(Connection conn, CourseSchedule schedule,
                                        String excludeScheduleId) throws SQLException {
        String sql = "SELECT 1 FROM tblCourseSchedule "
                + "WHERE dayOfWeek = ? AND classroom = ? "
                + "AND startTime < ? AND endTime > ? "
                + "AND (? IS NULL OR scheduleId <> ?) LIMIT 1 FOR UPDATE";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, schedule.getDayOfWeek());
            pstmt.setString(2, schedule.getClassroom());
            pstmt.setTime(3, Time.valueOf(schedule.getEndTime()));
            pstmt.setTime(4, Time.valueOf(schedule.getStartTime()));
            pstmt.setString(5, excludeScheduleId);
            pstmt.setString(6, excludeScheduleId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** 判断同一教师是否存在时间重叠，并锁定匹配范围。 */
    public boolean hasTeacherConflict(Connection conn, CourseSchedule schedule,
                                      String teacher, String excludeScheduleId) throws SQLException {
        String sql = "SELECT 1 FROM tblCourseSchedule s "
                + "JOIN tblCourse c ON s.courseId = c.courseId "
                + "WHERE s.dayOfWeek = ? AND c.teacher = ? "
                + "AND s.startTime < ? AND s.endTime > ? "
                + "AND (? IS NULL OR s.scheduleId <> ?) LIMIT 1 FOR UPDATE";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, schedule.getDayOfWeek());
            pstmt.setString(2, teacher);
            pstmt.setTime(3, Time.valueOf(schedule.getEndTime()));
            pstmt.setTime(4, Time.valueOf(schedule.getStartTime()));
            pstmt.setString(5, excludeScheduleId);
            pstmt.setString(6, excludeScheduleId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** 绑定 INSERT 使用的排课字段。 */
    private void bindSchedule(PreparedStatement pstmt, CourseSchedule schedule) throws SQLException {
        pstmt.setString(1, schedule.getScheduleId());
        pstmt.setString(2, schedule.getCourseId());
        pstmt.setString(3, schedule.getClassroom());
        pstmt.setInt(4, schedule.getDayOfWeek());
        pstmt.setTime(5, Time.valueOf(schedule.getStartTime()));
        pstmt.setTime(6, Time.valueOf(schedule.getEndTime()));
    }

    /** 映射结果集中的全部排课。 */
    private List<CourseSchedule> mapList(ResultSet rs) throws SQLException {
        List<CourseSchedule> schedules = new ArrayList<>();
        while (rs.next()) {
            schedules.add(mapRow(rs));
        }
        return schedules;
    }

    /** 映射结果集当前行。 */
    private CourseSchedule mapRow(ResultSet rs) throws SQLException {
        CourseSchedule schedule = new CourseSchedule();
        schedule.setScheduleId(rs.getString("scheduleId"));
        schedule.setCourseId(rs.getString("courseId"));
        schedule.setClassroom(rs.getString("classroom"));
        schedule.setDayOfWeek(rs.getInt("dayOfWeek"));
        Time startTime = rs.getTime("startTime");
        Time endTime = rs.getTime("endTime");
        schedule.setStartTime(startTime == null ? null : startTime.toLocalTime());
        schedule.setEndTime(endTime == null ? null : endTime.toLocalTime());
        return schedule;
    }
}
