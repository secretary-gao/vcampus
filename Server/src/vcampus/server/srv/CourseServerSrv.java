/*
 * CourseServerSrv
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
import vcampus.common.vo.SelectCourse;
import vcampus.common.vo.TeacherCourseEnrollment;
import vcampus.server.dao.CourseDAO;
import vcampus.server.dao.CourseScheduleDAO;
import vcampus.server.dao.CourseStudentDAO;
import vcampus.server.dao.DbHelper;
import vcampus.server.dao.SelectCourseDAO;
import vcampus.server.dao.TeacherCourseEnrollmentDAO;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * {@link ICourseServerSrv} 的实现类。课程查询直接委托 DAO；选课和退课负责业务校验，
 * 并在同一个 JDBC 事务内同步修改选课记录与课程已选人数。
 */
public class CourseServerSrv implements ICourseServerSrv {

    /** 课程数据访问对象。 */
    private final CourseDAO _courseDAO;

    /** 选课记录数据访问对象。 */
    private final SelectCourseDAO _selectCourseDAO;

    /** 排课数据访问对象。 */
    private final CourseScheduleDAO _courseScheduleDAO;

    /** 登录用户与正式学号映射查询。 */
    private final CourseStudentDAO _courseStudentDAO = new CourseStudentDAO();

    /** 教师课程名单查询。 */
    private final TeacherCourseEnrollmentDAO _teacherEnrollmentDAO =
            new TeacherCourseEnrollmentDAO();

    /**
     * 使用默认 DAO 创建业务服务。
     */
    public CourseServerSrv() {
        this(new CourseDAO(), new SelectCourseDAO(), new CourseScheduleDAO());
    }

    /**
     * 注入 DAO，便于独立验证事务回滚。
     *
     * @param courseDAO       课程 DAO
     * @param selectCourseDAO 选课记录 DAO
     */
    CourseServerSrv(CourseDAO courseDAO, SelectCourseDAO selectCourseDAO) {
        this(courseDAO, selectCourseDAO, new CourseScheduleDAO());
    }

    /** 注入全部 DAO，供业务层测试复用。 */
    CourseServerSrv(CourseDAO courseDAO, SelectCourseDAO selectCourseDAO,
                    CourseScheduleDAO courseScheduleDAO) {
        this._courseDAO = courseDAO;
        this._selectCourseDAO = selectCourseDAO;
        this._courseScheduleDAO = courseScheduleDAO;
    }

    /** {@inheritDoc} */
    @Override
    public List<Course> queryCourse(String keyword) throws SQLException, IOException {
        return _courseDAO.findByKeyword(keyword);
    }

    /** {@inheritDoc} */
    @Override
    public Course addCourse(Course course)
            throws SQLException, IOException, CourseServiceException {
        validateCourse(course);
        course.setSelectedCount(0);
        if (_courseDAO.findById(course.getCourseId()) != null) {
            throw new CourseServiceException("课程号已存在：" + course.getCourseId());
        }
        try {
            if (!_courseDAO.insertCourse(course)) {
                throw new SQLException("插入课程失败");
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new CourseServiceException("课程号已存在：" + course.getCourseId());
        }
        return course;
    }

    /** {@inheritDoc} */
    @Override
    public boolean updateCourse(Course course)
            throws SQLException, IOException, CourseServiceException {
        validateCourse(course);
        try (Connection conn = DbHelper.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Course current = _courseDAO.findByIdForUpdate(conn, course.getCourseId());
                if (current == null) {
                    throw new CourseServiceException("课程不存在：" + course.getCourseId());
                }
                if (course.getCapacity() < current.getSelectedCount()) {
                    throw new CourseServiceException("课程容量不能小于已选人数");
                }
                if (!course.getTeacher().equals(current.getTeacher())) {
                    for (CourseSchedule schedule : _courseScheduleDAO.findByCourseIds(
                            conn, Set.of(course.getCourseId()))) {
                        if (_courseScheduleDAO.hasTeacherConflict(
                                conn, schedule, course.getTeacher(), schedule.getScheduleId())) {
                            throw new CourseServiceException("修改教师后将产生排课时间冲突");
                        }
                    }
                }
                course.setSelectedCount(current.getSelectedCount());
                if (!_courseDAO.updateCourse(conn, course)) {
                    throw new SQLException("更新课程失败");
                }
                conn.commit();
                return true;
            } catch (SQLException | CourseServiceException | RuntimeException e) {
                rollback(conn, e);
                throw e;
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean deleteCourse(String courseId)
            throws SQLException, IOException, CourseServiceException {
        if (courseId == null || courseId.isBlank()) {
            throw new CourseServiceException("课程号不能为空");
        }
        String normalizedCourseId = courseId.trim();
        try (Connection conn = DbHelper.getConnection()) {
            conn.setAutoCommit(false);
            try {
                if (_courseDAO.findByIdForUpdate(conn, normalizedCourseId) == null) {
                    throw new CourseServiceException("课程不存在：" + normalizedCourseId);
                }
                if (!_selectCourseDAO.findByCourseId(conn, normalizedCourseId).isEmpty()) {
                    throw new CourseServiceException("课程已有学生选课，不能删除");
                }
                if (!_courseScheduleDAO.findByCourseIds(
                        conn, Set.of(normalizedCourseId)).isEmpty()) {
                    throw new CourseServiceException("课程已有排课，不能删除");
                }
                if (!_courseDAO.deleteCourse(conn, normalizedCourseId)) {
                    throw new SQLException("删除课程失败");
                }
                conn.commit();
                return true;
            } catch (SQLException | CourseServiceException | RuntimeException e) {
                rollback(conn, e);
                throw e;
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean selectCourse(String studentId, String courseId)
            throws SQLException, IOException, CourseServiceException {
        validateIds(studentId, courseId);
        String normalizedStudentId = studentId.trim();
        String normalizedCourseId = courseId.trim();

        try (Connection conn = DbHelper.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Course course = _courseDAO.findByIdForUpdate(conn, normalizedCourseId);
                if (course == null) {
                    throw new CourseServiceException("课程不存在：" + normalizedCourseId);
                }
                if (_selectCourseDAO.findByStudentAndCourse(
                        conn, normalizedStudentId, normalizedCourseId) != null) {
                    throw new CourseServiceException("请勿重复选择同一门课程");
                }
                if (course.getSelectedCount() >= course.getCapacity()) {
                    throw new CourseServiceException("课程已满");
                }

                SelectCourse record = new SelectCourse(
                        newSelectId(), normalizedStudentId, normalizedCourseId, LocalDateTime.now());
                if (!_selectCourseDAO.insertSelectCourse(conn, record)) {
                    throw new SQLException("插入选课记录失败");
                }

                course.setSelectedCount(course.getSelectedCount() + 1);
                if (!_courseDAO.updateCourse(conn, course)) {
                    throw new SQLException("更新课程已选人数失败");
                }

                conn.commit();
                return true;
            } catch (SQLException | CourseServiceException | RuntimeException e) {
                rollback(conn, e);
                throw e;
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean dropCourse(String studentId, String courseId)
            throws SQLException, IOException, CourseServiceException {
        validateIds(studentId, courseId);
        String normalizedStudentId = studentId.trim();
        String normalizedCourseId = courseId.trim();

        try (Connection conn = DbHelper.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Course course = _courseDAO.findByIdForUpdate(conn, normalizedCourseId);
                if (course == null) {
                    throw new CourseServiceException("课程不存在：" + normalizedCourseId);
                }
                if (_selectCourseDAO.findByStudentAndCourse(
                        conn, normalizedStudentId, normalizedCourseId) == null) {
                    throw new CourseServiceException("未找到对应选课记录");
                }
                if (course.getSelectedCount() <= 0) {
                    throw new CourseServiceException("课程已选人数异常，不能继续退课");
                }

                if (!_selectCourseDAO.deleteSelectCourse(
                        conn, normalizedStudentId, normalizedCourseId)) {
                    throw new SQLException("删除选课记录失败");
                }
                course.setSelectedCount(course.getSelectedCount() - 1);
                if (!_courseDAO.updateCourse(conn, course)) {
                    throw new SQLException("更新课程已选人数失败");
                }

                conn.commit();
                return true;
            } catch (SQLException | CourseServiceException | RuntimeException e) {
                rollback(conn, e);
                throw e;
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public List<SelectCourse> querySelectedCourse(String studentId)
            throws SQLException, IOException, CourseServiceException {
        if (studentId == null || studentId.isBlank()) {
            throw new CourseServiceException("学号不能为空");
        }
        return _selectCourseDAO.findByStudentId(studentId.trim());
    }

    /** {@inheritDoc} */
    @Override
    public String queryStudentId(String userId)
            throws SQLException, IOException, CourseServiceException {
        if (userId == null || userId.isBlank()) {
            throw new CourseServiceException("用户 ID 不能为空");
        }
        return _courseStudentDAO.findStudentIdByUserId(userId.trim());
    }

    /** {@inheritDoc} */
    @Override
    public List<CourseSchedule> querySchedule() throws SQLException, IOException {
        return _courseScheduleDAO.findAll();
    }

    /** {@inheritDoc} */
    @Override
    public CourseSchedule addSchedule(CourseSchedule schedule)
            throws SQLException, IOException, CourseServiceException {
        validateSchedule(schedule, false);
        if (schedule.getScheduleId() == null || schedule.getScheduleId().isBlank()) {
            schedule.setScheduleId(newScheduleId());
        }

        try (Connection conn = DbHelper.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Course course = requireCourseForSchedule(conn, schedule.getCourseId());
                checkScheduleConflicts(conn, schedule, course.getTeacher(), null);
                if (!_courseScheduleDAO.insertSchedule(conn, schedule)) {
                    throw new SQLException("插入排课记录失败");
                }
                conn.commit();
                return schedule;
            } catch (SQLException | CourseServiceException | RuntimeException e) {
                rollback(conn, e);
                throw e;
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean updateSchedule(CourseSchedule schedule)
            throws SQLException, IOException, CourseServiceException {
        validateSchedule(schedule, true);
        try (Connection conn = DbHelper.getConnection()) {
            conn.setAutoCommit(false);
            try {
                if (_courseScheduleDAO.findById(conn, schedule.getScheduleId()) == null) {
                    throw new CourseServiceException("排课记录不存在");
                }
                Course course = requireCourseForSchedule(conn, schedule.getCourseId());
                checkScheduleConflicts(
                        conn, schedule, course.getTeacher(), schedule.getScheduleId());
                if (!_courseScheduleDAO.updateSchedule(conn, schedule)) {
                    throw new SQLException("更新排课记录失败");
                }
                conn.commit();
                return true;
            } catch (SQLException | CourseServiceException | RuntimeException e) {
                rollback(conn, e);
                throw e;
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean deleteSchedule(String scheduleId)
            throws SQLException, IOException, CourseServiceException {
        if (scheduleId == null || scheduleId.isBlank()) {
            throw new CourseServiceException("排课记录号不能为空");
        }
        if (!_courseScheduleDAO.deleteSchedule(scheduleId.trim())) {
            throw new CourseServiceException("排课记录不存在");
        }
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public List<CourseSchedule> queryStudentSchedule(String studentId)
            throws SQLException, IOException, CourseServiceException {
        List<SelectCourse> selected = querySelectedCourse(studentId);
        Set<String> courseIds = new LinkedHashSet<>();
        for (SelectCourse record : selected) {
            courseIds.add(record.getCourseId());
        }
        return _courseScheduleDAO.findByCourseIds(courseIds);
    }

    /** {@inheritDoc} */
    @Override
    public List<TeacherCourseEnrollment> queryTeacherCourseEnrollments(String teacherName)
            throws SQLException, IOException, CourseServiceException {
        if (teacherName == null || teacherName.isBlank()) {
            throw new CourseServiceException("教师姓名不能为空");
        }
        return _teacherEnrollmentDAO.findByTeacher(teacherName.trim());
    }

    /** 校验并规范化课程主数据。 */
    private void validateCourse(Course course) throws CourseServiceException {
        if (course == null) {
            throw new CourseServiceException("课程信息不能为空");
        }
        String courseId = normalizeCourseText(course.getCourseId(), "课程号", 20);
        String courseName = normalizeCourseText(course.getCourseName(), "课程名称", 50);
        String teacher = normalizeCourseText(course.getTeacher(), "授课教师", 20);
        if (course.getCredit() <= 0) {
            throw new CourseServiceException("学分必须大于 0");
        }
        if (course.getCapacity() <= 0) {
            throw new CourseServiceException("课程容量必须大于 0");
        }
        course.setCourseId(courseId);
        course.setCourseName(courseName);
        course.setTeacher(teacher);
    }

    /** 规范化必填文本并匹配数据库长度限制。 */
    private String normalizeCourseText(String value, String fieldName, int maxLength)
            throws CourseServiceException {
        if (value == null || value.isBlank()) {
            throw new CourseServiceException(fieldName + "不能为空");
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new CourseServiceException(fieldName + "不能超过 " + maxLength + " 个字符");
        }
        return normalized;
    }

    /** 校验排课字段。 */
    private void validateSchedule(CourseSchedule schedule, boolean requireId)
            throws CourseServiceException {
        if (schedule == null) {
            throw new CourseServiceException("排课信息不能为空");
        }
        if (requireId && (schedule.getScheduleId() == null
                || schedule.getScheduleId().isBlank())) {
            throw new CourseServiceException("排课记录号不能为空");
        }
        if (schedule.getCourseId() == null || schedule.getCourseId().isBlank()) {
            throw new CourseServiceException("课程号不能为空");
        }
        if (schedule.getClassroom() == null || schedule.getClassroom().isBlank()) {
            throw new CourseServiceException("教室不能为空");
        }
        if (schedule.getDayOfWeek() < 1 || schedule.getDayOfWeek() > 7) {
            throw new CourseServiceException("星期必须在 1 到 7 之间");
        }
        if (schedule.getStartTime() == null || schedule.getEndTime() == null
                || !schedule.getStartTime().isBefore(schedule.getEndTime())) {
            throw new CourseServiceException("开始时间必须早于结束时间");
        }
        schedule.setCourseId(schedule.getCourseId().trim());
        schedule.setClassroom(schedule.getClassroom().trim());
        if (schedule.getScheduleId() != null) {
            schedule.setScheduleId(schedule.getScheduleId().trim());
        }
    }

    /** 查询并锁定排课引用的课程。 */
    private Course requireCourseForSchedule(Connection conn, String courseId)
            throws SQLException, CourseServiceException {
        Course course = _courseDAO.findByIdForUpdate(conn, courseId);
        if (course == null) {
            throw new CourseServiceException("课程不存在：" + courseId);
        }
        return course;
    }

    /** 检查教室和教师的时间区间冲突。 */
    private void checkScheduleConflicts(Connection conn, CourseSchedule schedule,
                                        String teacher, String excludeScheduleId)
            throws SQLException, CourseServiceException {
        if (_courseScheduleDAO.hasClassroomConflict(conn, schedule, excludeScheduleId)) {
            throw new CourseServiceException("该教室在所选时间已有课程");
        }
        if (_courseScheduleDAO.hasTeacherConflict(
                conn, schedule, teacher, excludeScheduleId)) {
            throw new CourseServiceException("该教师在所选时间已有课程");
        }
    }

    /** 生成不超过 varchar(20) 的排课记录号。 */
    private String newScheduleId() {
        return "CSH" + UUID.randomUUID().toString().replace("-", "").substring(0, 17);
    }

    /** 校验选课和退课使用的业务标识。 */
    private void validateIds(String studentId, String courseId) throws CourseServiceException {
        if (studentId == null || studentId.isBlank()) {
            throw new CourseServiceException("学号不能为空");
        }
        if (courseId == null || courseId.isBlank()) {
            throw new CourseServiceException("课程号不能为空");
        }
    }

    /** 生成不超过 varchar(20) 的选课记录号。 */
    private String newSelectId() {
        return "SC" + UUID.randomUUID().toString().replace("-", "").substring(0, 18);
    }

    /** 回滚事务；若回滚本身失败，将异常附加到原异常上。 */
    private void rollback(Connection conn, Exception cause) {
        try {
            conn.rollback();
        } catch (SQLException rollbackError) {
            cause.addSuppressed(rollbackError);
        }
    }
}
