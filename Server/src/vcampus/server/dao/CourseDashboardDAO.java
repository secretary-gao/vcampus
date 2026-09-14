package vcampus.server.dao;

import vcampus.common.vo.CourseDashboardStats;
import vcampus.common.vo.TeachingClassStatistic;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Read-only aggregate queries for the Course administrator dashboard. */
public class CourseDashboardDAO {
    public CourseDashboardStats load() throws SQLException, IOException {
        try (Connection conn = DbHelper.getConnection()) {
            CourseDashboardStats value = new CourseDashboardStats();
            try (PreparedStatement statement = conn.prepareStatement(
                    "SELECT (SELECT COUNT(*) FROM tblCourse) courseCount, "
                            + "COUNT(*) classCount, COALESCE(AVG(capacity),0) avgCapacity, "
                            + "COALESCE(AVG(selectedCount),0) avgSelected, "
                            + "SUM(CASE WHEN capacity>0 AND selectedCount>=capacity THEN 1 ELSE 0 END) fullCount, "
                            + "COALESCE(SUM(selectedCount)/NULLIF(SUM(capacity),0),0) utilization "
                            + "FROM tblTeachingClass");
                 ResultSet rs = statement.executeQuery()) {
                rs.next();
                value.setCourseCount(rs.getInt("courseCount"));
                value.setTeachingClassCount(rs.getInt("classCount"));
                value.setAverageCapacity(rs.getDouble("avgCapacity"));
                value.setAverageSelected(rs.getDouble("avgSelected"));
                value.setFullClassCount(rs.getInt("fullCount"));
                value.setCapacityUtilization(rs.getDouble("utilization"));
            }
            try (PreparedStatement statement = conn.prepareStatement(
                    "SELECT COUNT(*) selectionCount, COUNT(DISTINCT studentId) studentCount "
                            + "FROM tblSelectCourse"); ResultSet rs = statement.executeQuery()) {
                rs.next();
                value.setSelectionCount(rs.getInt("selectionCount"));
                value.setStudentCount(rs.getInt("studentCount"));
            }
            value.setPopularClasses(top(conn, "tc.selectedCount DESC, tc.teachingClassId", 5));
            value.setAvailableClasses(top(conn,
                    "(tc.capacity-tc.selectedCount) DESC, tc.teachingClassId", 5));
            value.setAllClasses(top(conn, "c.courseId, tc.classNumber", Integer.MAX_VALUE));
            return value;
        }
    }

    public List<TeachingClassStatistic> allClassStatistics()
            throws SQLException, IOException {
        try (Connection conn = DbHelper.getConnection()) {
            return top(conn, "c.courseId, tc.classNumber", Integer.MAX_VALUE);
        }
    }

    private List<TeachingClassStatistic> top(Connection conn, String order, int limit)
            throws SQLException {
        String sql = "SELECT tc.teachingClassId,c.courseName,tc.teacher,tc.capacity,tc.selectedCount "
                + "FROM tblTeachingClass tc JOIN tblCourse c ON c.courseId=tc.courseId "
                + "ORDER BY " + order + (limit == Integer.MAX_VALUE ? "" : " LIMIT " + limit);
        List<TeachingClassStatistic> result = new ArrayList<>();
        try (PreparedStatement statement = conn.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) result.add(new TeachingClassStatistic(
                    rs.getString("teachingClassId"), rs.getString("courseName"),
                    rs.getString("teacher"), rs.getInt("capacity"),
                    rs.getInt("selectedCount")));
        }
        return result;
    }
}
