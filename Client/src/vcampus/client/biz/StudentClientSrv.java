package vcampus.client.biz;

import vcampus.common.constant.StudentProtocol;
import vcampus.common.vo.Message;
import vcampus.common.vo.MessageType;
import vcampus.common.vo.Student;
import vcampus.common.vo.StudentCampusOverview;
import vcampus.common.vo.StudentUpdateRequest;
import vcampus.common.vo.User;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.util.List;

/** 通过 Socket 向服务器发送学籍请求。 */
public class StudentClientSrv implements IStudentClientSrv {

    private final String _host;
    private final int _port;
    private final User _currentUser;

    public StudentClientSrv() {
        this(null, StudentProtocol.DEFAULT_HOST, StudentProtocol.DEFAULT_PORT);
    }

    public StudentClientSrv(String host, int port) {
        this(null, host, port);
    }

    public StudentClientSrv(User currentUser) {
        this(currentUser, StudentProtocol.DEFAULT_HOST, StudentProtocol.DEFAULT_PORT);
    }

    public StudentClientSrv(User currentUser, String host, int port) {
        this._currentUser = currentUser;
        this._host = host;
        this._port = port;
    }

    @Override
    public List<Student> findAll()
            throws IOException, ClassNotFoundException, StudentClientException {
        return studentList(request(StudentProtocol.LIST, null).getData());
    }

    @Override
    public Student getMyStudentInfo()
            throws IOException, ClassNotFoundException, StudentClientException {
        Message response = requestAllowNotFound(StudentProtocol.GET_SELF, null);
        return response == null ? null : (Student) response.getData();
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
        String originalStudentId = student == null ? null : student.getStudentId();
        return updateStudent(originalStudentId, student);
    }

    @Override
    public Student updateStudent(String originalStudentId, Student student)
            throws IOException, ClassNotFoundException, StudentClientException {
        return (Student) request(StudentProtocol.UPDATE,
                new StudentUpdateRequest(originalStudentId, student)).getData();
    }

    @Override
    public void deleteStudent(String studentId)
            throws IOException, ClassNotFoundException, StudentClientException {
        request(StudentProtocol.DELETE, studentId);
    }

    @Override
    public StudentCampusOverview loadOverview(String studentId)
            throws IOException, ClassNotFoundException, StudentClientException {
        return (StudentCampusOverview) request(StudentProtocol.OVERVIEW, studentId).getData();
    }

    private Message request(String name, Object data)
            throws IOException, ClassNotFoundException, StudentClientException {
        Message response = sendAndReceive(new Message(System.currentTimeMillis(), name,
                MessageType.COMMAND, null, data, _currentUser));
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
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(_host, _port), 5000);
            socket.setSoTimeout(10000);
            try (ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream())) {
                output.flush();
                try (ObjectInputStream input = new ObjectInputStream(socket.getInputStream())) {
                    output.writeObject(request);
                    output.flush();
                    return (Message) input.readObject();
                }
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
