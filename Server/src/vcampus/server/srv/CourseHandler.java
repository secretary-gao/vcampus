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

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;

/**
 * 选课模块 Socket 请求处理器：解析 {@link Message}，调用服务器业务服务，并封装响应。
 */
public class CourseHandler implements ModuleHandler {

    /** 选课服务器业务服务。 */
    private final ICourseServerSrv _courseServerSrv;

    /** 使用默认业务服务创建处理器。 */
    public CourseHandler() {
        this(new CourseServerSrv());
    }

    /**
     * 注入业务服务创建处理器。
     *
     * @param courseServerSrv 选课服务器业务服务
     */
    CourseHandler(ICourseServerSrv courseServerSrv) {
        this._courseServerSrv = courseServerSrv;
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
                case IConstant.MSG_TEACHER_COURSE_ENROLLMENTS_QUERY -> success(
                        request, _courseServerSrv.queryTeacherCourseEnrollments(
                                (String) request.getData()));
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
