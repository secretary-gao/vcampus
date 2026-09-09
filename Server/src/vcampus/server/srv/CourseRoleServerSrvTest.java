/*
 * CourseRoleServerSrvTest
 *
 * Version 1.0
 *
 * 2026-09-07
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.server.srv;

import vcampus.common.vo.Course;
import vcampus.common.vo.CourseSchedule;
import vcampus.common.vo.TeacherCourseEnrollment;
import vcampus.server.dao.CourseDAO;
import vcampus.server.dao.CourseScheduleDAO;
import vcampus.server.dao.CourseTestData;
import vcampus.server.dao.SelectCourseDAO;

import java.time.LocalTime;
import java.util.List;

/** 可运行的 Course 三角色新增能力与数据清理回归测试。 */
public class CourseRoleServerSrvTest {

    private static final String SUFFIX = Long.toHexString(System.nanoTime());
    private static final String COURSE_A = id("T_RA_");
    private static final String COURSE_B = id("T_RB_");
    private static final String COURSE_C = id("T_RC_");
    private static final String USER_ID = ("R" + SUFFIX).substring(0, 8);
    private static final String STUDENT_ID = ("R" + SUFFIX + "000000000").substring(0, 10);

    /** 程序入口。 */
    public static void main(String[] args) throws Exception {
        CourseServerSrv service = new CourseServerSrv();
        cleanup();
        try {
            Course added = service.addCourse(
                    new Course(COURSE_A, "角色测试课程", "角色教师", 2, 2, 99));
            require(added.getSelectedCount() == 0, "新增课程人数由服务端初始化为 0");
            expectBusinessFailure(() -> service.addCourse(
                            new Course(COURSE_A, "重复课程", "角色教师", 2, 2, 0)),
                    "重复课程号按业务错误拒绝");

            List<TeacherCourseEnrollment> emptyRoster =
                    service.queryTeacherCourseEnrollments("角色教师");
            require(emptyRoster.stream().anyMatch(row -> COURSE_A.equals(row.getCourseId())
                            && row.getStudentId() == null),
                    "教师可看到尚无人选的本人课程");

            CourseTestData.prepareStudent(USER_ID, STUDENT_ID);
            require(service.selectCourse(STUDENT_ID, COURSE_A), "为教师名单准备选课记录");
            List<TeacherCourseEnrollment> roster =
                    service.queryTeacherCourseEnrollments("角色教师");
            require(roster.stream().anyMatch(row -> COURSE_A.equals(row.getCourseId())
                            && STUDENT_ID.equals(row.getStudentId())
                            && "课程测试".equals(row.getStudentName())),
                    "教师按登录姓名查询到本人课程的学生名单");

            expectBusinessFailure(() -> service.deleteCourse(COURSE_A),
                    "已有选课记录的课程不能删除");
            require(service.updateCourse(
                    new Course(COURSE_A, "角色测试课程已改", "角色教师", 3, 3, 999)),
                    "管理员修改课程主数据");
            Course updated = new CourseDAO().findById(COURSE_A);
            require(updated != null && updated.getSelectedCount() == 1,
                    "修改课程不会覆盖真实 selectedCount");
            require(service.dropCourse(STUDENT_ID, COURSE_A), "清理选课记录");

            service.addCourse(new Course(COURSE_B, "教师冲突甲", "甲教师", 1, 20, 0));
            service.addCourse(new Course(COURSE_C, "教师冲突乙", "乙教师", 1, 20, 0));
            CourseSchedule scheduleB = service.addSchedule(new CourseSchedule(
                    null, COURSE_B, "角色教室甲", 2,
                    LocalTime.of(8, 0), LocalTime.of(9, 40)));
            CourseSchedule scheduleC = service.addSchedule(new CourseSchedule(
                    null, COURSE_C, "角色教室乙", 2,
                    LocalTime.of(8, 0), LocalTime.of(9, 40)));
            expectBusinessFailure(() -> service.updateCourse(
                            new Course(COURSE_C, "教师冲突乙", "甲教师", 1, 20, 0)),
                    "修改授课教师时拒绝已有排课冲突");
            expectBusinessFailure(() -> service.deleteCourse(COURSE_B),
                    "已有排课的课程不能删除");
            service.deleteSchedule(scheduleB.getScheduleId());
            service.deleteSchedule(scheduleC.getScheduleId());

            require(service.deleteCourse(COURSE_A), "删除无引用课程 A");
            require(service.deleteCourse(COURSE_B), "删除无引用课程 B");
            require(service.deleteCourse(COURSE_C), "删除无引用课程 C");
            System.out.println("COURSE_ROLE_SERVER_SRV_TEST=PASS");
        } finally {
            cleanup();
            int residue = residue();
            System.out.println("COURSE_ROLE_SERVER_SRV_TEST_RESIDUE=" + residue);
            require(residue == 0, "角色测试数据清理完成");
        }
    }

    private static String id(String prefix) {
        String value = prefix + SUFFIX;
        return value.substring(0, Math.min(value.length(), 20));
    }

    private static void cleanup() throws Exception {
        CourseScheduleDAO scheduleDAO = new CourseScheduleDAO();
        for (CourseSchedule schedule : scheduleDAO.findByCourseIds(
                List.of(COURSE_A, COURSE_B, COURSE_C))) {
            scheduleDAO.deleteSchedule(schedule.getScheduleId());
        }
        SelectCourseDAO selectDAO = new SelectCourseDAO();
        selectDAO.deleteSelectCourse(STUDENT_ID, COURSE_A);
        CourseDAO courseDAO = new CourseDAO();
        deleteIfPresent(courseDAO, COURSE_A);
        deleteIfPresent(courseDAO, COURSE_B);
        deleteIfPresent(courseDAO, COURSE_C);
        CourseTestData.cleanupStudent(USER_ID, STUDENT_ID);
    }

    private static int residue() throws Exception {
        CourseDAO courseDAO = new CourseDAO();
        int count = 0;
        count += courseDAO.findById(COURSE_A) == null ? 0 : 1;
        count += courseDAO.findById(COURSE_B) == null ? 0 : 1;
        count += courseDAO.findById(COURSE_C) == null ? 0 : 1;
        count += new CourseScheduleDAO().findByCourseIds(
                List.of(COURSE_A, COURSE_B, COURSE_C)).size();
        count += new SelectCourseDAO().findByStudentAndCourse(STUDENT_ID, COURSE_A) == null ? 0 : 1;
        count += CourseTestData.countResidue(USER_ID, STUDENT_ID);
        return count;
    }

    private static void deleteIfPresent(CourseDAO dao, String courseId) throws Exception {
        if (dao.findById(courseId) != null) {
            dao.deleteCourse(courseId);
        }
    }

    private static void expectBusinessFailure(CheckedAction action, String description)
            throws Exception {
        boolean failed = false;
        try {
            action.run();
        } catch (CourseServiceException expected) {
            failed = true;
        }
        require(failed, description);
    }

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
