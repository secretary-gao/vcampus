package vcampus.server.srv;

import vcampus.common.constant.StudentProtocol;
import vcampus.common.vo.Message;
import vcampus.common.vo.MessageType;
import vcampus.common.vo.Student;
import vcampus.common.vo.StudentStatus;
import vcampus.common.vo.User;
import vcampus.server.dao.UserDAO;

import java.util.List;

/** 不访问数据库，验证学籍模块的服务端角色权限。 */
public class StudentPermissionTest {

    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        FakeStudentService service = new FakeStudentService();
        FakeUserDAO userDAO = new FakeUserDAO();
        StudentRequestHandler handler = new StudentRequestHandler(service, userDAO);

        Message selfResponse = handler.handle(request(StudentProtocol.GET_SELF, null,
                credentials("student1", "学生")));
        requireSuccess(selfResponse, "学生查询本人失败");
        require("student1".equals(service.lastUserId), "查询本人未使用登录账号");

        Message studentList = handler.handle(request(StudentProtocol.LIST, null,
                credentials("student1", "学生")));
        require(StudentProtocol.STATUS_FORBIDDEN.equals(studentList.getStatusCode()),
                "学生不应查询全部学籍");

        Message teacherList = handler.handle(request(StudentProtocol.LIST, null,
                credentials("teacher1", "教师")));
        requireSuccess(teacherList, "教师查询学生失败");
        require("teacher1".equals(service.lastTeacherUserId),
                "教师列表没有按当前登录账号限制范围");
        Student publicStudent = ((List<Student>) teacherList.getData()).get(0);
        require(publicStudent.getCampusCardNo() == null && publicStudent.getUserId() == null,
                "教师响应泄露了一卡通号或关联账号");

        Message unrelatedStudent = handler.handle(request(StudentProtocol.QUERY_BY_ID,
                "2024000099", credentials("teacher1", "教师")));
        require(StudentProtocol.STATUS_NOT_FOUND.equals(unrelatedStudent.getStatusCode()),
                "教师查询到了不属于自己授课范围的学生");

        Message teacherCardQuery = handler.handle(request(StudentProtocol.QUERY_BY_CARD,
                "CARD-001", credentials("teacher1", "教师")));
        require(StudentProtocol.STATUS_FORBIDDEN.equals(teacherCardQuery.getStatusCode()),
                "教师不应按一卡通号查询");

        Message teacherUpdate = handler.handle(request(StudentProtocol.UPDATE, service.student,
                credentials("teacher1", "教师")));
        require(StudentProtocol.STATUS_FORBIDDEN.equals(teacherUpdate.getStatusCode()),
                "教师不应修改学籍");

        User forgedAdmin = credentials("student1", "管理员");
        Message forgedUpdate = handler.handle(request(StudentProtocol.UPDATE, service.student,
                forgedAdmin));
        require(StudentProtocol.STATUS_FORBIDDEN.equals(forgedUpdate.getStatusCode()),
                "服务器不应信任客户端伪造的管理员角色");

        Message adminUpdate = handler.handle(request(StudentProtocol.UPDATE, service.student,
                credentials("admin001", "管理员")));
        requireSuccess(adminUpdate, "管理员修改学籍失败");

        // 客户端即使保留原登录信息并自称“正常”，也必须服从数据库中的最新状态。
        for (String status : new String[] {User.STATUS_DISABLED, User.STATUS_PENDING, null, "未知状态"}) {
            userDAO.status = status;
            requireForbidden(handler.handle(request(StudentProtocol.GET_SELF, null,
                    credentials("student1", "学生"))), "非正常学生账号读取了本人学籍");
            requireForbidden(handler.handle(request(StudentProtocol.LIST, null,
                    credentials("teacher1", "教师"))), "非正常教师账号读取了学籍列表");
            requireForbidden(handler.handle(request(StudentProtocol.QUERY_BY_ID, "2024000001",
                    credentials("teacher1", "教师"))), "非正常教师账号读取了单个学生");
            requireForbidden(handler.handle(request(StudentProtocol.UPDATE, service.student,
                    credentials("admin001", "管理员"))), "非正常管理员账号修改了学籍");
        }
        userDAO.status = User.STATUS_NORMAL;
        requireSuccess(handler.handle(request(StudentProtocol.QUERY_BY_ID, "2024000001",
                credentials("teacher1", "教师"))), "恢复账号状态后仍无法查看学籍");
        User incorrectPassword = credentials("teacher1", "教师");
        incorrectPassword.setUPwd("wrong-password");
        requireForbidden(handler.handle(request(StudentProtocol.LIST, null, incorrectPassword)),
                "错误密码通过了身份校验");

        System.out.println("Student permission tests passed");
    }

    private static Message request(String name, Object data, User sender) {
        return new Message(System.currentTimeMillis(), name, MessageType.COMMAND,
                null, data, sender);
    }

    private static User credentials(String userId, String role) {
        User user = new User();
        user.setUId(userId);
        user.setUPwd("test-password-hash");
        user.setURole(role);
        return user;
    }

    private static void requireSuccess(Message message, String error) {
        require(StudentProtocol.STATUS_SUCCESS.equals(message.getStatusCode()), error);
    }

    private static void requireForbidden(Message message, String error) {
        require(StudentProtocol.STATUS_FORBIDDEN.equals(message.getStatusCode()), error);
    }

    private static void require(boolean condition, String error) {
        if (!condition) {
            throw new AssertionError(error);
        }
    }

    private static final class FakeUserDAO extends UserDAO {
        private String status = User.STATUS_NORMAL;

        @Override
        public User findByUId(String userId) {
            User user = credentials(userId, switch (userId) {
                case "teacher1" -> "教师";
                case "admin001" -> "管理员";
                default -> "学生";
            });
            user.setUPwd("test-password-hash");
            user.setUStatus(status);
            return user;
        }
    }

    private static final class FakeStudentService implements IStudentServerSrv {
        private final Student student = sampleStudent();
        private String lastUserId;
        private String lastTeacherUserId;

        @Override
        public List<Student> findAll() {
            return List.of(student);
        }

        @Override
        public List<Student> findStudentsTaughtBy(String teacherUserId) {
            lastTeacherUserId = teacherUserId;
            return List.of(student);
        }

        @Override
        public Student findStudentTaughtBy(String teacherUserId, String studentId) {
            lastTeacherUserId = teacherUserId;
            return student.getStudentId().equals(studentId) ? student : null;
        }

        @Override
        public List<Student> findStudentsTaughtByName(String teacherUserId, String name) {
            lastTeacherUserId = teacherUserId;
            return student.getName().equals(name) ? List.of(student) : List.of();
        }

        @Override
        public Student findByUserId(String userId) {
            lastUserId = userId;
            return student;
        }

        @Override
        public Student findByStudentId(String studentId) {
            return student;
        }

        @Override
        public Student findByCampusCardNo(String campusCardNo) {
            return student;
        }

        @Override
        public List<Student> findByName(String name) {
            return List.of(student);
        }

        @Override
        public Student addStudent(Student newStudent) {
            return newStudent;
        }

        @Override
        public Student updateStudent(Student updatedStudent) {
            return updatedStudent;
        }

        @Override
        public void deleteStudent(String studentId) {
        }

        private static Student sampleStudent() {
            Student student = new Student();
            student.setStudentId("2024000001");
            student.setCampusCardNo("CARD-001");
            student.setUserId("student1");
            student.setName("测试学生");
            student.setClassName("计算机一班");
            student.setMajor("计算机科学与技术");
            student.setGrade("2024");
            student.setStatus(StudentStatus.ENROLLED);
            return student;
        }
    }
}
