/*
 * CourseScheduleDAOTest
 *
 * Version 1.0
 *
 * 2026-09-04
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.server.dao;

import vcampus.common.vo.Course;
import vcampus.common.vo.CourseSchedule;

import java.sql.SQLException;
import java.time.LocalTime;
import java.util.List;

/** CourseScheduleDAO 可运行自测，包含 TIME 映射与数据库 CHECK 约束验证。 */
public class CourseScheduleDAOTest {

    private static final String COURSE_ID = "T_SCH_DAO_0904";
    private static final String SCHEDULE_ID = "T_SCH_0904";
    private static final String BAD_SCHEDULE_ID = "T_SCH_BAD_0904";

    /** 程序入口。 */
    public static void main(String[] args) throws Exception {
        CourseDAO courseDAO = new CourseDAO();
        CourseScheduleDAO scheduleDAO = new CourseScheduleDAO();
        cleanup(courseDAO, scheduleDAO);
        try {
            require(courseDAO.insertCourse(new Course(
                    COURSE_ID, "排课 DAO 测试", "测试教师", 2, 20, 0)), "准备测试课程");
            CourseSchedule schedule = new CourseSchedule(
                    SCHEDULE_ID, COURSE_ID, "教一-101", 1,
                    LocalTime.of(8, 0), LocalTime.of(9, 40));
            require(scheduleDAO.insertSchedule(schedule), "插入排课");

            CourseSchedule queried = scheduleDAO.findById(SCHEDULE_ID);
            require(queried != null
                            && LocalTime.of(8, 0).equals(queried.getStartTime())
                            && LocalTime.of(9, 40).equals(queried.getEndTime()),
                    "TIME 与 LocalTime 双向映射");
            require(scheduleDAO.findAll().stream()
                    .anyMatch(s -> SCHEDULE_ID.equals(s.getScheduleId())), "findAll 查询排课");
            require(scheduleDAO.findByCourseIds(List.of(COURSE_ID)).size() == 1,
                    "按课程集合查询排课");

            queried.setClassroom("教二-202");
            try (var conn = DbHelper.getConnection()) {
                require(scheduleDAO.updateSchedule(conn, queried), "修改排课教室");
            }
            require("教二-202".equals(scheduleDAO.findById(SCHEDULE_ID).getClassroom()),
                    "回查修改结果");

            CourseSchedule invalid = new CourseSchedule(
                    BAD_SCHEDULE_ID, COURSE_ID, "教三-303", 1,
                    LocalTime.of(12, 0), LocalTime.of(11, 0));
            boolean checkRejected = false;
            try {
                scheduleDAO.insertSchedule(invalid);
            } catch (SQLException expected) {
                checkRejected = expected.getErrorCode() == 3819;
            }
            require(checkRejected, "数据库 CHECK 拒绝 startTime >= endTime");

            require(scheduleDAO.deleteSchedule(SCHEDULE_ID), "删除排课");
            require(scheduleDAO.findById(SCHEDULE_ID) == null, "删除后查不到排课");
            System.out.println("COURSE_SCHEDULE_DAO_TEST=PASS");
        } finally {
            cleanup(courseDAO, scheduleDAO);
            int residue = residue(courseDAO, scheduleDAO);
            System.out.println("COURSE_SCHEDULE_DAO_TEST_RESIDUE=" + residue);
            require(residue == 0, "排课 DAO 测试数据清理完成");
        }
    }

    /** 清理测试数据。 */
    private static void cleanup(CourseDAO courseDAO, CourseScheduleDAO scheduleDAO)
            throws Exception {
        if (scheduleDAO.findById(SCHEDULE_ID) != null) {
            scheduleDAO.deleteSchedule(SCHEDULE_ID);
        }
        if (scheduleDAO.findById(BAD_SCHEDULE_ID) != null) {
            scheduleDAO.deleteSchedule(BAD_SCHEDULE_ID);
        }
        if (courseDAO.findById(COURSE_ID) != null) {
            courseDAO.deleteCourse(COURSE_ID);
        }
    }

    /** 统计残留。 */
    private static int residue(CourseDAO courseDAO, CourseScheduleDAO scheduleDAO)
            throws Exception {
        int count = courseDAO.findById(COURSE_ID) == null ? 0 : 1;
        count += scheduleDAO.findById(SCHEDULE_ID) == null ? 0 : 1;
        count += scheduleDAO.findById(BAD_SCHEDULE_ID) == null ? 0 : 1;
        return count;
    }

    /** 断言测试步骤。 */
    private static void require(boolean condition, String description) {
        if (!condition) {
            throw new IllegalStateException("FAIL: " + description);
        }
        System.out.println("PASS: " + description);
    }
}
