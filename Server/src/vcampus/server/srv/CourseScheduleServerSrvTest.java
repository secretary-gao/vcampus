/*
 * CourseScheduleServerSrvTest
 *
 * Version 1.0
 *
 * 2026-09-04
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.server.srv;

import vcampus.common.vo.Course;
import vcampus.common.vo.CourseSchedule;
import vcampus.server.dao.CourseDAO;
import vcampus.server.dao.CourseScheduleDAO;
import vcampus.server.dao.SelectCourseDAO;

import java.time.LocalTime;
import java.util.List;

/** 排课业务自测，覆盖教室冲突、教师冲突、CRUD 和学生课程表。 */
public class CourseScheduleServerSrvTest {

    private static final String COURSE_A = "T_SCH_SRV_A_0904";
    private static final String COURSE_B = "T_SCH_SRV_B_0904";
    private static final String COURSE_C = "T_SCH_SRV_C_0904";
    private static final String STUDENT_ID = "SCH0904001";

    /** 程序入口。 */
    public static void main(String[] args) throws Exception {
        CourseDAO courseDAO = new CourseDAO();
        SelectCourseDAO selectDAO = new SelectCourseDAO();
        CourseScheduleDAO scheduleDAO = new CourseScheduleDAO();
        CourseServerSrv service = new CourseServerSrv(courseDAO, selectDAO, scheduleDAO);

        cleanup(courseDAO, selectDAO, scheduleDAO);
        try {
            require(courseDAO.insertCourse(new Course(
                    COURSE_A, "排课课程 A", "同一教师", 2, 20, 0)), "准备课程 A");
            require(courseDAO.insertCourse(new Course(
                    COURSE_B, "排课课程 B", "同一教师", 2, 20, 0)), "准备课程 B");
            require(courseDAO.insertCourse(new Course(
                    COURSE_C, "排课课程 C", "另一教师", 2, 20, 0)), "准备课程 C");

            CourseSchedule first = service.addSchedule(new CourseSchedule(
                    null, COURSE_A, "教一-101", 1,
                    LocalTime.of(9, 0), LocalTime.of(10, 0)));
            require(first.getScheduleId() != null, "新增排课并生成记录号");

            expectFailure(() -> service.addSchedule(new CourseSchedule(
                    null, COURSE_C, "教一-101", 1,
                    LocalTime.of(9, 30), LocalTime.of(10, 30))), "教室重叠冲突");
            expectFailure(() -> service.addSchedule(new CourseSchedule(
                    null, COURSE_B, "教二-202", 1,
                    LocalTime.of(9, 30), LocalTime.of(10, 30))), "教师重叠冲突");
            expectFailure(() -> service.addSchedule(new CourseSchedule(
                    null, COURSE_C, "教三-303", 2,
                    LocalTime.of(11, 0), LocalTime.of(10, 0))), "开始时间必须早于结束时间");

            CourseSchedule second = service.addSchedule(new CourseSchedule(
                    null, COURSE_C, "教二-202", 1,
                    LocalTime.of(9, 30), LocalTime.of(10, 30)));
            require(service.querySchedule().size() >= 2, "查询全部排课");

            second.setClassroom("教三-303");
            second.setDayOfWeek(2);
            second.setStartTime(LocalTime.of(10, 0));
            second.setEndTime(LocalTime.of(11, 0));
            require(service.updateSchedule(second), "修改排课");

            require(service.selectCourse(STUDENT_ID, COURSE_A), "学生选择课程 A");
            require(service.selectCourse(STUDENT_ID, COURSE_C), "学生选择课程 C");
            List<CourseSchedule> timetable = service.queryStudentSchedule(STUDENT_ID);
            require(timetable.stream().anyMatch(s -> COURSE_A.equals(s.getCourseId()))
                            && timetable.stream().anyMatch(s -> COURSE_C.equals(s.getCourseId())),
                    "学生课程表只汇总已选课程排课");

            require(service.deleteSchedule(first.getScheduleId()), "删除第一条排课");
            require(scheduleDAO.findById(first.getScheduleId()) == null, "删除后排课不存在");
            System.out.println("COURSE_SCHEDULE_SERVER_SRV_TEST=PASS");
        } finally {
            cleanup(courseDAO, selectDAO, scheduleDAO);
            int residue = residue(courseDAO, selectDAO, scheduleDAO);
            System.out.println("COURSE_SCHEDULE_SERVER_SRV_TEST_RESIDUE=" + residue);
            require(residue == 0, "排课 Service 测试数据清理完成");
        }
    }

    /** 清理测试数据。 */
    private static void cleanup(CourseDAO courseDAO, SelectCourseDAO selectDAO,
                                CourseScheduleDAO scheduleDAO) throws Exception {
        for (CourseSchedule schedule : scheduleDAO.findAll()) {
            if (List.of(COURSE_A, COURSE_B, COURSE_C).contains(schedule.getCourseId())) {
                scheduleDAO.deleteSchedule(schedule.getScheduleId());
            }
        }
        selectDAO.deleteSelectCourse(STUDENT_ID, COURSE_A);
        selectDAO.deleteSelectCourse(STUDENT_ID, COURSE_B);
        selectDAO.deleteSelectCourse(STUDENT_ID, COURSE_C);
        deleteCourse(courseDAO, COURSE_A);
        deleteCourse(courseDAO, COURSE_B);
        deleteCourse(courseDAO, COURSE_C);
    }

    /** 统计测试残留。 */
    private static int residue(CourseDAO courseDAO, SelectCourseDAO selectDAO,
                               CourseScheduleDAO scheduleDAO) throws Exception {
        int count = 0;
        count += courseDAO.findById(COURSE_A) == null ? 0 : 1;
        count += courseDAO.findById(COURSE_B) == null ? 0 : 1;
        count += courseDAO.findById(COURSE_C) == null ? 0 : 1;
        count += selectDAO.findByStudentAndCourse(STUDENT_ID, COURSE_A) == null ? 0 : 1;
        count += selectDAO.findByStudentAndCourse(STUDENT_ID, COURSE_C) == null ? 0 : 1;
        count += scheduleDAO.findAll().stream()
                .filter(s -> List.of(COURSE_A, COURSE_B, COURSE_C).contains(s.getCourseId()))
                .count();
        return count;
    }

    /** 删除存在的测试课程。 */
    private static void deleteCourse(CourseDAO courseDAO, String courseId) throws Exception {
        if (courseDAO.findById(courseId) != null) {
            courseDAO.deleteCourse(courseId);
        }
    }

    /** 断言业务失败。 */
    private static void expectFailure(CheckedAction action, String description) throws Exception {
        boolean failed = false;
        try {
            action.run();
        } catch (CourseServiceException expected) {
            failed = true;
        }
        require(failed, description);
    }

    /** 断言测试步骤。 */
    private static void require(boolean condition, String description) {
        if (!condition) {
            throw new IllegalStateException("FAIL: " + description);
        }
        System.out.println("PASS: " + description);
    }

    @FunctionalInterface
    private interface CheckedAction {
        void run() throws Exception;
    }
}
