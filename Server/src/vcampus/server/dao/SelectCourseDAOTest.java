/*
 * SelectCourseDAOTest
 *
 * Version 1.0
 *
 * 2026-09-04
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.server.dao;

import vcampus.common.vo.Course;
import vcampus.common.vo.SelectCourse;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * {@link SelectCourseDAO} 的可运行自测：验证选课记录的插入、组合查询、列表查询、
 * 删除以及数据库 UNIQUE(studentId, courseId) 约束。测试数据始终在
 * {@code finally} 中清理，避免污染正式数据库。
 *
 * <p>测试同时准备正式 {@code tblStudent} 夹具，验证选课记录满足学生外键。</p>
 */
public class SelectCourseDAOTest {

    /** 本测试专用课程号。 */
    private static final String TEST_COURSE_ID = "T_SCDAO_20260904";

    /** 本测试专用学号。 */
    private static final String TEST_STUDENT_ID = "TST0904001";

    /** 本测试专用用户 ID。 */
    private static final String TEST_USER_ID = "SCDA0904";

    /** 本测试第一条选课记录号。 */
    private static final String TEST_SELECT_ID = "T_SEL_20260904";

    /** 重复选课尝试使用的不同记录号。 */
    private static final String DUPLICATE_SELECT_ID = "T_DUP_20260904";

    /**
     * 程序入口：执行 SelectCourseDAO 完整自测。
     *
     * @param args 命令行参数（未使用）
     * @throws SQLException 数据库操作或清理失败
     * @throws IOException  数据库配置文件读取失败
     */
    public static void main(String[] args) throws SQLException, IOException {
        CourseDAO courseDAO = new CourseDAO();
        SelectCourseDAO selectCourseDAO = new SelectCourseDAO();

        cleanup(courseDAO, selectCourseDAO);
        try {
            CourseTestData.prepareStudent(TEST_USER_ID, TEST_STUDENT_ID);
            Course course = new Course(
                    TEST_COURSE_ID, "选课 DAO 测试课程", "测试教师", 1, 5, 0);
            require(CourseTestData.prepareCourse(course), "准备测试课程和教学班");

            LocalDateTime selectTime = LocalDateTime.of(2026, 9, 4, 10, 30);
            SelectCourse record = new SelectCourse(
                    TEST_SELECT_ID, TEST_STUDENT_ID, TEST_COURSE_ID, selectTime);
            require(selectCourseDAO.insertSelectCourse(record), "插入选课记录");

            SelectCourse queried = selectCourseDAO.findByStudentAndCourse(
                    TEST_STUDENT_ID, TEST_COURSE_ID);
            require(queried != null
                            && TEST_SELECT_ID.equals(queried.getSelectId())
                            && selectTime.equals(queried.getSelectTime()),
                    "findByStudentAndCourse 查询并正确映射时间");

            List<SelectCourse> studentRecords =
                    selectCourseDAO.findByStudentId(TEST_STUDENT_ID);
            require(studentRecords.stream()
                            .anyMatch(r -> TEST_SELECT_ID.equals(r.getSelectId())),
                    "findByStudentId 包含测试记录");

            List<SelectCourse> courseRecords =
                    selectCourseDAO.findByCourseId(TEST_COURSE_ID);
            require(courseRecords.stream()
                            .anyMatch(r -> TEST_SELECT_ID.equals(r.getSelectId())),
                    "findByCourseId 包含测试记录");

            SelectCourse duplicate = new SelectCourse(
                    DUPLICATE_SELECT_ID, TEST_STUDENT_ID, TEST_COURSE_ID,
                    selectTime.plusMinutes(1));
            boolean duplicateRejected = false;
            try {
                selectCourseDAO.insertSelectCourse(duplicate);
            } catch (SQLException expected) {
                duplicateRejected = expected.getErrorCode() == 1062
                        && "23000".equals(expected.getSQLState());
            }
            require(duplicateRejected,
                    "数据库拒绝重复 studentId + courseId（UNIQUE，错误码 1062）");

            require(selectCourseDAO.deleteSelectCourse(TEST_STUDENT_ID, TEST_COURSE_ID),
                    "删除选课记录");
            require(selectCourseDAO.findByStudentAndCourse(
                    TEST_STUDENT_ID, TEST_COURSE_ID) == null, "删除后确实查不到选课记录");

            System.out.println("SELECT_COURSE_DAO_TEST=PASS");
        } finally {
            cleanup(courseDAO, selectCourseDAO);
            int residue = 0;
            if (selectCourseDAO.findByStudentAndCourse(
                    TEST_STUDENT_ID, TEST_COURSE_ID) != null) {
                residue++;
            }
            if (courseDAO.findById(TEST_COURSE_ID) != null) {
                residue++;
            }
            residue += CourseTestData.countResidue(TEST_USER_ID, TEST_STUDENT_ID);
            System.out.println("SELECT_COURSE_DAO_TEST_RESIDUE=" + residue);
            if (residue != 0) {
                throw new IllegalStateException("SelectCourseDAOTest 清理失败");
            }
        }
    }

    /**
     * 按精确测试 ID 清理选课记录和课程，保证测试可重复运行。
     *
     * @param courseDAO CourseDAO 实例
     * @param selectCourseDAO SelectCourseDAO 实例
     * @throws SQLException 数据库操作异常
     * @throws IOException  数据库配置文件读取异常
     */
    private static void cleanup(CourseDAO courseDAO, SelectCourseDAO selectCourseDAO)
            throws SQLException, IOException {
        selectCourseDAO.deleteSelectCourse(TEST_STUDENT_ID, TEST_COURSE_ID);
        if (courseDAO.findById(TEST_COURSE_ID) != null) {
            CourseTestData.cleanupCourse(TEST_COURSE_ID);
        }
        CourseTestData.cleanupStudent(TEST_USER_ID, TEST_STUDENT_ID);
    }

    /**
     * 断言一个测试步骤成功，并打印真实 PASS 结果。
     *
     * @param condition 测试条件
     * @param description 测试步骤说明
     */
    private static void require(boolean condition, String description) {
        if (!condition) {
            throw new IllegalStateException("FAIL: " + description);
        }
        System.out.println("PASS: " + description);
    }
}
