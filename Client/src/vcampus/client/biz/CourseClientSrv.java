/*
 * CourseClientSrv
 *
 * Version 1.0
 *
 * 2026-09-04
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.client.biz;

import vcampus.common.constant.IConstant;
import vcampus.common.vo.Course;
import vcampus.common.vo.CourseSchedule;
import vcampus.common.vo.Message;
import vcampus.common.vo.MessageType;
import vcampus.common.vo.SelectCourse;
import vcampus.common.vo.TeacherCourseEnrollment;
import vcampus.common.vo.TeachingClass;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link ICourseClientSrv} 的 Socket 实现。每次调用建立一条短连接，发送一个
 * {@link Message} 并读取一个响应，连接方式与 {@link UserClientSrv} 一致。
 */
public class CourseClientSrv implements ICourseClientSrv {

    @Override
    public List<TeachingClass> queryTeachingClass(String keyword)
            throws IOException, ClassNotFoundException {
        Message response = send(IConstant.MSG_TEACHING_CLASS_QUERY, keyword, "Client");
        ensureSuccess(response);
        return castList(response.getData());
    }

    @Override
    public TeachingClass addTeachingClass(TeachingClass teachingClass)
            throws IOException, ClassNotFoundException {
        Message response = send(IConstant.MSG_TEACHING_CLASS_ADD, teachingClass, "Admin");
        ensureSuccess(response);
        if (!(response.getData() instanceof TeachingClass added)) {
            throw new CourseClientException(IConstant.STATUS_ERROR, "服务器响应格式错误");
        }
        return added;
    }

    @Override
    public boolean updateTeachingClass(TeachingClass teachingClass)
            throws IOException, ClassNotFoundException {
        Message response = send(IConstant.MSG_TEACHING_CLASS_UPDATE, teachingClass, "Admin");
        ensureSuccess(response);
        return true;
    }

    @Override
    public boolean deleteTeachingClass(String teachingClassId)
            throws IOException, ClassNotFoundException {
        Message response = send(IConstant.MSG_TEACHING_CLASS_DELETE, teachingClassId, "Admin");
        ensureSuccess(response);
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public List<Course> queryCourse(String keyword) throws IOException, ClassNotFoundException {
        Message response = send(IConstant.MSG_COURSE_QUERY, keyword, "Client");
        ensureSuccess(response);
        return castList(response.getData());
    }

    /** {@inheritDoc} */
    @Override
    public Course addCourse(Course course) throws IOException, ClassNotFoundException {
        Message response = send(IConstant.MSG_COURSE_ADD, course, "Admin");
        ensureSuccess(response);
        if (!(response.getData() instanceof Course added)) {
            throw new CourseClientException(IConstant.STATUS_ERROR, "服务器响应格式错误");
        }
        return added;
    }

    /** {@inheritDoc} */
    @Override
    public boolean updateCourse(Course course) throws IOException, ClassNotFoundException {
        Message response = send(IConstant.MSG_COURSE_UPDATE, course, "Admin");
        ensureSuccess(response);
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean deleteCourse(String courseId) throws IOException, ClassNotFoundException {
        Message response = send(IConstant.MSG_COURSE_DELETE, courseId, "Admin");
        ensureSuccess(response);
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean selectCourse(String studentId, String courseId)
            throws IOException, ClassNotFoundException {
        Message response = send(IConstant.MSG_COURSE_SELECT,
                idParams(studentId, courseId), studentId);
        ensureSuccess(response);
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean dropCourse(String studentId, String courseId)
            throws IOException, ClassNotFoundException {
        Message response = send(IConstant.MSG_COURSE_DROP,
                idParams(studentId, courseId), studentId);
        ensureSuccess(response);
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public List<SelectCourse> querySelectedCourse(String studentId)
            throws IOException, ClassNotFoundException {
        Message response = send(IConstant.MSG_COURSE_SELECTED_QUERY, studentId, studentId);
        ensureSuccess(response);
        return castList(response.getData());
    }

    /** {@inheritDoc} */
    @Override
    public String queryStudentId(String userId) throws IOException, ClassNotFoundException {
        Message response = send(IConstant.MSG_COURSE_STUDENT_ID_QUERY, userId, userId);
        ensureSuccess(response);
        return response.getData() == null ? null : String.valueOf(response.getData());
    }

    /** {@inheritDoc} */
    @Override
    public List<CourseSchedule> querySchedule() throws IOException, ClassNotFoundException {
        Message response = send(IConstant.MSG_COURSE_SCHEDULE_QUERY, null, "Client");
        ensureSuccess(response);
        return castList(response.getData());
    }

    /** {@inheritDoc} */
    @Override
    public CourseSchedule addSchedule(CourseSchedule schedule)
            throws IOException, ClassNotFoundException {
        Message response = send(IConstant.MSG_COURSE_SCHEDULE_ADD, schedule, "Admin");
        ensureSuccess(response);
        if (!(response.getData() instanceof CourseSchedule added)) {
            throw new CourseClientException(IConstant.STATUS_ERROR, "服务器响应格式错误");
        }
        return added;
    }

    /** {@inheritDoc} */
    @Override
    public boolean updateSchedule(CourseSchedule schedule)
            throws IOException, ClassNotFoundException {
        Message response = send(IConstant.MSG_COURSE_SCHEDULE_UPDATE, schedule, "Admin");
        ensureSuccess(response);
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean deleteSchedule(String scheduleId)
            throws IOException, ClassNotFoundException {
        Message response = send(IConstant.MSG_COURSE_SCHEDULE_DELETE, scheduleId, "Admin");
        ensureSuccess(response);
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public List<CourseSchedule> queryStudentSchedule(String studentId)
            throws IOException, ClassNotFoundException {
        Message response = send(IConstant.MSG_STUDENT_TIMETABLE_QUERY, studentId, studentId);
        ensureSuccess(response);
        return castList(response.getData());
    }

    /** {@inheritDoc} */
    @Override
    public List<TeacherCourseEnrollment> queryTeacherCourseEnrollments(String teacherName)
            throws IOException, ClassNotFoundException {
        Message response = send(IConstant.MSG_TEACHER_COURSE_ENROLLMENTS_QUERY,
                teacherName, teacherName);
        ensureSuccess(response);
        return castList(response.getData());
    }

    /** 创建学号与课程号参数。 */
    private Map<String, String> idParams(String studentId, String teachingClassId) {
        Map<String, String> params = new HashMap<>();
        params.put("studentId", studentId);
        params.put("teachingClassId", teachingClassId);
        return params;
    }

    /** 创建请求并通过 Socket 发送。 */
    private Message send(String name, Object data, Object sender)
            throws IOException, ClassNotFoundException {
        Message request = new Message(System.currentTimeMillis(), name,
                MessageType.COMMAND, null, data, sender);
        return sendAndReceive(request);
    }

    /** 建立短连接、发送请求并接收响应。 */
    private Message sendAndReceive(Message request) throws IOException, ClassNotFoundException {
        try (Socket socket = new Socket(IConstant.SERVER_HOST, IConstant.SERVER_PORT);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
            out.flush();
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            out.writeObject(request);
            out.flush();
            return (Message) in.readObject();
        }
    }

    /** 非成功响应转换为包含服务器提示的异常。 */
    private void ensureSuccess(Message response) throws CourseClientException {
        if (!IConstant.STATUS_SUCCESS.equals(response.getStatusCode())) {
            throw new CourseClientException(response.getStatusCode(),
                    String.valueOf(response.getData()));
        }
    }

    /** 将响应中的可序列化列表转换为调用方需要的泛型列表。 */
    @SuppressWarnings("unchecked")
    private <T> List<T> castList(Object data) throws CourseClientException {
        if (!(data instanceof List<?>)) {
            throw new CourseClientException(IConstant.STATUS_ERROR, "服务器响应格式错误");
        }
        return (List<T>) data;
    }
}
