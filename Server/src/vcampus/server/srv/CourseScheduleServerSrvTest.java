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
import vcampus.server.dao.CourseTestData;
import vcampus.server.dao.DbHelper;
import vcampus.server.dao.SelectCourseDAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** 排课业务自测，覆盖教室冲突、教师冲突、CRUD 和学生课程表。 */
public class CourseScheduleServerSrvTest {

    // 同时隔离主键和冲突检查使用的教师、教室，不占用 demo 或业务数据的时空资源。
    private final String _runId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    private final String COURSE_A = "TSA" + _runId;
    private final String COURSE_B = "TSB" + _runId;
    private final String COURSE_C = "TSC" + _runId;
    private final String STUDENT_ID = "TS" + _runId.substring(0, 8);
    private final String USER_ID = _runId.substring(0, 8);
    private final String TEACHER_A = "TA" + _runId;
    private final String TEACHER_B = "TB" + _runId;
    private final String ROOM_A = "RA" + _runId;
    private final String ROOM_B = "RB" + _runId;
    private final String ROOM_C = "RC" + _runId;
    private final List<String> _createdCourses = new ArrayList<>();
    private boolean _studentCreated;

    /** 程序入口。 */
    public static void main(String[] args) throws Exception {
        new CourseScheduleServerSrvTest().run();
    }

    private void run() throws Exception {
        CourseDAO courseDAO = new CourseDAO();
        SelectCourseDAO selectDAO = new SelectCourseDAO();
        CourseScheduleDAO scheduleDAO = new CourseScheduleDAO();
        CourseServerSrv service = new CourseServerSrv(courseDAO, selectDAO, scheduleDAO);

        try {
            prepareStudent();
            prepareCourse(courseDAO, COURSE_A, TEACHER_A);
            prepareCourse(courseDAO, COURSE_B, TEACHER_A);
            prepareCourse(courseDAO, COURSE_C, TEACHER_B);

            CourseSchedule first = new CourseSchedule(null, COURSE_A, ROOM_A, 1,
                    LocalTime.of(9, 0), LocalTime.of(10, 0));
            first.setStartPeriod(2);
            first.setEndPeriod(3);
            first = service.addSchedule(first);
            require(first.getScheduleId() != null, "新增排课并生成记录号");

            CourseSchedule classroomConflict = new CourseSchedule(null, COURSE_C, ROOM_A, 1,
                    LocalTime.of(9, 30), LocalTime.of(10, 30));
            classroomConflict.setStartPeriod(3);
            classroomConflict.setEndPeriod(4);
            expectFailure(() -> service.addSchedule(classroomConflict), "教室重叠冲突");
            CourseSchedule teacherConflict = new CourseSchedule(null, COURSE_B, ROOM_B, 1,
                    LocalTime.of(9, 30), LocalTime.of(10, 30));
            teacherConflict.setStartPeriod(3);
            teacherConflict.setEndPeriod(4);
            expectFailure(() -> service.addSchedule(teacherConflict), "教师重叠冲突");
            expectFailure(() -> service.addSchedule(new CourseSchedule(
                    null, COURSE_C, ROOM_C, 2,
                    LocalTime.of(11, 0), LocalTime.of(10, 0))), "开始时间必须早于结束时间");

            CourseSchedule adjacent = new CourseSchedule(null, COURSE_C, ROOM_A, 1,
                    LocalTime.of(10, 0), LocalTime.of(11, 0));
            adjacent.setStartPeriod(4);
            adjacent.setEndPeriod(5);
            adjacent = service.addSchedule(adjacent);
            require(adjacent != null, "相邻节次边界不视为冲突");
            service.deleteSchedule(adjacent.getScheduleId());

            CourseSchedule laterWeeks = new CourseSchedule(
                    null, COURSE_C, ROOM_A, 1,
                    LocalTime.of(9, 0), LocalTime.of(10, 0));
            laterWeeks.setWeekStart(17);
            laterWeeks.setWeekEnd(18);
            laterWeeks.setStartPeriod(2);
            laterWeeks.setEndPeriod(3);
            require(service.addSchedule(laterWeeks) != null,
                    "相同教室时段但周次不重叠时允许排课");

            CourseSchedule second = service.addSchedule(new CourseSchedule(
                    null, COURSE_C, ROOM_B, 1,
                    LocalTime.of(9, 30), LocalTime.of(10, 30)));
            require(service.querySchedule().size() >= 2, "查询全部排课");

            second.setClassroom(ROOM_C);
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

    /** 只插入新夹具，不预先删除任何同 ID 的既有数据；碰撞时由唯一键拒绝。 */
    private void prepareStudent() throws Exception {
        try (Connection conn = DbHelper.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement user = conn.prepareStatement(
                    "INSERT INTO tblUser (uId, uName, uAge, uSex, uPwd, uRole) "
                            + "VALUES (?, '排课测试', 20, '男', ?, '学生')");
                 PreparedStatement student = conn.prepareStatement(
                         "INSERT INTO tblStudent (studentId, campusCardNo, userId, name, "
                                 + "className, major, grade, enrollmentDate, status) "
                                 + "VALUES (?, ?, ?, '排课测试', '测试班', '计算机', "
                                 + "'2026', '2026-09-01', '在读')")) {
                user.setString(1, USER_ID);
                user.setString(2, "00000000000000000000000000000000");
                user.executeUpdate();
                student.setString(1, STUDENT_ID);
                student.setString(2, "CARD" + _runId);
                student.setString(3, USER_ID);
                student.executeUpdate();
                conn.commit();
                _studentCreated = true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    private void prepareCourse(CourseDAO dao, String id, String teacher) throws Exception {
        require(CourseTestData.prepareCourse(
                new Course(id, "排课测试课程", teacher, 2, 20, 0)), "准备测试课程和教学班");
        _createdCourses.add(id);
    }

    /** 只清理本轮已成功创建的夹具。 */
    private void cleanup(CourseDAO courseDAO, SelectCourseDAO selectDAO,
                                CourseScheduleDAO scheduleDAO) throws Exception {
        for (CourseSchedule schedule : scheduleDAO.findByCourseIds(_createdCourses)) {
            scheduleDAO.deleteSchedule(schedule.getScheduleId());
        }
        for (String courseId : _createdCourses) {
            selectDAO.deleteSelectCourse(STUDENT_ID, courseId);
            deleteCourse(courseDAO, courseId);
        }
        if (_studentCreated) {
            CourseTestData.cleanupStudent(USER_ID, STUDENT_ID);
        }
    }

    /** 统计测试残留。 */
    private int residue(CourseDAO courseDAO, SelectCourseDAO selectDAO,
                               CourseScheduleDAO scheduleDAO) throws Exception {
        int count = 0;
        count += courseDAO.findById(COURSE_A) == null ? 0 : 1;
        count += courseDAO.findById(COURSE_B) == null ? 0 : 1;
        count += courseDAO.findById(COURSE_C) == null ? 0 : 1;
        count += selectDAO.findByStudentAndCourse(STUDENT_ID, COURSE_A) == null ? 0 : 1;
        count += selectDAO.findByStudentAndCourse(STUDENT_ID, COURSE_C) == null ? 0 : 1;
        count += scheduleDAO.findByCourseIds(List.of(COURSE_A, COURSE_B, COURSE_C)).size();
        count += CourseTestData.countResidue(USER_ID, STUDENT_ID);
        return count;
    }

    /** 删除存在的测试课程。 */
    private static void deleteCourse(CourseDAO courseDAO, String courseId) throws Exception {
        if (courseDAO.findById(courseId) != null) {
            CourseTestData.cleanupCourse(courseId);
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
