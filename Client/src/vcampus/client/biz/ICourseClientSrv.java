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
import vcampus.common.vo.AutoSchedulePlan;
import vcampus.common.vo.AutoScheduleRequest;
import vcampus.common.vo.CourseSchedule;
import vcampus.common.vo.CourseRequirementGroup;
import vcampus.common.vo.CourseDashboardStats;
import vcampus.common.vo.SelectCourse;
import vcampus.common.vo.TeacherCourseEnrollment;
import vcampus.common.vo.TeachingClass;
import vcampus.common.vo.User;
import vcampus.common.vo.CourseScore;

import java.io.IOException;
import java.util.List;

/** 客户端选课业务接口，封装全部 Course Socket 调用。 */
public interface ICourseClientSrv {

    /** 查询课程。 */
    List<Course> queryCourse(String keyword) throws IOException, ClassNotFoundException;

    List<TeachingClass> queryTeachingClass(String keyword)
            throws IOException, ClassNotFoundException;

    List<CourseRequirementGroup> queryRequirementGroups()
            throws IOException, ClassNotFoundException;

    AutoSchedulePlan previewAutoSchedule(AutoScheduleRequest request)
            throws IOException, ClassNotFoundException;

    int applyAutoSchedule(AutoSchedulePlan plan)
            throws IOException, ClassNotFoundException;

    int validateAutoSchedule(AutoSchedulePlan plan)
            throws IOException, ClassNotFoundException;

    int loadAutoScheduleDemoData() throws IOException, ClassNotFoundException;

    CourseDashboardStats queryDashboard() throws IOException, ClassNotFoundException;

    TeachingClass addTeachingClass(TeachingClass teachingClass)
            throws IOException, ClassNotFoundException;

    boolean updateTeachingClass(TeachingClass teachingClass)
            throws IOException, ClassNotFoundException;

    boolean deleteTeachingClass(String teachingClassId)
            throws IOException, ClassNotFoundException;

    /** 新增课程。 */
    Course addCourse(Course course) throws IOException, ClassNotFoundException;

    /** 修改课程。 */
    boolean updateCourse(Course course) throws IOException, ClassNotFoundException;

    /** 删除课程。 */
    boolean deleteCourse(String courseId) throws IOException, ClassNotFoundException;

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

    /** 查询教师本人课程及选课学生名单。 */
    List<TeacherCourseEnrollment> queryTeacherCourseEnrollments(User currentUser)
            throws IOException, ClassNotFoundException;

    List<CourseScore> queryStudentScores(String studentId) throws IOException, ClassNotFoundException;
    List<CourseScore> queryTeacherScores(String teacher) throws IOException, ClassNotFoundException;
    int submitScores(String teacher, List<CourseScore> scores) throws IOException, ClassNotFoundException;
    List<CourseScore> queryPendingScores() throws IOException, ClassNotFoundException;
    boolean reviewScore(String scoreId, boolean approved) throws IOException, ClassNotFoundException;
}
