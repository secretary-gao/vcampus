/*
 * CourseClientSrvTest
 *
 * Version 1.0
 *
 * 2026-09-04
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.client.biz;

import vcampus.common.vo.AutoSchedulePlan;
import vcampus.common.vo.AutoScheduleRequest;
import vcampus.common.vo.Course;
import vcampus.common.vo.CourseDashboardStats;
import vcampus.common.vo.SelectCourse;
import vcampus.common.vo.TeacherCourseEnrollment;
import vcampus.common.vo.TeachingClass;

import java.util.List;

/**
 * Course 模块无界面 Socket 端到端测试。运行前先启动服务器并执行服务端测试夹具 setup。
 * 本类只调用 {@link ICourseClientSrv}，不访问 DAO 或 MySQL。
 */
public class CourseClientSrvTest {

    private static final String NORMAL_COURSE_ID = "T_E2E_NORMAL_0904";
    private static final String FULL_COURSE_ID = "T_E2E_FULL_0904";
    private static final String ROLLBACK_COURSE_ID = "T_E2E_ROLL_0904";
    private static final String STUDENT_A = "E2E0904001";
    private static final String STUDENT_B = "E2E0904002";
    private static final String USER_A = "CE2EA904";
    private static final String ADMIN_COURSE_ID = "T_E2E_ADMIN_0907";
    private static final String TIME_SOURCE_COURSE_ID = "T_E2E_TIME_SRC";
    private static final String TIME_CONFLICT_COURSE_ID = "T_E2E_TIME_CONFLICT";

    /**
     * 程序入口。
     *
     * @param args 命令行参数（未使用）
     * @throws Exception 端到端测试失败
     */
    public static void main(String[] args) throws Exception {
        ICourseClientSrv client = new CourseClientSrv();

        require(STUDENT_A.equals(client.queryStudentId(USER_A)),
                "Socket 登录用户映射正式学号");

        List<Course> courses = client.queryCourse("Socket");
        require(findCourse(courses, NORMAL_COURSE_ID) != null, "Socket 查询课程");
        require(client.queryTeachingClass(NORMAL_COURSE_ID).stream()
                .anyMatch(value -> NORMAL_COURSE_ID.equals(value.getCourseId())),
                "Socket 查询具体教学班");

        require(client.selectCourse(STUDENT_A, NORMAL_COURSE_ID), "Socket 正常选课");
        require(!client.queryRequirementGroups().isEmpty(),
                "Socket query CourseRequirementGroup");
        CourseDashboardStats dashboard = client.queryDashboard();
        require(dashboard.getCourseCount() > 0 && dashboard.getTeachingClassCount() > 0,
                "Socket query Course dashboard");
        int scheduleCountBeforePreview = client.querySchedule().size();
        AutoSchedulePlan preview = client.previewAutoSchedule(new AutoScheduleRequest(
                List.of(NORMAL_COURSE_ID + "-01"), 1, 16));
        require(preview.getUnassignedTeachingClassIds().isEmpty()
                        && preview.getAssignments().size() == 1,
                "Socket auto-schedule Preview returns a complete plan");
        require(client.querySchedule().size() == scheduleCountBeforePreview,
                "Socket auto-schedule Preview does not mutate the database");

        List<SelectCourse> selected = client.querySelectedCourse(STUDENT_A);
        require(selected.stream().anyMatch(r -> NORMAL_COURSE_ID.equals(r.getCourseId())),
                "Socket 查询本人已选课程");
        require(selected.stream().anyMatch(r -> r.getTeachingClassId() != null),
                "Socket 选课记录包含教学班 ID");

        expectFailure(() -> client.selectCourse(STUDENT_A, NORMAL_COURSE_ID), "400",
                "Socket 重复选课失败");
        Course normalAfterDuplicate = findCourse(client.queryCourse(NORMAL_COURSE_ID), NORMAL_COURSE_ID);
        require(normalAfterDuplicate != null && normalAfterDuplicate.getSelectedCount() == 1,
                "重复选课后 selectedCount 仍为 1");

        expectFailure(() -> client.queryTeacherCourseEnrollments(new vcampus.common.vo.User()),
                "403", "未登录账号不能查询教师课程学生名单");

        expectFailure(() -> client.selectCourse(STUDENT_B, FULL_COURSE_ID), "400",
                "Socket 满员课程选课失败");
        Course fullCourse = findCourse(client.queryCourse(FULL_COURSE_ID), FULL_COURSE_ID);
        require(fullCourse != null && fullCourse.getSelectedCount() == 1,
                "满员失败后 selectedCount 不变");

        expectFailure(() -> client.selectCourse(STUDENT_B, ROLLBACK_COURSE_ID), "500",
                "Socket 人为第二步失败");
        require(client.querySelectedCourse(STUDENT_B).stream()
                        .noneMatch(r -> ROLLBACK_COURSE_ID.equals(r.getCourseId())),
                "Socket 回滚后无选课记录");
        Course rollbackCourse = findCourse(
                client.queryCourse(ROLLBACK_COURSE_ID), ROLLBACK_COURSE_ID);
        require(rollbackCourse != null && rollbackCourse.getSelectedCount() == 0,
                "Socket 回滚后 selectedCount 为 0");

        require(client.selectCourse(STUDENT_B, TIME_SOURCE_COURSE_ID),
                "Socket 准备时间冲突来源课程");
        expectFailure(() -> client.selectCourse(STUDENT_B, TIME_CONFLICT_COURSE_ID),
                "400", "课程时间冲突", "Socket 绕过 UI 仍拒绝时间冲突教学班");
        require(client.querySelectedCourse(STUDENT_B).stream()
                        .noneMatch(r -> TIME_CONFLICT_COURSE_ID.equals(r.getCourseId())),
                "Socket 冲突拒绝后 SelectCourse 不新增");
        Course conflictCourse = findCourse(
                client.queryCourse(TIME_CONFLICT_COURSE_ID), TIME_CONFLICT_COURSE_ID);
        require(conflictCourse != null && conflictCourse.getSelectedCount() == 0,
                "Socket 冲突拒绝后 selectedCount 不变化");
        require(client.dropCourse(STUDENT_B, TIME_SOURCE_COURSE_ID),
                "Socket 清理时间冲突来源课程");

        require(client.dropCourse(STUDENT_A, NORMAL_COURSE_ID), "Socket 正常退课");
        require(client.querySelectedCourse(STUDENT_A).stream()
                        .noneMatch(r -> NORMAL_COURSE_ID.equals(r.getCourseId())),
                "Socket 退课后已选列表无记录");
        Course normalAfterDrop = findCourse(client.queryCourse(NORMAL_COURSE_ID), NORMAL_COURSE_ID);
        require(normalAfterDrop != null && normalAfterDrop.getSelectedCount() == 0,
                "Socket 退课后 selectedCount 为 0");

        Course added = client.addCourse(new Course(
                ADMIN_COURSE_ID, "Socket 管理课程", "Socket 教师", 2, 25, 8));
        require(added.getSelectedCount() == 0, "Socket 管理员新增课程");
        TeachingClass extra = client.addTeachingClass(new TeachingClass(
                ADMIN_COURSE_ID + "-02", ADMIN_COURSE_ID, "02", "Socket 教师乙",
                12, 99, null, "Socket test"));
        require(extra.getSelectedCount() == 0, "Socket 管理员新增教学班");
        extra.setCapacity(15);
        require(client.updateTeachingClass(extra), "Socket 管理员修改教学班");
        require(client.deleteTeachingClass(extra.getTeachingClassId()),
                "Socket 管理员删除教学班");
        require(client.updateCourse(new Course(
                ADMIN_COURSE_ID, "Socket 管理课程已改", "Socket 教师", 3, 30, 99)),
                "Socket 管理员修改课程");
        Course updated = findCourse(client.queryCourse(ADMIN_COURSE_ID), ADMIN_COURSE_ID);
        require(updated != null && updated.getCredit() == 3
                        && updated.getSelectedCount() == 0,
                "Socket 回查课程修改且服务端维护人数");
        require(client.deleteCourse(ADMIN_COURSE_ID), "Socket 管理员删除课程");

        System.out.println("COURSE_SOCKET_E2E_TEST=PASS");
    }

    /** 查找指定课程。 */
    private static Course findCourse(List<Course> courses, String courseId) {
        return courses.stream()
                .filter(course -> courseId.equals(course.getCourseId()))
                .findFirst()
                .orElse(null);
    }

    /** 断言客户端调用以指定状态码失败。 */
    private static void expectFailure(CheckedAction action, String statusCode, String description)
            throws Exception {
        expectFailure(action, statusCode, null, description);
    }

    /** Asserts both the response status and, when supplied, a clear error fragment. */
    private static void expectFailure(CheckedAction action, String statusCode,
                                      String messageFragment, String description)
            throws Exception {
        boolean failed = false;
        try {
            action.run();
        } catch (CourseClientException expected) {
            failed = statusCode.equals(expected.getStatusCode())
                    && (messageFragment == null
                    || (expected.getMessage() != null
                    && expected.getMessage().contains(messageFragment)));
        }
        require(failed, description);
    }

    /** 断言测试步骤成功。 */
    private static void require(boolean condition, String description) {
        if (!condition) {
            throw new IllegalStateException("FAIL: " + description);
        }
        System.out.println("PASS: " + description);
    }

    /** 可抛异常的客户端动作。 */
    @FunctionalInterface
    private interface CheckedAction {
        void run() throws Exception;
    }
}
