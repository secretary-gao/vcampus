/*
 * CourseScheduleClientSrvTest
 *
 * Version 1.0
 *
 * 2026-09-04
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.client.biz;

import vcampus.common.vo.CourseSchedule;

import java.time.LocalTime;
import java.util.List;

/** 排课模块无界面 Socket 端到端测试，只通过 ICourseClientSrv 访问服务器。 */
public class CourseScheduleClientSrvTest {

    private static final String NORMAL_COURSE_ID = "T_E2E_NORMAL_0904";
    private static final String FULL_COURSE_ID = "T_E2E_FULL_0904";
    private static final String SAME_TEACHER_COURSE_ID = "T_E2E_ROLL_0904";
    private static final String STUDENT_ID = "E2E0904001";

    /** 程序入口。 */
    public static void main(String[] args) throws Exception {
        ICourseClientSrv client = new CourseClientSrv();

        CourseSchedule first = client.addSchedule(new CourseSchedule(
                null, NORMAL_COURSE_ID, "Socket-101", 1,
                LocalTime.of(8, 0), LocalTime.of(9, 0)));
        require(first.getScheduleId() != null, "Socket 管理员新增排课");

        expectFailure(() -> client.addSchedule(new CourseSchedule(
                null, FULL_COURSE_ID, "Socket-101", 1,
                LocalTime.of(8, 30), LocalTime.of(9, 30))), "教室冲突经 Socket 返回");
        expectFailure(() -> client.addSchedule(new CourseSchedule(
                null, SAME_TEACHER_COURSE_ID, "Socket-202", 1,
                LocalTime.of(8, 30), LocalTime.of(9, 30))), "教师冲突经 Socket 返回");

        CourseSchedule second = client.addSchedule(new CourseSchedule(
                null, FULL_COURSE_ID, "Socket-202", 2,
                LocalTime.of(10, 0), LocalTime.of(11, 0)));
        List<CourseSchedule> all = client.querySchedule();
        require(all.stream().anyMatch(s -> first.getScheduleId().equals(s.getScheduleId()))
                        && all.stream().anyMatch(s -> second.getScheduleId().equals(s.getScheduleId())),
                "Socket 查询全部排课");

        second.setClassroom("Socket-303");
        second.setStartTime(LocalTime.of(11, 0));
        second.setEndTime(LocalTime.of(12, 0));
        require(client.updateSchedule(second), "Socket 管理员修改排课");

        require(client.selectCourse(STUDENT_ID, NORMAL_COURSE_ID), "准备学生已选课程");
        List<CourseSchedule> timetable = client.queryStudentSchedule(STUDENT_ID);
        require(timetable.stream().anyMatch(s -> first.getScheduleId().equals(s.getScheduleId()))
                        && timetable.stream().noneMatch(s -> second.getScheduleId().equals(s.getScheduleId())),
                "Socket 学生课程表只包含已选课程");

        require(client.deleteSchedule(first.getScheduleId()), "Socket 删除第一条排课");
        require(client.deleteSchedule(second.getScheduleId()), "Socket 删除第二条排课");
        require(client.dropCourse(STUDENT_ID, NORMAL_COURSE_ID), "清理学生选课");
        require(client.querySchedule().stream()
                .noneMatch(s -> first.getScheduleId().equals(s.getScheduleId())
                        || second.getScheduleId().equals(s.getScheduleId())), "Socket 删除后查不到排课");
        System.out.println("COURSE_SCHEDULE_SOCKET_E2E_TEST=PASS");
    }

    /** 断言业务调用失败。 */
    private static void expectFailure(CheckedAction action, String description) throws Exception {
        boolean failed = false;
        try {
            action.run();
        } catch (CourseClientException expected) {
            failed = "400".equals(expected.getStatusCode());
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
