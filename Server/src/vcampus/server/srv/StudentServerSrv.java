package vcampus.server.srv;

import vcampus.common.constant.StudentProtocol;
import vcampus.common.vo.Student;
import vcampus.common.vo.StudentCampusOverview;
import vcampus.common.vo.StudentStatus;
import vcampus.common.vo.StudentFocus;
import vcampus.common.util.MD5Util;
import vcampus.common.vo.User;
import vcampus.server.dao.UserDAO;
import vcampus.server.dao.StudentDAO;
import vcampus.server.dao.StudentCampusOverviewDAO;
import vcampus.server.dao.StudentFocusDAO;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/** 学籍业务实现：负责输入校验、重复检查和修改冲突判断。 */
public class StudentServerSrv implements IStudentServerSrv {

    private final StudentDAO _studentDAO;
    private final UserDAO _userDAO;
    private final StudentCampusOverviewDAO _overviewDAO;
    private final StudentFocusDAO _focusDAO;

    public StudentServerSrv() {
        this(new StudentDAO(), new UserDAO(), new StudentCampusOverviewDAO(), new StudentFocusDAO());
    }

    StudentServerSrv(StudentDAO studentDAO) {
        this(studentDAO, new UserDAO(), new StudentCampusOverviewDAO(), new StudentFocusDAO());
    }

    StudentServerSrv(StudentDAO studentDAO, UserDAO userDAO) {
        this(studentDAO, userDAO, new StudentCampusOverviewDAO(), new StudentFocusDAO());
    }

    StudentServerSrv(StudentDAO studentDAO, UserDAO userDAO,
                     StudentCampusOverviewDAO overviewDAO) {
        this(studentDAO, userDAO, overviewDAO, new StudentFocusDAO());
    }

    StudentServerSrv(StudentDAO studentDAO, UserDAO userDAO,
                     StudentCampusOverviewDAO overviewDAO, StudentFocusDAO focusDAO) {
        this._studentDAO = studentDAO;
        this._userDAO = userDAO;
        this._overviewDAO = overviewDAO;
        this._focusDAO = focusDAO;
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
        boolean accountCreated = ensureStudentAccount(student);
        boolean inserted = false;
        try {
            ensureUserNotBound(student.getUserId(), student.getStudentId());
            if (!_studentDAO.insert(student)) {
                throw new StudentServiceException(StudentProtocol.STATUS_ERROR, "新增学生失败");
            }
            inserted = true;
            syncAccountStatus(null, student);
        } catch (SQLException | IOException | StudentServiceException exception) {
            if (inserted) {
                try {
                    _studentDAO.deleteByStudentId(student.getStudentId());
                } catch (SQLException | IOException rollbackException) {
                    exception.addSuppressed(rollbackException);
                }
            }
            if (accountCreated) {
                try {
                    _userDAO.deleteByUId(student.getUserId());
                } catch (SQLException | IOException rollbackException) {
                    exception.addSuppressed(rollbackException);
                }
            }
            throw exception;
        }
        return _studentDAO.findByStudentId(student.getStudentId());
    }

    @Override
    public Student updateStudent(Student student)
            throws SQLException, IOException, StudentServiceException {
        return updateStudent(student == null ? null : student.getStudentId(), student);
    }

    @Override
    public Student updateStudent(String originalStudentId, Student student)
            throws SQLException, IOException, StudentServiceException {
        String originalId = requireText(originalStudentId, "原学号");
        normalizeAndValidate(student);
        Student oldStudent = _studentDAO.findByStudentId(originalId);
        if (oldStudent == null) {
            throw notFound("学生不存在：" + originalId);
        }

        if (!originalId.equals(student.getStudentId())
                && _studentDAO.findByStudentId(student.getStudentId()) != null) {
            throw conflict("学号已被其他学生使用：" + student.getStudentId());
        }

        Student sameCardStudent = _studentDAO.findByCampusCardNo(student.getCampusCardNo());
        if (sameCardStudent != null
                && !sameCardStudent.getStudentId().equals(originalId)) {
            throw conflict("一卡通号已被其他学生使用：" + student.getCampusCardNo());
        }
        ensureUserExists(student.getUserId());
        ensureUserNotBound(student.getUserId(), originalId);

        if (!_studentDAO.update(originalId, student)) {
            throw conflict("该学生信息已被其他操作修改，请刷新后重试");
        }
        syncAccountStatus(oldStudent, student);
        return _studentDAO.findByStudentId(student.getStudentId());
    }

    /** 根据学籍状态变化同步学生账号状态；普通编辑不会覆盖管理员手动禁用。 */
    private void syncAccountStatus(Student oldStudent, Student newStudent)
            throws SQLException, IOException, StudentServiceException {
        StudentStatus oldStatus = oldStudent == null ? null : oldStudent.getStatus();
        String targetStatus = accountStatusForTransition(oldStatus, newStudent.getStatus());
        if (targetStatus == null) {
            return;
        }
        if (!_userDAO.updateStatus(newStudent.getUserId(), targetStatus)) {
            throw new StudentServiceException(StudentProtocol.STATUS_ERROR,
                    "学籍已保存，但学生账号状态同步失败，请联系管理员处理");
        }
    }

    /** 返回需要写入账号表的状态；没有需要联动时返回 null。 */
    static String accountStatusForTransition(StudentStatus oldStatus, StudentStatus newStatus) {
        if (newStatus == StudentStatus.GRADUATED || newStatus == StudentStatus.WITHDRAWN) {
            if (oldStatus != newStatus) {
                return User.STATUS_DISABLED;
            }
            return null;
        }
        if (newStatus == StudentStatus.ENROLLED
                && (oldStatus == StudentStatus.GRADUATED || oldStatus == StudentStatus.WITHDRAWN)) {
            return User.STATUS_NORMAL;
        }
        return null;
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

    @Override
    public StudentCampusOverview loadOverview(String studentId)
            throws SQLException, IOException, StudentServiceException {
        Student student = findByStudentId(requireText(studentId, "学号"));
        if (student == null) {
            throw notFound("学生不存在：" + studentId);
        }
        return _overviewDAO.load(student);
    }

    @Override
    public List<StudentFocus> listStudentFocus(String teacherUserId)
            throws SQLException, IOException, StudentServiceException {
        return _focusDAO.findByTeacher(requireText(teacherUserId, "教师账号"));
    }

    @Override
    public void addStudentFocus(String teacherUserId, String studentId, String tags, String note)
            throws SQLException, IOException, StudentServiceException {
        String teacher = requireText(teacherUserId, "教师账号");
        String student = requireText(studentId, "学号");
        if (_studentDAO.findStudentTaughtBy(teacher, student) == null) {
            throw new StudentServiceException(StudentProtocol.STATUS_FORBIDDEN,
                    "只能关注自己所授课程中的学生");
        }
        _focusDAO.add(teacher, student, tags, note);
    }

    @Override
    public void removeStudentFocus(String teacherUserId, String studentId)
            throws SQLException, IOException, StudentServiceException {
        _focusDAO.remove(requireText(teacherUserId, "教师账号"), requireText(studentId, "学号"));
    }

    /**
     * 确保学籍绑定的是学生账号。管理员新增学籍时，如果账号尚不存在，
     * 自动创建正常状态的学生账号，初始密码为 123456。
     *
     * @return 是否由本次操作新建了账号
     */
    private boolean ensureStudentAccount(Student student)
            throws SQLException, IOException, StudentServiceException {
        User existing = _userDAO.findByUId(student.getUserId());
        if (existing != null) {
            if (!"学生".equals(existing.getURole())) {
                throw new StudentServiceException(StudentProtocol.STATUS_BAD_REQUEST,
                        "用户账号已存在且不是学生账号：" + student.getUserId());
            }
            return false;
        }

        User account = new User();
        account.setUId(student.getUserId());
        account.setUName(student.getName());
        account.setUPwd(MD5Util.md5("123456"));
        account.setURole("学生");
        account.setUStatus(User.STATUS_NORMAL);
        if (!_userDAO.insert(account)) {
            throw new StudentServiceException(StudentProtocol.STATUS_ERROR,
                    "学生账号创建失败，请稍后重试");
        }
        return true;
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
        if (student.getEnrollmentDate() == null) {
            throw badRequest("入学日期不能为空");
        }
        if (student.getStatus() == null) {
            throw badRequest("学籍状态不能为空");
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
