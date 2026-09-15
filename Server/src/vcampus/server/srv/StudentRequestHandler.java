package vcampus.server.srv;

import vcampus.common.constant.StudentProtocol;
import vcampus.common.vo.Message;
import vcampus.common.vo.MessageType;
import vcampus.common.vo.Student;
import vcampus.common.vo.StudentUpdateRequest;
import vcampus.common.vo.User;
import vcampus.server.dao.UserDAO;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** 将学生模块的 Socket 消息分发给 StudentServerSrv。 */
public class StudentRequestHandler {

    private final IStudentServerSrv _studentServerSrv;
    private final UserDAO _userDAO;

    public StudentRequestHandler() {
        this(new StudentServerSrv(), new UserDAO());
    }

    StudentRequestHandler(IStudentServerSrv studentServerSrv) {
        this(studentServerSrv, new UserDAO());
    }

    StudentRequestHandler(IStudentServerSrv studentServerSrv, UserDAO userDAO) {
        this._studentServerSrv = studentServerSrv;
        this._userDAO = userDAO;
    }

    public boolean supports(Message request) {
        return request != null && request.getName() != null
                && request.getName().startsWith("student.");
    }

    public Message handle(Message request) {
        if (!supports(request)) {
            return response(request, StudentProtocol.STATUS_BAD_REQUEST, "不是学籍模块请求");
        }

        try {
            User currentUser = authenticate(request);
            Object data = switch (request.getName()) {
                case StudentProtocol.LIST -> listStudents(currentUser);
                case StudentProtocol.GET_SELF -> getMyStudentInfo(currentUser);
                case StudentProtocol.QUERY_BY_ID ->
                        queryByStudentId(currentUser, (String) request.getData());
                case StudentProtocol.QUERY_BY_CARD ->
                        queryByCampusCard(currentUser, (String) request.getData());
                case StudentProtocol.QUERY_BY_NAME ->
                        queryByName(currentUser, (String) request.getData());
                case StudentProtocol.ADD ->
                        addStudent(currentUser, (Student) request.getData());
                case StudentProtocol.UPDATE ->
                        updateStudent(currentUser, request.getData());
                case StudentProtocol.DELETE -> {
                    requireAdmin(currentUser);
                    _studentServerSrv.deleteStudent((String) request.getData());
                    yield "删除成功";
                }
                case StudentProtocol.OVERVIEW -> {
                    requireAdmin(currentUser);
                    yield _studentServerSrv.loadOverview((String) request.getData());
                }
                default -> throw new StudentServiceException(
                        StudentProtocol.STATUS_BAD_REQUEST, "未知的学籍操作：" + request.getName());
            };

            if ((StudentProtocol.GET_SELF.equals(request.getName())
                    || StudentProtocol.QUERY_BY_ID.equals(request.getName())
                    || StudentProtocol.QUERY_BY_CARD.equals(request.getName())) && data == null) {
                return response(request, StudentProtocol.STATUS_NOT_FOUND, "没有找到学生");
            }
            return response(request, StudentProtocol.STATUS_SUCCESS, data);
        } catch (StudentServiceException exception) {
            return response(request, exception.getStatusCode(), exception.getMessage());
        } catch (ClassCastException exception) {
            return response(request, StudentProtocol.STATUS_BAD_REQUEST, "请求数据类型不正确");
        } catch (SQLException | IOException exception) {
            return response(request, StudentProtocol.STATUS_ERROR,
                    "服务器处理学籍请求失败：" + exception.getMessage());
        }
    }

    private User authenticate(Message request)
            throws SQLException, IOException, StudentServiceException {
        if (!(request.getSender() instanceof User credentials)
                || credentials.getUId() == null || credentials.getUPwd() == null) {
            throw forbidden("请先登录后再访问学籍模块");
        }
        User actualUser = _userDAO.findByUId(credentials.getUId().trim());
        if (actualUser == null || !credentials.getUPwd().equals(actualUser.getUPwd())) {
            throw forbidden("登录状态无效，请重新登录");
        }
        if (User.STATUS_DISABLED.equals(actualUser.getUStatus())) {
            throw forbidden("账号已被禁用，请联系管理员");
        }
        if (User.STATUS_PENDING.equals(actualUser.getUStatus())) {
            throw forbidden("账号尚未通过审核，请联系管理员");
        }
        if (!User.STATUS_NORMAL.equals(actualUser.getUStatus())) {
            throw forbidden("账号状态异常，请联系管理员");
        }
        return actualUser;
    }

    private List<Student> listStudents(User currentUser)
            throws SQLException, IOException, StudentServiceException {
        requireTeacherOrAdmin(currentUser);
        if (currentUser.isTeacher()) {
            return publicStudents(_studentServerSrv.findStudentsTaughtBy(currentUser.getUId()));
        }
        return _studentServerSrv.findAll();
    }

    private Student getMyStudentInfo(User currentUser)
            throws SQLException, IOException, StudentServiceException {
        if (!currentUser.isStudent()) {
            throw forbidden("只有学生账号可以查询本人学籍");
        }
        return _studentServerSrv.findByUserId(currentUser.getUId());
    }

    private Student queryByStudentId(User currentUser, String studentId)
            throws SQLException, IOException, StudentServiceException {
        requireTeacherOrAdmin(currentUser);
        if (currentUser.isTeacher()) {
            String keyword = requireQueryText(studentId, "学号");
            return publicStudent(_studentServerSrv.findStudentTaughtBy(currentUser.getUId(), keyword));
        }
        Student student = _studentServerSrv.findByStudentId(studentId);
        return student;
    }

    private Student queryByCampusCard(User currentUser, String campusCardNo)
            throws SQLException, IOException, StudentServiceException {
        requireAdmin(currentUser);
        return _studentServerSrv.findByCampusCardNo(campusCardNo);
    }

    private List<Student> queryByName(User currentUser, String name)
            throws SQLException, IOException, StudentServiceException {
        requireTeacherOrAdmin(currentUser);
        if (currentUser.isTeacher()) {
            String keyword = requireQueryText(name, "姓名");
            return publicStudents(_studentServerSrv.findStudentsTaughtByName(currentUser.getUId(), keyword));
        }
        return _studentServerSrv.findByName(name);
    }

    private Student addStudent(User currentUser, Student student)
            throws SQLException, IOException, StudentServiceException {
        requireAdmin(currentUser);
        return _studentServerSrv.addStudent(student);
    }

    private Student updateStudent(User currentUser, Object requestData)
            throws SQLException, IOException, StudentServiceException {
        requireAdmin(currentUser);
        if (requestData instanceof StudentUpdateRequest updateRequest) {
            return _studentServerSrv.updateStudent(updateRequest.getOriginalStudentId(),
                    updateRequest.getStudent());
        }
        if (requestData instanceof Student student) {
            return _studentServerSrv.updateStudent(student);
        }
        throw new StudentServiceException(StudentProtocol.STATUS_BAD_REQUEST,
                "学籍修改请求格式不正确");
    }

    private static void requireTeacherOrAdmin(User user) throws StudentServiceException {
        if (!user.isTeacher() && !user.isAdmin()) {
            throw forbidden("当前账号无权查询其他学生学籍");
        }
    }

    private static void requireAdmin(User user) throws StudentServiceException {
        if (!user.isAdmin()) {
            throw forbidden("只有管理员可以维护学生档案");
        }
    }

    private static StudentServiceException forbidden(String message) {
        return new StudentServiceException(StudentProtocol.STATUS_FORBIDDEN, message);
    }

    private static String requireQueryText(String value, String fieldName)
            throws StudentServiceException {
        if (value == null || value.trim().isEmpty()) {
            throw new StudentServiceException(StudentProtocol.STATUS_BAD_REQUEST,
                    fieldName + "不能为空");
        }
        return value.trim();
    }

    private static List<Student> publicStudents(List<Student> students) {
        List<Student> publicStudents = new ArrayList<>();
        for (Student student : students) {
            publicStudents.add(publicStudent(student));
        }
        return publicStudents;
    }

    /** 教师视图只返回说明书约定的公开字段。 */
    private static Student publicStudent(Student source) {
        if (source == null) {
            return null;
        }
        Student target = new Student();
        target.setStudentId(source.getStudentId());
        target.setCampusCardNo(source.getCampusCardNo());
        target.setName(source.getName());
        target.setClassName(source.getClassName());
        target.setMajor(source.getMajor());
        target.setGrade(source.getGrade());
        target.setStatus(source.getStatus());
        return target;
    }

    private static Message response(Message request, String statusCode, Object data) {
        Long uid = request == null ? System.currentTimeMillis() : request.getUid();
        String name = request == null ? "student.unknown" : request.getName();
        return new Message(uid, name, MessageType.DATA, statusCode, data, "StudentServer");
    }
}
