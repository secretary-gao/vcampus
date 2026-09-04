/*
 * ICourseClientSrv
 *
 * Version 1.0
 *
 * 2026-09-04
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.client.biz;

import vcampus.common.vo.Course;
import vcampus.common.vo.CourseSchedule;
import vcampus.common.vo.SelectCourse;

import java.io.IOException;
import java.util.List;

/** 客户端选课业务接口，封装全部 Course Socket 调用。 */
public interface ICourseClientSrv {

    /** 查询课程。 */
    List<Course> queryCourse(String keyword) throws IOException, ClassNotFoundException;

    /** 学生选课。 */
    boolean selectCourse(String studentId, String courseId)
            throws IOException, ClassNotFoundException;

    /** 学生退课。 */
    boolean dropCourse(String studentId, String courseId)
            throws IOException, ClassNotFoundException;

    /** 查询学生已选课程。 */
    List<SelectCourse> querySelectedCourse(String studentId)
            throws IOException, ClassNotFoundException;

    /** 查询登录用户对应的正式学号。 */
    String queryStudentId(String userId) throws IOException, ClassNotFoundException;

    /** 查询全部排课。 */
    List<CourseSchedule> querySchedule() throws IOException, ClassNotFoundException;

    /** 新增排课。 */
    CourseSchedule addSchedule(CourseSchedule schedule)
            throws IOException, ClassNotFoundException;

    /** 修改排课。 */
    boolean updateSchedule(CourseSchedule schedule)
            throws IOException, ClassNotFoundException;

    /** 删除排课。 */
    boolean deleteSchedule(String scheduleId)
            throws IOException, ClassNotFoundException;

    /** 查询学生已选课程对应的课程表。 */
    List<CourseSchedule> queryStudentSchedule(String studentId)
            throws IOException, ClassNotFoundException;
}
