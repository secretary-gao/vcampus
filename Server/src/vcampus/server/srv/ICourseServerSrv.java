/*
 * ICourseServerSrv
 *
 * Version 1.0
 *
 * 2026-09-04
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.server.srv;

import vcampus.common.vo.Course;
import vcampus.common.vo.SelectCourse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * 选课模块服务器端业务接口，定义课程查询、选课、退课和已选课程查询能力。
 */
public interface ICourseServerSrv {

    /**
     * 按课程号、课程名或教师查询课程。
     *
     * @param keyword 查询关键字；空值表示全部课程
     * @return 匹配的课程列表
     * @throws SQLException 数据库操作异常
     * @throws IOException  数据库配置文件读取异常
     */
    List<Course> queryCourse(String keyword) throws SQLException, IOException;

    /**
     * 为学生选择课程。
     *
     * @param studentId 学号
     * @param courseId  课程号
     * @return 选课成功返回 {@code true}
     * @throws SQLException          数据库操作异常
     * @throws IOException           数据库配置文件读取异常
     * @throws CourseServiceException 参数或业务规则不满足
     */
    boolean selectCourse(String studentId, String courseId)
            throws SQLException, IOException, CourseServiceException;

    /**
     * 为学生退选课程。
     *
     * @param studentId 学号
     * @param courseId  课程号
     * @return 退课成功返回 {@code true}
     * @throws SQLException          数据库操作异常
     * @throws IOException           数据库配置文件读取异常
     * @throws CourseServiceException 参数或业务规则不满足
     */
    boolean dropCourse(String studentId, String courseId)
            throws SQLException, IOException, CourseServiceException;

    /**
     * 查询学生的全部选课记录。
     *
     * @param studentId 学号
     * @return 选课记录列表
     * @throws SQLException          数据库操作异常
     * @throws IOException           数据库配置文件读取异常
     * @throws CourseServiceException 学号无效
     */
    List<SelectCourse> querySelectedCourse(String studentId)
            throws SQLException, IOException, CourseServiceException;
}
