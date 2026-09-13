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
            "SELECT scheduleId, teachingClassId, courseId, classroom, weekStart, weekEnd, "
                    + "dayOfWeek, startPeriod, endPeriod, startTime, endTime "
                    + "FROM tblCourseSchedule ";

    /** 插入排课记录。 */
    public boolean insertSchedule(CourseSchedule schedule) throws SQLException, IOException {
        try (Connection conn = DbHelper.getConnection()) {
            return insertSchedule(conn, schedule);
        }
    }

    /** 使用调用方连接插入排课记录。 */
    public boolean insertSchedule(Connection conn, CourseSchedule schedule) throws SQLException {
        normalizeCompatibilityFields(conn, schedule);
        String sql = "INSERT INTO tblCourseSchedule "
                + "(scheduleId, teachingClassId, courseId, classroom, weekStart, weekEnd, "
                + "dayOfWeek, startPeriod, endPeriod, startTime, endTime) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            bindSchedule(pstmt, schedule);
            return pstmt.executeUpdate() > 0;
        }
    }

    /** 修改排课记录。 */
    public boolean updateSchedule(Connection conn, CourseSchedule schedule) throws SQLException {
        normalizeCompatibilityFields(conn, schedule);
        String sql = "UPDATE tblCourseSchedule SET teachingClassId=?, courseId = ?, "
                + "classroom = ?, weekStart=?, weekEnd=?, dayOfWeek = ?, "
                + "startPeriod=?, endPeriod=?, startTime = ?, endTime = ? WHERE scheduleId = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, schedule.getTeachingClassId());
            pstmt.setString(2, schedule.getCourseId());
            pstmt.setString(3, schedule.getClassroom());
            pstmt.setInt(4, schedule.getWeekStart());
            pstmt.setInt(5, schedule.getWeekEnd());
            pstmt.setInt(6, schedule.getDayOfWeek());
            pstmt.setInt(7, schedule.getStartPeriod());
            pstmt.setInt(8, schedule.getEndPeriod());
            pstmt.setTime(9, Time.valueOf(schedule.getStartTime()));
            pstmt.setTime(10, Time.valueOf(schedule.getEndTime()));
            pstmt.setString(11, schedule.getScheduleId());
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
        try (Connection conn = DbHelper.getConnection()) {
            return findAll(conn);
        }
    }

    /** Queries all schedules using the caller's transaction connection. */
    public List<CourseSchedule> findAll(Connection conn) throws SQLException {
        String sql = SELECT_FIELDS + "ORDER BY dayOfWeek, startTime, scheduleId";
        try (PreparedStatement pstmt = conn.prepareStatement(sql);
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

    /** Queries schedules belonging to concrete teaching classes. */
    public List<CourseSchedule> findByTeachingClassIds(Collection<String> teachingClassIds)
            throws SQLException, IOException {
        if (teachingClassIds == null || teachingClassIds.isEmpty()) {
            return new ArrayList<>();
        }
        String placeholders = String.join(", ",
                Collections.nCopies(teachingClassIds.size(), "?"));
        String sql = SELECT_FIELDS + "WHERE teachingClassId IN (" + placeholders + ") "
                + "ORDER BY dayOfWeek, startPeriod, scheduleId";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {
            int index = 1;
            for (String id : teachingClassIds) {
                statement.setString(index++, id);
            }
            try (ResultSet rs = statement.executeQuery()) {
                return mapList(rs);
            }
        }
    }

    /**
     * Finds one schedule that conflicts with a student's existing selections.
     * Every candidate schedule is compared with every schedule of every selected
     * teaching class using inclusive week and period intervals.
     */
    public StudentScheduleConflict findStudentScheduleConflict(
            Connection conn, String studentId, String candidateTeachingClassId)
            throws SQLException {
        String sql = "SELECT c.courseName, existing.weekStart, existing.weekEnd, "
                + "existing.dayOfWeek, existing.startPeriod, existing.endPeriod "
                + "FROM tblCourseSchedule candidate "
                + "JOIN tblSelectCourse sc ON sc.studentId=? "
                + "JOIN tblCourseSchedule existing "
                + "ON existing.teachingClassId=sc.teachingClassId "
                + "JOIN tblCourse c ON c.courseId=sc.courseId "
                + "WHERE candidate.teachingClassId=? "
                + "AND candidate.dayOfWeek=existing.dayOfWeek "
                + "AND candidate.weekStart<=existing.weekEnd "
                + "AND candidate.weekEnd>=existing.weekStart "
                + "AND candidate.startPeriod<=existing.endPeriod "
                + "AND candidate.endPeriod>=existing.startPeriod "
                + "ORDER BY existing.dayOfWeek, existing.startPeriod, "
                + "existing.weekStart, sc.selectTime LIMIT 1";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, studentId);
            statement.setString(2, candidateTeachingClassId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new StudentScheduleConflict(
                        rs.getString("courseName"),
                        rs.getInt("weekStart"),
                        rs.getInt("weekEnd"),
                        rs.getInt("dayOfWeek"),
                        rs.getInt("startPeriod"),
                        rs.getInt("endPeriod"));
            }
        }
    }

    /** 判断同一教室是否存在时间重叠，并锁定匹配范围。 */
    public boolean hasClassroomConflict(Connection conn, CourseSchedule schedule,
                                        String excludeScheduleId) throws SQLException {
        String sql = "SELECT 1 FROM tblCourseSchedule "
                + "WHERE dayOfWeek = ? AND classroom = ? "
                + "AND weekStart <= ? AND weekEnd >= ? "
                + "AND startPeriod <= ? AND endPeriod >= ? "
                + "AND (? IS NULL OR scheduleId <> ?) LIMIT 1 FOR UPDATE";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, schedule.getDayOfWeek());
            pstmt.setString(2, schedule.getClassroom());
            pstmt.setInt(3, schedule.getWeekEnd());
            pstmt.setInt(4, schedule.getWeekStart());
            pstmt.setInt(5, schedule.getEndPeriod());
            pstmt.setInt(6, schedule.getStartPeriod());
            pstmt.setString(7, excludeScheduleId);
            pstmt.setString(8, excludeScheduleId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** 判断同一教师是否存在时间重叠，并锁定匹配范围。 */
    public boolean hasTeacherConflict(Connection conn, CourseSchedule schedule,
                                      String teacher, String excludeScheduleId) throws SQLException {
        String sql = "SELECT 1 FROM tblCourseSchedule s "
                + "JOIN tblTeachingClass tc ON s.teachingClassId = tc.teachingClassId "
                + "WHERE s.dayOfWeek = ? AND tc.teacher = ? "
                + "AND s.weekStart <= ? AND s.weekEnd >= ? "
                + "AND s.startPeriod <= ? AND s.endPeriod >= ? "
                + "AND (? IS NULL OR s.scheduleId <> ?) LIMIT 1 FOR UPDATE";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, schedule.getDayOfWeek());
            pstmt.setString(2, teacher);
            pstmt.setInt(3, schedule.getWeekEnd());
            pstmt.setInt(4, schedule.getWeekStart());
            pstmt.setInt(5, schedule.getEndPeriod());
            pstmt.setInt(6, schedule.getStartPeriod());
            pstmt.setString(7, excludeScheduleId);
            pstmt.setString(8, excludeScheduleId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** 绑定 INSERT 使用的排课字段。 */
    private void bindSchedule(PreparedStatement pstmt, CourseSchedule schedule) throws SQLException {
        pstmt.setString(1, schedule.getScheduleId());
        pstmt.setString(2, schedule.getTeachingClassId());
        pstmt.setString(3, schedule.getCourseId());
        pstmt.setString(4, schedule.getClassroom());
        pstmt.setInt(5, schedule.getWeekStart());
        pstmt.setInt(6, schedule.getWeekEnd());
        pstmt.setInt(7, schedule.getDayOfWeek());
        pstmt.setInt(8, schedule.getStartPeriod());
        pstmt.setInt(9, schedule.getEndPeriod());
        pstmt.setTime(10, Time.valueOf(schedule.getStartTime()));
        pstmt.setTime(11, Time.valueOf(schedule.getEndTime()));
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
        schedule.setTeachingClassId(rs.getString("teachingClassId"));
        schedule.setClassroom(rs.getString("classroom"));
        schedule.setWeekStart(rs.getInt("weekStart"));
        schedule.setWeekEnd(rs.getInt("weekEnd"));
        schedule.setDayOfWeek(rs.getInt("dayOfWeek"));
        schedule.setStartPeriod(rs.getInt("startPeriod"));
        schedule.setEndPeriod(rs.getInt("endPeriod"));
        Time startTime = rs.getTime("startTime");
        Time endTime = rs.getTime("endTime");
        schedule.setStartTime(startTime == null ? null : startTime.toLocalTime());
        schedule.setEndTime(endTime == null ? null : endTime.toLocalTime());
        return schedule;
    }

    /** Populates normalized fields for legacy Course 1.0 callers. */
    private void normalizeCompatibilityFields(Connection conn, CourseSchedule schedule)
            throws SQLException {
        if (schedule.getTeachingClassId() == null || schedule.getTeachingClassId().isBlank()) {
            try (PreparedStatement statement = conn.prepareStatement(
                    "SELECT teachingClassId FROM tblTeachingClass WHERE courseId=? "
                            + "ORDER BY classNumber LIMIT 1")) {
                statement.setString(1, schedule.getCourseId());
                try (ResultSet rs = statement.executeQuery()) {
                    if (!rs.next()) {
                        throw new SQLException("课程没有可排课的教学班：" + schedule.getCourseId());
                    }
                    schedule.setTeachingClassId(rs.getString(1));
                }
            }
        } else {
            try (PreparedStatement statement = conn.prepareStatement(
                    "SELECT courseId FROM tblTeachingClass WHERE teachingClassId=?")) {
                statement.setString(1, schedule.getTeachingClassId());
                try (ResultSet rs = statement.executeQuery()) {
                    if (!rs.next()) {
                        throw new SQLException("教学班不存在：" + schedule.getTeachingClassId());
                    }
                    String authoritativeCourseId = rs.getString(1);
                    if (schedule.getCourseId() != null && !schedule.getCourseId().isBlank()
                            && !schedule.getCourseId().equals(authoritativeCourseId)) {
                        throw new SQLException("教学班与课程号不一致");
                    }
                    schedule.setCourseId(authoritativeCourseId);
                }
            }
        }
        if (schedule.getWeekStart() <= 0) {
            schedule.setWeekStart(1);
        }
        if (schedule.getWeekEnd() <= 0) {
            schedule.setWeekEnd(16);
        }
        if (schedule.getStartPeriod() <= 0) {
            schedule.setStartPeriod(periodFor(schedule.getStartTime()));
        }
        if (schedule.getEndPeriod() <= 0) {
            schedule.setEndPeriod(periodFor(schedule.getEndTime().minusMinutes(1)));
        }
    }

    private int periodFor(java.time.LocalTime time) {
        int minutes = time.getHour() * 60 + time.getMinute();
        if (minutes < 525) return 1;
        if (minutes < 575) return 2;
        if (minutes < 675) return 3;
        if (minutes < 725) return 4;
        if (minutes < 775) return 5;
        if (minutes < 875) return 6;
        if (minutes < 925) return 7;
        if (minutes < 1025) return 8;
        if (minutes < 1075) return 9;
        if (minutes < 1125) return 10;
        if (minutes < 1175) return 11;
        if (minutes < 1225) return 12;
        return 13;
    }

    /** Details of one existing course schedule that blocks a selection. */
    public record StudentScheduleConflict(
            String courseName,
            int weekStart,
            int weekEnd,
            int dayOfWeek,
            int startPeriod,
            int endPeriod) {
    }
}
