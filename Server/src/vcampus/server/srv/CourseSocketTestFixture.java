/*
 * CourseSocketTestFixture
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
import vcampus.server.dao.DbHelper;
import vcampus.server.dao.SelectCourseDAO;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Course Socket 端到端测试的数据夹具。客户端测试不接触数据库；本服务端工具负责
 * 准备课程、安装仅针对回滚测试课程的故障触发器，并在测试后彻底清理。
 */
public class CourseSocketTestFixture {

    public static final String NORMAL_COURSE_ID = "T_E2E_NORMAL_0904";
    public static final String FULL_COURSE_ID = "T_E2E_FULL_0904";
    public static final String ROLLBACK_COURSE_ID = "T_E2E_ROLL_0904";
    public static final String STUDENT_A = "E2E0904001";
    public static final String STUDENT_B = "E2E0904002";
    private static final String FAILURE_TRIGGER = "trgCourseE2ERollback";

    /**
     * 程序入口：支持 setup、cleanup、residue 三个动作。
     *
     * @param args 第一个参数为动作名
     * @throws Exception 数据库操作失败
     */
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("用法：CourseSocketTestFixture setup|cleanup|residue");
        }
        switch (args[0]) {
            case "setup" -> setup();
            case "cleanup" -> cleanup();
            case "residue" -> printResidue();
            default -> throw new IllegalArgumentException("未知动作：" + args[0]);
        }
    }

    /** 准备三门测试课程及回滚故障触发器。 */
    private static void setup() throws Exception {
        cleanup();
        CourseDAO courseDAO = new CourseDAO();
        courseDAO.insertCourse(new Course(
                NORMAL_COURSE_ID, "Socket 正常课程", "测试教师", 2, 2, 0));
        courseDAO.insertCourse(new Course(
                FULL_COURSE_ID, "Socket 满员课程", "测试教师", 1, 1, 1));
        courseDAO.insertCourse(new Course(
                ROLLBACK_COURSE_ID, "Socket 回滚课程", "测试教师", 1, 2, 0));

        try (Connection conn = DbHelper.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("CREATE TRIGGER " + FAILURE_TRIGGER
                    + " BEFORE UPDATE ON tblCourse FOR EACH ROW "
                    + "BEGIN IF NEW.courseId = '" + ROLLBACK_COURSE_ID + "' THEN "
                    + "SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'forced course e2e rollback'; "
                    + "END IF; END");
        }
        System.out.println("COURSE_SOCKET_FIXTURE_SETUP=PASS");
    }

    /** 删除触发器、选课记录与测试课程。 */
    private static void cleanup() throws Exception {
        try (Connection conn = DbHelper.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DROP TRIGGER IF EXISTS " + FAILURE_TRIGGER);
        }

        SelectCourseDAO selectDAO = new SelectCourseDAO();
        selectDAO.deleteSelectCourse(STUDENT_A, NORMAL_COURSE_ID);
        selectDAO.deleteSelectCourse(STUDENT_A, FULL_COURSE_ID);
        selectDAO.deleteSelectCourse(STUDENT_B, FULL_COURSE_ID);
        selectDAO.deleteSelectCourse(STUDENT_B, ROLLBACK_COURSE_ID);

        CourseDAO courseDAO = new CourseDAO();
        deleteIfPresent(courseDAO, NORMAL_COURSE_ID);
        deleteIfPresent(courseDAO, FULL_COURSE_ID);
        deleteIfPresent(courseDAO, ROLLBACK_COURSE_ID);
        System.out.println("COURSE_SOCKET_FIXTURE_CLEANUP=PASS");
    }

    /** 输出夹具残留数量，非零时以异常结束。 */
    private static void printResidue() throws Exception {
        CourseDAO courseDAO = new CourseDAO();
        SelectCourseDAO selectDAO = new SelectCourseDAO();
        int residue = 0;
        residue += courseDAO.findById(NORMAL_COURSE_ID) == null ? 0 : 1;
        residue += courseDAO.findById(FULL_COURSE_ID) == null ? 0 : 1;
        residue += courseDAO.findById(ROLLBACK_COURSE_ID) == null ? 0 : 1;
        residue += selectDAO.findByStudentAndCourse(STUDENT_A, NORMAL_COURSE_ID) == null ? 0 : 1;
        residue += selectDAO.findByStudentAndCourse(STUDENT_B, ROLLBACK_COURSE_ID) == null ? 0 : 1;

        try (Connection conn = DbHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM information_schema.TRIGGERS "
                     + "WHERE TRIGGER_SCHEMA = DATABASE() AND TRIGGER_NAME = '" + FAILURE_TRIGGER + "'")) {
            if (rs.next()) {
                residue += rs.getInt(1);
            }
        }
        System.out.println("COURSE_SOCKET_TEST_RESIDUE=" + residue);
        if (residue != 0) {
            throw new IllegalStateException("Course Socket 测试存在残留");
        }
    }

    /** 按 ID 删除存在的测试课程。 */
    private static void deleteIfPresent(CourseDAO courseDAO, String courseId) throws Exception {
        if (courseDAO.findById(courseId) != null) {
            courseDAO.deleteCourse(courseId);
        }
    }
}
