package vcampus.server.srv;

import vcampus.common.constant.StudentProtocol;
import vcampus.common.vo.Student;
import vcampus.common.vo.StudentStatus;
import vcampus.server.dao.StudentDAO;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/** 学籍业务实现：负责输入校验、重复检查和修改冲突判断。 */
public class StudentServerSrv implements IStudentServerSrv {

    private final StudentDAO _studentDAO;

    public StudentServerSrv() {
        this(new StudentDAO());
    }

    StudentServerSrv(StudentDAO studentDAO) {
        this._studentDAO = studentDAO;
    }

    @Override
    public List<Student> findAll() throws SQLException, IOException {
        return _studentDAO.findAll();
    }

    @Override
    public List<Student> findStudentsTaughtBy(String teacherUserId)
            throws SQLException, IOException, StudentServiceException {
        return _studentDAO.findStudentsTaughtBy(requireText(teacherUserId, "教师账号"));
    }

    @Override
    public Student findStudentTaughtBy(String teacherUserId, String studentId)
            throws SQLException, IOException, StudentServiceException {
        return _studentDAO.findStudentTaughtBy(requireText(teacherUserId, "教师账号"),
                requireText(studentId, "学号"));
    }

    @Override
    public List<Student> findStudentsTaughtByName(String teacherUserId, String name)
            throws SQLException, IOException, StudentServiceException {
        return _studentDAO.findStudentsTaughtByName(requireText(teacherUserId, "教师账号"),
                requireText(name, "姓名"));
    }

    @Override
    public Student findByUserId(String userId)
            throws SQLException, IOException, StudentServiceException {
        return _studentDAO.findByUserId(requireText(userId, "用户账号"));
    }

    @Override
    public Student findByStudentId(String studentId)
            throws SQLException, IOException, StudentServiceException {
        return _studentDAO.findByStudentId(requireText(studentId, "学号"));
    }

    @Override
    public Student findByCampusCardNo(String campusCardNo)
            throws SQLException, IOException, StudentServiceException {
        return _studentDAO.findByCampusCardNo(requireText(campusCardNo, "一卡通号"));
    }

    @Override
    public List<Student> findByName(String name)
            throws SQLException, IOException, StudentServiceException {
        return _studentDAO.findByName(requireText(name, "姓名"));
    }

    @Override
    public Student addStudent(Student student)
            throws SQLException, IOException, StudentServiceException {
        normalizeAndValidate(student);
        if (_studentDAO.findByStudentId(student.getStudentId()) != null) {
            throw conflict("学号已存在：" + student.getStudentId());
        }
        if (_studentDAO.findByCampusCardNo(student.getCampusCardNo()) != null) {
            throw conflict("一卡通号已存在：" + student.getCampusCardNo());
        }
        ensureUserExists(student.getUserId());
        ensureUserNotBound(student.getUserId(), student.getStudentId());
        if (!_studentDAO.insert(student)) {
            throw new StudentServiceException(StudentProtocol.STATUS_ERROR, "新增学生失败");
        }
        return _studentDAO.findByStudentId(student.getStudentId());
    }

    @Override
    public Student updateStudent(Student student)
            throws SQLException, IOException, StudentServiceException {
        normalizeAndValidate(student);
        Student oldStudent = _studentDAO.findByStudentId(student.getStudentId());
        if (oldStudent == null) {
            throw notFound("学生不存在：" + student.getStudentId());
        }

        Student sameCardStudent = _studentDAO.findByCampusCardNo(student.getCampusCardNo());
        if (sameCardStudent != null
                && !sameCardStudent.getStudentId().equals(student.getStudentId())) {
            throw conflict("一卡通号已被其他学生使用：" + student.getCampusCardNo());
        }
        ensureUserExists(student.getUserId());
        ensureUserNotBound(student.getUserId(), student.getStudentId());

        if (!_studentDAO.update(student)) {
            throw conflict("该学生信息已被其他操作修改，请刷新后重试");
        }
        return _studentDAO.findByStudentId(student.getStudentId());
    }

    @Override
    public void deleteStudent(String studentId)
            throws SQLException, IOException, StudentServiceException {
        String value = requireText(studentId, "学号");
        if (!_studentDAO.deleteByStudentId(value)) {
            throw notFound("学生不存在：" + value);
        }
    }

    private void ensureUserExists(String userId)
            throws SQLException, IOException, StudentServiceException {
        if (!_studentDAO.studentUserExists(userId)) {
            throw new StudentServiceException(StudentProtocol.STATUS_BAD_REQUEST,
                    "关联账号不存在或账号角色不是学生：" + userId);
        }
    }

    private void ensureUserNotBound(String userId, String studentId)
            throws SQLException, IOException, StudentServiceException {
        Student boundStudent = _studentDAO.findByUserId(userId);
        if (boundStudent != null && !boundStudent.getStudentId().equals(studentId)) {
            throw conflict("该用户账号已绑定其他学籍：" + userId);
        }
    }

    private static void normalizeAndValidate(Student student) throws StudentServiceException {
        if (student == null) {
            throw badRequest("学生信息不能为空");
        }

        student.setStudentId(checkLength(student.getStudentId(), "学号", 10));
        student.setCampusCardNo(checkLength(student.getCampusCardNo(), "一卡通号", 20));
        student.setUserId(checkLength(student.getUserId(), "用户账号", 8));
        student.setName(checkLength(student.getName(), "姓名", 20));
        student.setClassName(checkLength(student.getClassName(), "班级", 40));
        student.setMajor(checkLength(student.getMajor(), "专业", 50));
        student.setGrade(requireText(student.getGrade(), "年级"));

        if (student.getUserId().length() != 8) {
            throw badRequest("用户账号必须为8位");
        }
        if (!student.getGrade().matches("[0-9]{4}")) {
            throw badRequest("年级必须是4位数字，例如2024");
        }
        if (student.getStatus() == null) {
            student.setStatus(StudentStatus.ENROLLED);
        }
        if (student.getEnrollmentDate() != null
                && student.getEnrollmentDate().isAfter(LocalDate.now())) {
            throw badRequest("入学日期不能晚于当前日期");
        }
    }

    private static String checkLength(String value, String fieldName, int maxLength)
            throws StudentServiceException {
        String text = requireText(value, fieldName);
        if (text.length() > maxLength) {
            throw badRequest(fieldName + "不能超过" + maxLength + "个字符");
        }
        return text;
    }

    private static String requireText(String value, String fieldName)
            throws StudentServiceException {
        if (value == null || value.trim().isEmpty()) {
            throw badRequest(fieldName + "不能为空");
        }
        return value.trim();
    }

    private static StudentServiceException badRequest(String message) {
        return new StudentServiceException(StudentProtocol.STATUS_BAD_REQUEST, message);
    }

    private static StudentServiceException notFound(String message) {
        return new StudentServiceException(StudentProtocol.STATUS_NOT_FOUND, message);
    }

    private static StudentServiceException conflict(String message) {
        return new StudentServiceException(StudentProtocol.STATUS_CONFLICT, message);
    }
}
