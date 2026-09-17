/*
 * CourseHandler
 *
 * Version 1.0
 *
 * 2026-09-04
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.server.srv;

import vcampus.common.constant.IConstant;
import vcampus.common.vo.Message;
import vcampus.common.vo.MessageType;
import vcampus.common.vo.CourseSchedule;
import vcampus.common.vo.AutoSchedulePlan;
import vcampus.common.vo.AutoScheduleRequest;
import vcampus.common.vo.Course;
import vcampus.common.vo.TeachingClass;
import vcampus.common.vo.User;
import vcampus.server.dao.UserDAO;
import vcampus.common.vo.CourseScore;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.List;
import java.util.Set;

/**
 * 选课模块 Socket 请求处理器：解析 {@link Message}，调用服务器业务服务，并封装响应。
 */
public class CourseHandler implements ModuleHandler {

    /** 选课服务器业务服务。 */
    private final ICourseServerSrv _courseServerSrv;
    private final UserDAO _userDAO;

    /** 使用默认业务服务创建处理器。 */
    public CourseHandler() {
        this(new CourseServerSrv(), new UserDAO());
    }

    /**
     * 注入业务服务创建处理器。
     *
     * @param courseServerSrv 选课服务器业务服务
     */
    CourseHandler(ICourseServerSrv courseServerSrv) {
        this(courseServerSrv, new UserDAO());
    }

    CourseHandler(ICourseServerSrv courseServerSrv, UserDAO userDAO) {
        this._courseServerSrv = courseServerSrv;
        this._userDAO = userDAO;
    }

    /** {@inheritDoc} */
    @Override
    public Set<String> supportedMessages() {
        return Set.of(
                IConstant.MSG_COURSE_QUERY,
                IConstant.MSG_COURSE_ADD,
                IConstant.MSG_COURSE_UPDATE,
                IConstant.MSG_COURSE_DELETE,
                IConstant.MSG_COURSE_SELECT,
                IConstant.MSG_COURSE_DROP,
                IConstant.MSG_COURSE_SELECTED_QUERY,
                IConstant.MSG_COURSE_STUDENT_ID_QUERY,
                IConstant.MSG_COURSE_SCHEDULE_QUERY,
                IConstant.MSG_COURSE_SCHEDULE_ADD,
                IConstant.MSG_COURSE_SCHEDULE_UPDATE,
                IConstant.MSG_COURSE_SCHEDULE_DELETE,
                IConstant.MSG_STUDENT_TIMETABLE_QUERY,
                IConstant.MSG_TEACHER_COURSE_ENROLLMENTS_QUERY
                , IConstant.MSG_TEACHING_CLASS_QUERY
                , IConstant.MSG_TEACHING_CLASS_ADD
                , IConstant.MSG_TEACHING_CLASS_UPDATE
                , IConstant.MSG_TEACHING_CLASS_DELETE
                , IConstant.MSG_COURSE_REQUIREMENT_GROUP_QUERY
                , IConstant.MSG_COURSE_AUTO_SCHEDULE_PREVIEW
                , IConstant.MSG_COURSE_AUTO_SCHEDULE_APPLY
                , IConstant.MSG_COURSE_AUTO_SCHEDULE_VALIDATE
                , IConstant.MSG_COURSE_AUTO_SCHEDULE_DEMO_LOAD
                , IConstant.MSG_COURSE_DASHBOARD_QUERY
                , IConstant.MSG_COURSE_SCORE_STUDENT_QUERY
                , IConstant.MSG_COURSE_SCORE_TEACHER_QUERY
                , IConstant.MSG_COURSE_SCORE_SUBMIT
                , IConstant.MSG_COURSE_SCORE_PENDING_QUERY
                , IConstant.MSG_COURSE_SCORE_REVIEW
        );
    }

    /**
     * 处理选课模块请求。
     *
     * @param request 客户端请求
     * @return 服务器响应
     */
    @Override
    public Message handle(Message request) {
        try {
            return switch (request.getName()) {
                case IConstant.MSG_COURSE_QUERY -> success(
                        request, _courseServerSrv.queryCourse((String) request.getData()));
                case IConstant.MSG_TEACHING_CLASS_QUERY -> success(
                        request, _courseServerSrv.queryTeachingClass((String) request.getData()));
                case IConstant.MSG_TEACHING_CLASS_ADD -> success(request,
                        _courseServerSrv.addTeachingClass((TeachingClass) request.getData()));
                case IConstant.MSG_TEACHING_CLASS_UPDATE -> success(request,
                        _courseServerSrv.updateTeachingClass((TeachingClass) request.getData()));
                case IConstant.MSG_TEACHING_CLASS_DELETE -> success(request,
                        _courseServerSrv.deleteTeachingClass((String) request.getData()));
                case IConstant.MSG_COURSE_REQUIREMENT_GROUP_QUERY -> success(request,
                        _courseServerSrv.queryRequirementGroups());
                case IConstant.MSG_COURSE_AUTO_SCHEDULE_PREVIEW -> success(request,
                        _courseServerSrv.previewAutoSchedule((AutoScheduleRequest) request.getData()));
                case IConstant.MSG_COURSE_AUTO_SCHEDULE_APPLY -> success(request,
                        _courseServerSrv.applyAutoSchedule((AutoSchedulePlan) request.getData()));
                case IConstant.MSG_COURSE_AUTO_SCHEDULE_VALIDATE -> success(request,
                        _courseServerSrv.validateAutoSchedule((AutoSchedulePlan) request.getData()));
                case IConstant.MSG_COURSE_AUTO_SCHEDULE_DEMO_LOAD -> success(request,
                        _courseServerSrv.loadAutoScheduleDemoData());
                case IConstant.MSG_COURSE_DASHBOARD_QUERY -> success(request,
                        _courseServerSrv.queryDashboard());
                case IConstant.MSG_COURSE_SCORE_STUDENT_QUERY -> success(request,
                        _courseServerSrv.queryStudentScores((String) request.getData()));
                case IConstant.MSG_COURSE_SCORE_TEACHER_QUERY -> success(request,
                        _courseServerSrv.queryTeacherScores((String) request.getData()));
                case IConstant.MSG_COURSE_SCORE_SUBMIT -> handleScoreSubmit(request);
                case IConstant.MSG_COURSE_SCORE_PENDING_QUERY -> success(request,
                        _courseServerSrv.queryPendingScores());
                case IConstant.MSG_COURSE_SCORE_REVIEW -> handleScoreReview(request);
                case IConstant.MSG_COURSE_ADD -> success(
                        request, _courseServerSrv.addCourse((Course) request.getData()));
                case IConstant.MSG_COURSE_UPDATE -> success(
                        request, _courseServerSrv.updateCourse((Course) request.getData()));
                case IConstant.MSG_COURSE_DELETE -> success(
                        request, _courseServerSrv.deleteCourse((String) request.getData()));
                case IConstant.MSG_COURSE_SELECT -> handleSelect(request);
                case IConstant.MSG_COURSE_DROP -> handleDrop(request);
                case IConstant.MSG_COURSE_SELECTED_QUERY -> success(
                        request, _courseServerSrv.querySelectedCourse((String) request.getData()));
                case IConstant.MSG_COURSE_STUDENT_ID_QUERY -> success(
                        request, _courseServerSrv.queryStudentId((String) request.getData()));
                case IConstant.MSG_COURSE_SCHEDULE_QUERY -> success(
                        request, _courseServerSrv.querySchedule());
                case IConstant.MSG_COURSE_SCHEDULE_ADD -> success(
                        request, _courseServerSrv.addSchedule((CourseSchedule) request.getData()));
                case IConstant.MSG_COURSE_SCHEDULE_UPDATE -> success(
                        request, _courseServerSrv.updateSchedule((CourseSchedule) request.getData()));
                case IConstant.MSG_COURSE_SCHEDULE_DELETE -> success(
                        request, _courseServerSrv.deleteSchedule((String) request.getData()));
                case IConstant.MSG_STUDENT_TIMETABLE_QUERY -> success(
                        request, _courseServerSrv.queryStudentSchedule((String) request.getData()));
                case IConstant.MSG_TEACHER_COURSE_ENROLLMENTS_QUERY ->
                        handleTeacherCourseEnrollments(request);
                default -> response(request, IConstant.STATUS_BAD_REQUEST,
                        "未知的选课操作：" + request.getName());
            };
        } catch (CourseServiceException | ClassCastException | NullPointerException e) {
            return response(request, IConstant.STATUS_BAD_REQUEST, e.getMessage());
        } catch (SQLException | IOException e) {
            return response(request, IConstant.STATUS_ERROR,
                    "服务器内部异常：" + e.getMessage());
        }
    }

    /** 处理选课请求。 */
    private Message handleSelect(Message request)
            throws SQLException, IOException, CourseServiceException {
        Map<String, String> params = stringMap(request.getData());
        _courseServerSrv.selectCourse(params.get("studentId"), classIdentifier(params));
        return success(request, "选课成功");
    }

    /** 处理退课请求。 */
    private Message handleDrop(Message request)
            throws SQLException, IOException, CourseServiceException {
        Map<String, String> params = stringMap(request.getData());
        _courseServerSrv.dropCourse(params.get("studentId"), classIdentifier(params));
        return success(request, "退课成功");
    }

    /** 只允许状态正常的真实教师账号读取自己的教学班名单。 */
    private Message handleTeacherCourseEnrollments(Message request)
            throws SQLException, IOException, CourseServiceException {
        if (!(request.getData() instanceof User credentials)
                || credentials.getUId() == null || credentials.getUPwd() == null) {
            return response(request, IConstant.STATUS_FORBIDDEN, "请先以教师账号登录");
        }
        User actual = _userDAO.findByUId(credentials.getUId().trim());
        if (actual == null || !credentials.getUPwd().equals(actual.getUPwd())) {
            return response(request, IConstant.STATUS_FORBIDDEN, "登录状态无效，请重新登录");
        }
        if (!actual.isTeacher()) {
            return response(request, IConstant.STATUS_FORBIDDEN, "只有教师可以查看教学班学生名单");
        }
        if (!User.STATUS_NORMAL.equals(actual.getUStatus())) {
            return response(request, IConstant.STATUS_FORBIDDEN, "当前账号状态无法查看学生名单");
        }
        String teacherName = actual.getUName() == null ? "" : actual.getUName().trim();
        if (teacherName.isEmpty()) {
            return response(request, IConstant.STATUS_FORBIDDEN, "教师账号尚未设置姓名");
        }
        if (_userDAO.hasOtherTeacherWithName(actual.getUId(), teacherName)) {
            return response(request, IConstant.STATUS_FORBIDDEN,
                    "存在同名教师账号，请联系管理员核对授课信息");
        }
        return success(request, _courseServerSrv.queryTeacherCourseEnrollments(teacherName));
    }

    @SuppressWarnings("unchecked")
    private Message handleScoreSubmit(Message request)
            throws SQLException, IOException, CourseServiceException {
        Map<String, Object> data = (Map<String, Object>) request.getData();
        Object values = data.get("scores");
        if (!(values instanceof List<?> list)) throw new CourseServiceException("成绩请求格式错误");
        return success(request, _courseServerSrv.submitScores((String) data.get("teacher"),
                (List<CourseScore>) list));
    }

    @SuppressWarnings("unchecked")
    private Message handleScoreReview(Message request)
            throws SQLException, IOException, CourseServiceException {
        Map<String, Object> data = (Map<String, Object>) request.getData();
        return success(request, _courseServerSrv.reviewScore((String) data.get("scoreId"),
                Boolean.TRUE.equals(data.get("approved"))));
    }

    /** 将请求 data 校验并转换为字符串参数表。 */
    @SuppressWarnings("unchecked")
    private Map<String, String> stringMap(Object data) throws CourseServiceException {
        if (!(data instanceof Map<?, ?>)) {
            throw new CourseServiceException("请求参数格式错误");
        }
        return (Map<String, String>) data;
    }

    private String classIdentifier(Map<String, String> params) {
        String value = params.get("teachingClassId");
        return value == null ? params.get("courseId") : value;
    }

    /** 创建成功响应。 */
    private Message success(Message request, Object data) {
        return response(request, IConstant.STATUS_SUCCESS, data);
    }

    /** 创建与请求同 uid/name 的响应。 */
    private Message response(Message request, String statusCode, Object data) {
        return new Message(request.getUid(), request.getName(), MessageType.DATA,
                statusCode, data, "Server");
    }
}
