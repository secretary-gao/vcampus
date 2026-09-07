/*
 * CourseDAOTest
 *
 * Version 1.0
 *
 * 2026-09-04
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.server.dao;

import vcampus.common.vo.Course;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * {@link CourseDAO} 的可运行自测：验证课程的插入、单条查询、全部查询、修改和删除。
 * 测试使用固定专用 ID，并在 {@code finally} 中清理，避免污染正式数据库。
 *
 * <p>运行前请确认：已执行 {@code sql/course/vcampus_course.sql}；已正确配置
 * {@code Server/db.properties}；运行时的工作目录为项目根目录。</p>
 */
public class CourseDAOTest {

    /** 本测试专用课程号。 */
    private static final String TEST_COURSE_ID = "T_CDAO_20260904";

    /**
     * 程序入口：执行 CourseDAO 完整 CRUD 自测。
     *
     * @param args 命令行参数（未使用）
     * @throws SQLException 数据库操作或清理失败
     * @throws IOException  数据库配置文件读取失败
     */
    public static void main(String[] args) throws SQLException, IOException {
        CourseDAO dao = new CourseDAO();

        cleanup(dao);
        try {
            Course course = new Course(
                    TEST_COURSE_ID, "DAO 测试课程", "测试教师", 2, 30, 0);

            require(dao.insertCourse(course), "插入测试课程");

            Course queried = dao.findById(TEST_COURSE_ID);
            require(queried != null && "DAO 测试课程".equals(queried.getCourseName()),
                    "findById 查询到刚插入的课程");

            List<Course> courses = dao.findAll();
            require(courses.stream().anyMatch(c -> TEST_COURSE_ID.equals(c.getCourseId())),
                    "findAll 包含测试课程");

            queried.setCapacity(45);
            require(dao.updateCourse(queried), "修改课程容量");
            Course updated = dao.findById(TEST_COURSE_ID);
            require(updated != null && updated.getCapacity() == 45,
                    "回查确认课程容量为 45");

            require(dao.deleteCourse(TEST_COURSE_ID), "删除测试课程");
            require(dao.findById(TEST_COURSE_ID) == null, "删除后确实查不到课程");

            System.out.println("COURSE_DAO_TEST=PASS");
        } finally {
            cleanup(dao);
            int residue = dao.findById(TEST_COURSE_ID) == null ? 0 : 1;
            System.out.println("COURSE_DAO_TEST_RESIDUE=" + residue);
            if (residue != 0) {
                throw new IllegalStateException("CourseDAOTest 清理失败");
            }
        }
    }

    /**
     * 清理本测试专用课程。
     *
     * @param dao CourseDAO 实例
     * @throws SQLException 数据库操作异常
     * @throws IOException  数据库配置文件读取异常
     */
    private static void cleanup(CourseDAO dao) throws SQLException, IOException {
        if (dao.findById(TEST_COURSE_ID) != null) {
            dao.deleteCourse(TEST_COURSE_ID);
        }
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
