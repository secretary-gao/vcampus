package vcampus.client.biz;

import vcampus.common.constant.StudentProtocol;
import vcampus.common.vo.Message;
import vcampus.common.vo.MessageType;
import vcampus.common.vo.Student;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;

/** 通过 Socket 向服务器发送学籍请求。 */
public class StudentClientSrv implements IStudentClientSrv {

    private final String _host;
    private final int _port;

    public StudentClientSrv() {
        this(StudentProtocol.DEFAULT_HOST, StudentProtocol.DEFAULT_PORT);
    }

    public StudentClientSrv(String host, int port) {
        this._host = host;
        this._port = port;
    }

    @Override
    public List<Student> findAll()
            throws IOException, ClassNotFoundException, StudentClientException {
        return studentList(request(StudentProtocol.LIST, null).getData());
    }

    @Override
    public Student findByStudentId(String studentId)
            throws IOException, ClassNotFoundException, StudentClientException {
        Message response = requestAllowNotFound(StudentProtocol.QUERY_BY_ID, studentId);
        return response == null ? null : (Student) response.getData();
    }

    @Override
    public Student findByCampusCardNo(String campusCardNo)
            throws IOException, ClassNotFoundException, StudentClientException {
        Message response = requestAllowNotFound(StudentProtocol.QUERY_BY_CARD, campusCardNo);
        return response == null ? null : (Student) response.getData();
    }

    @Override
    public List<Student> findByName(String name)
            throws IOException, ClassNotFoundException, StudentClientException {
        return studentList(request(StudentProtocol.QUERY_BY_NAME, name).getData());
    }

    @Override
    public Student addStudent(Student student)
            throws IOException, ClassNotFoundException, StudentClientException {
        return (Student) request(StudentProtocol.ADD, student).getData();
    }

    @Override
    public Student updateStudent(Student student)
            throws IOException, ClassNotFoundException, StudentClientException {
        return (Student) request(StudentProtocol.UPDATE, student).getData();
    }

    @Override
    public void deleteStudent(String studentId)
            throws IOException, ClassNotFoundException, StudentClientException {
        request(StudentProtocol.DELETE, studentId);
    }

    private Message request(String name, Object data)
            throws IOException, ClassNotFoundException, StudentClientException {
        Message response = sendAndReceive(new Message(System.currentTimeMillis(), name,
                MessageType.COMMAND, null, data, "StudentClient"));
        if (!StudentProtocol.STATUS_SUCCESS.equals(response.getStatusCode())) {
            throw new StudentClientException(response.getStatusCode(),
                    String.valueOf(response.getData()));
        }
        return response;
    }

    private Message requestAllowNotFound(String name, Object data)
            throws IOException, ClassNotFoundException, StudentClientException {
        try {
            return request(name, data);
        } catch (StudentClientException exception) {
            if (StudentProtocol.STATUS_NOT_FOUND.equals(exception.getStatusCode())) {
                return null;
            }
            throw exception;
        }
    }

    private Message sendAndReceive(Message request)
            throws IOException, ClassNotFoundException {
        try (Socket socket = new Socket(_host, _port);
             ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream())) {
            output.flush();
            try (ObjectInputStream input = new ObjectInputStream(socket.getInputStream())) {
                output.writeObject(request);
                output.flush();
                return (Message) input.readObject();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Student> studentList(Object data) throws StudentClientException {
        if (!(data instanceof List<?>)) {
            throw new StudentClientException(StudentProtocol.STATUS_ERROR,
                    "服务器返回的学生列表格式不正确");
        }
        return (List<Student>) data;
    }
}
