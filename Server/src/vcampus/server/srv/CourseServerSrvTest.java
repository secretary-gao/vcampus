/*
 * CourseServerSrvTest
 *
 * Version 1.0
 *
 * 2026-09-04
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.server.srv;

import vcampus.common.vo.Course;
import vcampus.server.dao.CourseDAO;
import vcampus.server.dao.CourseTestData;
import vcampus.server.dao.SelectCourseDAO;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Course Server Service 自测，覆盖成功选课/退课、重复选课、满员校验和第二步失败回滚。
 */
public class CourseServerSrvTest {

    /** 正常流程测试课程。 */
    private static final String TEST_COURSE_ID = "T_SRV_20260904";

    /** 回滚流程测试课程。 */
    private static final String ROLLBACK_COURSE_ID = "T_ROLL_20260904";

    /** 第一位测试学生。 */
    private static final String STUDENT_A = "SRV0904001";

    /** 第二位测试学生。 */
    private static final String STUDENT_B = "SRV0904002";

    private static final String USER_A = "CSRA0904";
    private static final String USER_B = "CSRB0904";

    /**
     * 程序入口。
     *
     * @param args 命令行参数（未使用）
     * @throws Exception 自测失败
     */
    public static void main(String[] args) throws Exception {
        CourseDAO courseDAO = new CourseDAO();
        SelectCourseDAO selectDAO = new SelectCourseDAO();
        CourseServerSrv service = new CourseServerSrv(courseDAO, selectDAO);

        cleanup(courseDAO, selectDAO);
        try {
            CourseTestData.prepareStudent(USER_A, STUDENT_A);
            CourseTestData.prepareStudent(USER_B, STUDENT_B);
            require(CourseTestData.prepareCourse(new Course(
                    TEST_COURSE_ID, "事务测试课程", "测试教师", 2, 1, 0)),
                    "准备容量为 1 的课程");
            require(service.queryCourse("事务测试").stream()
                            .anyMatch(c -> TEST_COURSE_ID.equals(c.getCourseId())),
                    "关键字查询课程");

            require(service.selectCourse(STUDENT_A, TEST_COURSE_ID), "正常选课");
            require(courseDAO.findById(TEST_COURSE_ID).getSelectedCount() == 1,
                    "选课后 selectedCount + 1");
            expectBusinessFailure(() -> service.selectCourse(STUDENT_A, TEST_COURSE_ID),
                    "重复选课失败");
            expectBusinessFailure(() -> service.selectCourse(STUDENT_B, TEST_COURSE_ID),
                    "满员课程选课失败");
            require(service.querySelectedCourse(STUDENT_A).size() == 1,
                    "查询本人已选课程");

            require(service.dropCourse(STUDENT_A, TEST_COURSE_ID), "正常退课");
            require(courseDAO.findById(TEST_COURSE_ID).getSelectedCount() == 0,
                    "退课后 selectedCount - 1");
            require(service.querySelectedCourse(STUDENT_A).isEmpty(),
                    "退课后已选列表为空");

            require(CourseTestData.prepareCourse(new Course(
                    ROLLBACK_COURSE_ID, "回滚测试课程", "测试教师", 1, 2, 0)),
                    "准备回滚测试课程");
            CourseDAO failingCourseDAO = new CourseDAO() {
                @Override
                public void refreshCompatibilityProjection(Connection conn, String courseId)
                        throws SQLException {
                    throw new SQLException("人为制造第二步更新失败");
                }
            };
            CourseServerSrv failingService = new CourseServerSrv(failingCourseDAO, selectDAO);
            boolean sqlFailed = false;
            try {
                failingService.selectCourse(STUDENT_B, ROLLBACK_COURSE_ID);
            } catch (SQLException expected) {
                sqlFailed = expected.getMessage().contains("人为制造第二步更新失败");
            }
            require(sqlFailed, "第二步失败被真实抛出");
            require(selectDAO.findByStudentAndCourse(STUDENT_B, ROLLBACK_COURSE_ID) == null,
                    "第一步 INSERT 已回滚");
            require(courseDAO.findById(ROLLBACK_COURSE_ID).getSelectedCount() == 0,
                    "回滚后 selectedCount 保持 0");

            System.out.println("COURSE_SERVER_SRV_TEST=PASS");
        } finally {
            cleanup(courseDAO, selectDAO);
            int residue = residue(courseDAO, selectDAO);
            System.out.println("COURSE_SERVER_SRV_TEST_RESIDUE=" + residue);
            require(residue == 0, "Service 测试数据清理完成");
        }
    }

    /** 清理所有 Service 测试数据。 */
    private static void cleanup(CourseDAO courseDAO, SelectCourseDAO selectDAO)
            throws SQLException, IOException {
        selectDAO.deleteSelectCourse(STUDENT_A, TEST_COURSE_ID);
        selectDAO.deleteSelectCourse(STUDENT_B, TEST_COURSE_ID);
        selectDAO.deleteSelectCourse(STUDENT_B, ROLLBACK_COURSE_ID);
        if (courseDAO.findById(TEST_COURSE_ID) != null) {
            CourseTestData.cleanupCourse(TEST_COURSE_ID);
        }
        if (courseDAO.findById(ROLLBACK_COURSE_ID) != null) {
            CourseTestData.cleanupCourse(ROLLBACK_COURSE_ID);
        }
        CourseTestData.cleanupStudent(USER_A, STUDENT_A);
        CourseTestData.cleanupStudent(USER_B, STUDENT_B);
    }

    /** 统计测试数据残留数量。 */
    private static int residue(CourseDAO courseDAO, SelectCourseDAO selectDAO)
            throws SQLException, IOException {
        int count = 0;
        count += courseDAO.findById(TEST_COURSE_ID) == null ? 0 : 1;
        count += courseDAO.findById(ROLLBACK_COURSE_ID) == null ? 0 : 1;
        count += selectDAO.findByStudentAndCourse(STUDENT_A, TEST_COURSE_ID) == null ? 0 : 1;
        count += selectDAO.findByStudentAndCourse(STUDENT_B, ROLLBACK_COURSE_ID) == null ? 0 : 1;
        count += CourseTestData.countResidue(USER_A, STUDENT_A);
        count += CourseTestData.countResidue(USER_B, STUDENT_B);
        return count;
    }

    /** 断言动作抛出可预期的业务异常。 */
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

    /** 断言测试步骤成功。 */
    private static void require(boolean condition, String description) {
        if (!condition) {
            throw new IllegalStateException("FAIL: " + description);
        }
        System.out.println("PASS: " + description);
    }

    /** 可抛异常的测试动作。 */
    @FunctionalInterface
    private interface CheckedAction {
        void run() throws Exception;
    }
}
