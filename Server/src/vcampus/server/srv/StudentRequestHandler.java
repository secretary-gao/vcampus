package vcampus.server.srv;

import vcampus.common.constant.StudentProtocol;
import vcampus.common.vo.Message;
import vcampus.common.vo.MessageType;
import vcampus.common.vo.Student;

import java.io.IOException;
import java.sql.SQLException;

/** 将学生模块的 Socket 消息分发给 StudentServerSrv。 */
public class StudentRequestHandler {

    private final IStudentServerSrv _studentServerSrv;

    public StudentRequestHandler() {
        this(new StudentServerSrv());
    }

    StudentRequestHandler(IStudentServerSrv studentServerSrv) {
        this._studentServerSrv = studentServerSrv;
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
            Object data = switch (request.getName()) {
                case StudentProtocol.LIST -> _studentServerSrv.findAll();
                case StudentProtocol.QUERY_BY_ID ->
                        _studentServerSrv.findByStudentId((String) request.getData());
                case StudentProtocol.QUERY_BY_CARD ->
                        _studentServerSrv.findByCampusCardNo((String) request.getData());
                case StudentProtocol.QUERY_BY_NAME ->
                        _studentServerSrv.findByName((String) request.getData());
                case StudentProtocol.ADD ->
                        _studentServerSrv.addStudent((Student) request.getData());
                case StudentProtocol.UPDATE ->
                        _studentServerSrv.updateStudent((Student) request.getData());
                case StudentProtocol.DELETE -> {
                    _studentServerSrv.deleteStudent((String) request.getData());
                    yield "删除成功";
                }
                default -> throw new StudentServiceException(
                        StudentProtocol.STATUS_BAD_REQUEST, "未知的学籍操作：" + request.getName());
            };

            if ((StudentProtocol.QUERY_BY_ID.equals(request.getName())
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

    private static Message response(Message request, String statusCode, Object data) {
        Long uid = request == null ? System.currentTimeMillis() : request.getUid();
        String name = request == null ? "student.unknown" : request.getName();
        return new Message(uid, name, MessageType.DATA, statusCode, data, "StudentServer");
    }
}
