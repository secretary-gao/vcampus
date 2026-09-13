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
import vcampus.common.vo.AutoSchedulePlan;
import vcampus.common.vo.AutoScheduleRequest;
import vcampus.common.vo.CourseSchedule;
import vcampus.common.vo.CourseRequirementGroup;
import vcampus.common.vo.SelectCourse;
import vcampus.common.vo.TeacherCourseEnrollment;
import vcampus.common.vo.TeachingClass;
import vcampus.server.dao.CourseDAO;
import vcampus.server.dao.CourseScheduleDAO;
import vcampus.server.dao.CourseRequirementGroupDAO;
import vcampus.server.dao.CourseStudentDAO;
import vcampus.server.dao.DbHelper;
import vcampus.server.dao.SelectCourseDAO;
import vcampus.server.dao.TeacherCourseEnrollmentDAO;
import vcampus.server.dao.TeachingClassDAO;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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

    private final CourseRequirementGroupDAO _requirementGroupDAO =
            new CourseRequirementGroupDAO();

    /** Concrete teaching-class persistence. */
    private final TeachingClassDAO _teachingClassDAO = new TeachingClassDAO();

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
        List<Course> courses = _courseDAO.findByKeyword(keyword);
        for (Course course : courses) {
            List<TeachingClass> classes = _teachingClassDAO.findByCourseId(course.getCourseId());
            if (!classes.isEmpty()) {
                course.setTeacher(classes.get(0).getTeacher());
                course.setCapacity(classes.stream().mapToInt(TeachingClass::getCapacity).sum());
                course.setSelectedCount(
                        classes.stream().mapToInt(TeachingClass::getSelectedCount).sum());
            }
        }
        return courses;
    }

    @Override
    public List<TeachingClass> queryTeachingClass(String keyword)
            throws SQLException, IOException {
        return _teachingClassDAO.findByKeyword(keyword);
    }

    @Override
    public List<CourseRequirementGroup> queryRequirementGroups()
            throws SQLException, IOException {
        return _requirementGroupDAO.findAll();
    }

    @Override
    public AutoSchedulePlan previewAutoSchedule(AutoScheduleRequest request)
            throws SQLException, IOException, CourseServiceException {
        if (request == null || request.getWeekStart() < 1
                || request.getWeekEnd() > 30
                || request.getWeekStart() > request.getWeekEnd()) {
            throw new CourseServiceException("自动排课周次范围无效");
        }
        List<TeachingClass> allClasses = _teachingClassDAO.findByKeyword("");
        List<CourseSchedule> existing = _courseScheduleDAO.findAll();
        Set<String> scheduled = existing.stream().map(CourseSchedule::getTeachingClassId)
                .collect(java.util.stream.Collectors.toSet());
        Set<String> requested = request.getTeachingClassIds() == null
                ? Set.of() : new LinkedHashSet<>(request.getTeachingClassIds());
        List<TeachingClass> targets = allClasses.stream()
                .filter(value -> requested.isEmpty()
                        ? !scheduled.contains(value.getTeachingClassId())
                        : requested.contains(value.getTeachingClassId()))
                .toList();
        if (targets.isEmpty()) {
            throw new CourseServiceException("没有需要自动排课的教学班");
        }
        if (targets.stream().anyMatch(value -> scheduled.contains(value.getTeachingClassId()))) {
            throw new CourseServiceException("自动排课只支持尚未排课的教学班");
        }
        if (!requested.isEmpty() && targets.size() != requested.size()) {
            throw new CourseServiceException("请求包含不存在的教学班");
        }
        return new CourseAutoScheduler().generate(targets, allClasses, existing,
                request.getWeekStart(), request.getWeekEnd());
    }

    @Override
    public int applyAutoSchedule(AutoSchedulePlan plan)
            throws SQLException, IOException, CourseServiceException {
        if (plan == null || plan.getAssignments() == null
                || plan.getAssignments().isEmpty()) {
            throw new CourseServiceException("自动排课方案为空，无法应用");
        }
        try (Connection conn = DbHelper.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Set<String> classIds = new LinkedHashSet<>();
                for (CourseSchedule schedule : plan.getAssignments()) {
                    validateSchedule(schedule, true);
                    if (!classIds.add(schedule.getTeachingClassId())) {
                        throw new CourseServiceException("方案中教学班重复："
                                + schedule.getTeachingClassId());
                    }
                    TeachingClass teachingClass = resolveScheduleTeachingClass(conn, schedule);
                    boolean alreadyScheduled = _courseScheduleDAO.findAll(conn).stream()
                            .anyMatch(value -> value.getTeachingClassId().equals(
                                    schedule.getTeachingClassId()));
                    if (alreadyScheduled) {
                        throw new CourseServiceException("教学班已存在排课，预览已过期："
                                + schedule.getTeachingClassId());
                    }
                    checkScheduleConflicts(conn, schedule, teachingClass.getTeacher(), null);
                    if (!_courseScheduleDAO.insertSchedule(conn, schedule)) {
                        throw new SQLException("写入自动排课失败");
                    }
                }
                conn.commit();
                return plan.getAssignments().size();
            } catch (SQLException | CourseServiceException | RuntimeException exception) {
                rollback(conn, exception);
                throw exception;
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public Course addCourse(Course course)
            throws SQLException, IOException, CourseServiceException {
        validateCourse(course);
        course.setSelectedCount(0);
        try (Connection conn = DbHelper.getConnection()) {
            conn.setAutoCommit(false);
            try {
                if (_courseDAO.findById(conn, course.getCourseId()) != null) {
                    throw new CourseServiceException("课程号已存在：" + course.getCourseId());
                }
                if (!_courseDAO.insertCourse(conn, course)) {
                    throw new SQLException("插入课程失败");
                }
                TeachingClass defaultClass = new TeachingClass(course.getCourseId() + "-01",
                        course.getCourseId(), "01", course.getTeacher(), course.getCapacity(),
                        0, null, "Course 1.0 admin compatibility");
                if (!_teachingClassDAO.insert(conn, defaultClass)) {
                    throw new SQLException("插入默认教学班失败");
                }
                conn.commit();
            } catch (SQLIntegrityConstraintViolationException e) {
                rollback(conn, e);
                throw new CourseServiceException("课程号或教学班号已存在");
            } catch (SQLException | CourseServiceException | RuntimeException e) {
                rollback(conn, e);
                throw e;
            }
        }
        return course;
    }

    @Override
    public TeachingClass addTeachingClass(TeachingClass teachingClass)
            throws SQLException, IOException, CourseServiceException {
        validateTeachingClass(teachingClass);
        teachingClass.setSelectedCount(0);
        try (Connection conn = DbHelper.getConnection()) {
            conn.setAutoCommit(false);
            try {
                if (_courseDAO.findByIdForUpdate(conn, teachingClass.getCourseId()) == null) {
                    throw new CourseServiceException("课程不存在：" + teachingClass.getCourseId());
                }
                if (!_teachingClassDAO.insert(conn, teachingClass)) {
                    throw new SQLException("插入教学班失败");
                }
                _courseDAO.refreshCompatibilityProjection(conn, teachingClass.getCourseId());
                conn.commit();
                return teachingClass;
            } catch (SQLIntegrityConstraintViolationException e) {
                rollback(conn, e);
                throw new CourseServiceException("教学班 ID 或班号已存在");
            } catch (SQLException | CourseServiceException | RuntimeException e) {
                rollback(conn, e);
                throw e;
            }
        }
    }

    @Override
    public boolean updateTeachingClass(TeachingClass teachingClass)
            throws SQLException, IOException, CourseServiceException {
        validateTeachingClass(teachingClass);
        try (Connection conn = DbHelper.getConnection()) {
            conn.setAutoCommit(false);
            try {
                TeachingClass current = _teachingClassDAO.findById(
                        conn, teachingClass.getTeachingClassId(), true);
                if (current == null) {
                    throw new CourseServiceException("教学班不存在");
                }
                if (!current.getCourseId().equals(teachingClass.getCourseId())) {
                    throw new CourseServiceException("教学班所属课程不可修改");
                }
                if (teachingClass.getCapacity() < current.getSelectedCount()) {
                    throw new CourseServiceException("教学班容量不能小于已选人数");
                }
                if (!teachingClass.getTeacher().equals(current.getTeacher())) {
                    for (CourseSchedule schedule : _courseScheduleDAO.findByCourseIds(
                            conn, Set.of(current.getCourseId()))) {
                        if (current.getTeachingClassId().equals(schedule.getTeachingClassId())
                                && _courseScheduleDAO.hasTeacherConflict(conn, schedule,
                                teachingClass.getTeacher(), schedule.getScheduleId())) {
                            throw new CourseServiceException("修改教师后将产生排课时间冲突");
                        }
                    }
                }
                teachingClass.setSelectedCount(current.getSelectedCount());
                if (!_teachingClassDAO.update(conn, teachingClass)) {
                    throw new SQLException("更新教学班失败");
                }
                _courseDAO.refreshCompatibilityProjection(conn, current.getCourseId());
                conn.commit();
                return true;
            } catch (SQLException | CourseServiceException | RuntimeException e) {
                rollback(conn, e);
                throw e;
            }
        }
    }

    @Override
    public boolean deleteTeachingClass(String teachingClassId)
            throws SQLException, IOException, CourseServiceException {
        if (teachingClassId == null || teachingClassId.isBlank()) {
            throw new CourseServiceException("教学班 ID 不能为空");
        }
        try (Connection conn = DbHelper.getConnection()) {
            conn.setAutoCommit(false);
            try {
                TeachingClass current = _teachingClassDAO.findById(
                        conn, teachingClassId.trim(), true);
                if (current == null) {
                    throw new CourseServiceException("教学班不存在");
                }
                if (_teachingClassDAO.countSelections(conn, teachingClassId) > 0
                        || _teachingClassDAO.countSchedules(conn, teachingClassId) > 0) {
                    throw new CourseServiceException("教学班已有选课或排课，不能删除");
                }
                if (!_teachingClassDAO.delete(conn, teachingClassId.trim())) {
                    throw new SQLException("删除教学班失败");
                }
                _courseDAO.refreshCompatibilityProjection(conn, current.getCourseId());
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
                List<TeachingClass> classes = _teachingClassDAO.findByCourseId(
                        conn, course.getCourseId());
                if (classes.size() == 1) {
                    TeachingClass onlyClass = classes.get(0);
                    if (course.getCapacity() < onlyClass.getSelectedCount()) {
                        throw new CourseServiceException("课程容量不能小于已选人数");
                    }
                    for (CourseSchedule schedule : _courseScheduleDAO.findByCourseIds(
                            conn, Set.of(course.getCourseId()))) {
                        if (!course.getTeacher().equals(onlyClass.getTeacher())
                                && _courseScheduleDAO.hasTeacherConflict(
                                conn, schedule, course.getTeacher(), schedule.getScheduleId())) {
                            throw new CourseServiceException("修改教师后将产生排课时间冲突");
                        }
                    }
                    onlyClass.setTeacher(course.getTeacher());
                    onlyClass.setCapacity(course.getCapacity());
                    _teachingClassDAO.update(conn, onlyClass);
                }
                course.setSelectedCount(classes.stream()
                        .mapToInt(TeachingClass::getSelectedCount).sum());
                if (!_courseDAO.updateCourse(conn, course)) {
                    throw new SQLException("更新课程失败");
                }
                _courseDAO.refreshCompatibilityProjection(conn, course.getCourseId());
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
                for (TeachingClass teachingClass : _teachingClassDAO.findByCourseId(
                        conn, normalizedCourseId)) {
                    if (!_teachingClassDAO.delete(conn, teachingClass.getTeachingClassId())) {
                        throw new SQLException("删除教学班失败");
                    }
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
    public boolean selectCourse(String studentId, String teachingClassId)
            throws SQLException, IOException, CourseServiceException {
        validateIds(studentId, teachingClassId);
        String normalizedStudentId = studentId.trim();
        String normalizedClassId = teachingClassId.trim();

        try (Connection conn = DbHelper.getConnection()) {
            conn.setAutoCommit(false);
            try {
                if (!_selectCourseDAO.lockStudent(conn, normalizedStudentId)) {
                    throw new CourseServiceException("学生不存在：" + normalizedStudentId);
                }
                TeachingClass teachingClass = resolveTeachingClass(conn, normalizedClassId, true);
                String normalizedCourseId = teachingClass.getCourseId();
                if (_selectCourseDAO.findByStudentAndCourse(
                        conn, normalizedStudentId, normalizedCourseId) != null) {
                    throw new CourseServiceException("请勿重复选择同一门课程");
                }
                CourseRequirementGroupDAO.BlockingSelection groupBlock =
                        _requirementGroupDAO.findBlockingSelection(
                                conn, normalizedStudentId, normalizedCourseId);
                if (groupBlock != null) {
                    throw new CourseServiceException("选课失败：已选择同组课程《"
                            + groupBlock.selectedCourseName() + "》（"
                            + groupBlock.groupName() + "）");
                }
                CourseScheduleDAO.StudentScheduleConflict conflict =
                        _courseScheduleDAO.findStudentScheduleConflict(
                                conn, normalizedStudentId, teachingClass.getTeachingClassId());
                if (conflict != null) {
                    throw new CourseServiceException(formatScheduleConflict(conflict));
                }
                if (teachingClass.getSelectedCount() >= teachingClass.getCapacity()) {
                    throw new CourseServiceException("教学班已满");
                }

                SelectCourse record = new SelectCourse(
                        newSelectId(), normalizedStudentId, normalizedCourseId, LocalDateTime.now());
                record.setTeachingClassId(teachingClass.getTeachingClassId());
                if (!_selectCourseDAO.insertSelectCourse(conn, record)) {
                    throw new SQLException("插入选课记录失败");
                }
                if (!_teachingClassDAO.incrementSelected(conn,
                        teachingClass.getTeachingClassId())) {
                    throw new CourseServiceException("教学班已满");
                }
                _courseDAO.refreshCompatibilityProjection(conn, normalizedCourseId);

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
    public boolean dropCourse(String studentId, String teachingClassId)
            throws SQLException, IOException, CourseServiceException {
        validateIds(studentId, teachingClassId);
        String normalizedStudentId = studentId.trim();
        String normalizedClassId = teachingClassId.trim();

        try (Connection conn = DbHelper.getConnection()) {
            conn.setAutoCommit(false);
            try {
                if (!_selectCourseDAO.lockStudent(conn, normalizedStudentId)) {
                    throw new CourseServiceException("学生不存在：" + normalizedStudentId);
                }
                TeachingClass teachingClass = resolveTeachingClass(conn, normalizedClassId, true);
                String normalizedCourseId = teachingClass.getCourseId();
                SelectCourse selected = _selectCourseDAO.findByStudentAndTeachingClass(
                        conn, normalizedStudentId, teachingClass.getTeachingClassId());
                if (selected == null && normalizedClassId.equals(normalizedCourseId)) {
                    selected = _selectCourseDAO.findByStudentAndCourse(
                            conn, normalizedStudentId, normalizedCourseId);
                    if (selected != null) {
                        teachingClass = _teachingClassDAO.findById(
                                conn, selected.getTeachingClassId(), true);
                    }
                }
                if (selected == null) {
                    throw new CourseServiceException("未找到对应选课记录");
                }
                if (teachingClass.getSelectedCount() <= 0) {
                    throw new CourseServiceException("教学班已选人数异常，不能继续退课");
                }

                if (!_selectCourseDAO.deleteByTeachingClass(conn, normalizedStudentId,
                        teachingClass.getTeachingClassId())) {
                    throw new SQLException("删除选课记录失败");
                }
                if (!_teachingClassDAO.decrementSelected(
                        conn, teachingClass.getTeachingClassId())) {
                    throw new SQLException("更新教学班已选人数失败");
                }
                _courseDAO.refreshCompatibilityProjection(conn, normalizedCourseId);

                conn.commit();
                return true;
            } catch (SQLException | CourseServiceException | RuntimeException e) {
                rollback(conn, e);
                throw e;
            }
        }
    }

    /** Builds a stable, user-facing explanation for a rejected selection. */
    private String formatScheduleConflict(
            CourseScheduleDAO.StudentScheduleConflict conflict) {
        return "选课失败：与《" + conflict.courseName() + "》第 "
                + conflict.weekStart() + "-" + conflict.weekEnd() + " 周 "
                + dayName(conflict.dayOfWeek()) + " "
                + conflict.startPeriod() + "-" + conflict.endPeriod()
                + " 节课程时间冲突";
    }

    /** Converts the persisted ISO weekday number into the UI wording. */
    private String dayName(int dayOfWeek) {
        return switch (dayOfWeek) {
            case 1 -> "周一";
            case 2 -> "周二";
            case 3 -> "周三";
            case 4 -> "周四";
            case 5 -> "周五";
            case 6 -> "周六";
            case 7 -> "周日";
            default -> "星期" + dayOfWeek;
        };
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
                TeachingClass teachingClass = resolveScheduleTeachingClass(conn, schedule);
                checkScheduleConflicts(conn, schedule, teachingClass.getTeacher(), null);
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
                TeachingClass teachingClass = resolveScheduleTeachingClass(conn, schedule);
                checkScheduleConflicts(
                        conn, schedule, teachingClass.getTeacher(), schedule.getScheduleId());
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
        Set<String> teachingClassIds = new LinkedHashSet<>();
        for (SelectCourse record : selected) {
            teachingClassIds.add(record.getTeachingClassId());
        }
        return _courseScheduleDAO.findByTeachingClassIds(teachingClassIds);
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
        String courseName = normalizeCourseText(course.getCourseName(), "课程名称", 80);
        String teacher = normalizeCourseText(course.getTeacher(), "授课教师", 60);
        if (course.getCredit() <= 0) {
            throw new CourseServiceException("学分必须大于 0");
        }
        if (course.getCapacity() <= 0) {
            throw new CourseServiceException("课程容量必须大于 0");
        }
        course.setCourseId(courseId);
        course.setCourseName(courseName);
        course.setTeacher(teacher);
        if (course.getCourseNature() != null && course.getCourseNature().length() > 20) {
            throw new CourseServiceException("课程性质不能超过 20 个字符");
        }
        if (course.getOpeningUnit() != null && course.getOpeningUnit().length() > 80) {
            throw new CourseServiceException("开课单位不能超过 80 个字符");
        }
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
        if ((schedule.getTeachingClassId() == null || schedule.getTeachingClassId().isBlank())
                && (schedule.getCourseId() == null || schedule.getCourseId().isBlank())) {
            throw new CourseServiceException("教学班 ID 不能为空");
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
        if (schedule.getCourseId() != null) {
            schedule.setCourseId(schedule.getCourseId().trim());
        }
        if (schedule.getTeachingClassId() != null) {
            schedule.setTeachingClassId(schedule.getTeachingClassId().trim());
        }
        if (schedule.getWeekStart() <= 0) schedule.setWeekStart(1);
        if (schedule.getWeekEnd() <= 0) schedule.setWeekEnd(16);
        if (schedule.getStartPeriod() <= 0) {
            schedule.setStartPeriod(periodFor(schedule.getStartTime()));
        }
        if (schedule.getEndPeriod() <= 0) {
            schedule.setEndPeriod(periodFor(schedule.getEndTime().minusMinutes(1)));
        }
        if (schedule.getWeekStart() > schedule.getWeekEnd()
                || schedule.getWeekStart() < 1 || schedule.getWeekEnd() > 30
                || schedule.getStartPeriod() > schedule.getEndPeriod()
                || schedule.getStartPeriod() < 1 || schedule.getEndPeriod() > 13) {
            throw new CourseServiceException("周次或节次范围无效");
        }
        schedule.setClassroom(schedule.getClassroom().trim());
        if (schedule.getScheduleId() != null) {
            schedule.setScheduleId(schedule.getScheduleId().trim());
        }
    }

    /** Validates and normalizes a concrete teaching class. */
    private void validateTeachingClass(TeachingClass value) throws CourseServiceException {
        if (value == null) {
            throw new CourseServiceException("教学班信息不能为空");
        }
        value.setTeachingClassId(normalizeCourseText(
                value.getTeachingClassId(), "教学班 ID", 32));
        value.setCourseId(normalizeCourseText(value.getCourseId(), "课程号", 20));
        value.setClassNumber(normalizeCourseText(value.getClassNumber(), "教学班号", 10));
        value.setTeacher(normalizeCourseText(value.getTeacher(), "授课教师", 60));
        if (value.getCapacity() < 0) {
            throw new CourseServiceException("教学班容量不能小于 0");
        }
    }

    /** Resolves a teaching-class ID, accepting a Course 1.0 course ID as fallback. */
    private TeachingClass resolveTeachingClass(Connection conn, String identifier,
                                                boolean forUpdate)
            throws SQLException, CourseServiceException {
        TeachingClass value = _teachingClassDAO.findById(conn, identifier, forUpdate);
        if (value != null) {
            return value;
        }
        List<TeachingClass> classes = _teachingClassDAO.findByCourseId(conn, identifier);
        if (classes.isEmpty()) {
            throw new CourseServiceException("教学班不存在：" + identifier);
        }
        if (classes.size() > 1) {
            throw new CourseServiceException("该课程有多个教学班，请指定教学班 ID");
        }
        return _teachingClassDAO.findById(conn, classes.get(0).getTeachingClassId(), forUpdate);
    }

    private TeachingClass resolveScheduleTeachingClass(Connection conn, CourseSchedule schedule)
            throws SQLException, CourseServiceException {
        String identifier = schedule.getTeachingClassId();
        if (identifier == null || identifier.isBlank()) {
            identifier = schedule.getCourseId();
        }
        TeachingClass value = resolveTeachingClass(conn, identifier.trim(), true);
        schedule.setTeachingClassId(value.getTeachingClassId());
        schedule.setCourseId(value.getCourseId());
        return value;
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

    private int periodFor(java.time.LocalTime time) {
        int minutes = time.getHour() * 60 + time.getMinute();
        if (minutes < 525) return 1;
        if (minutes < 575) return 2;
        if (minutes < 675) return 3;
        if (minutes < 725) return 4;
        if (minutes < 775) return 5;
        if (minutes < 875) return 6;
        if (minutes < 925) return 7;
        if (minutes < 1025) return 8;
        if (minutes < 1075) return 9;
        if (minutes < 1125) return 10;
        if (minutes < 1175) return 11;
        if (minutes < 1225) return 12;
        return 13;
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
