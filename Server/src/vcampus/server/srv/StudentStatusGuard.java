package vcampus.server.srv;

import vcampus.common.vo.Student;
import vcampus.common.vo.StudentStatus;
import vcampus.server.dao.StudentDAO;

import java.io.IOException;
import java.sql.SQLException;

/** 供选课、图书馆、校园卡和医院模块复用的学籍状态判断。 */
public final class StudentStatusGuard {

    private static final StudentDAO STUDENT_DAO = new StudentDAO();

    private StudentStatusGuard() {
    }

    /** 学生档案不存在时返回 null；非学生账号不受学籍状态限制。 */
    public static Student findByUserId(String userId) throws SQLException, IOException {
        if (userId == null || userId.isBlank()) {
            return null;
        }
        return STUDENT_DAO.findByUserId(userId.trim());
    }

    public static boolean canUseStudentServices(String userId)
            throws SQLException, IOException {
        Student student = findByUserId(userId);
        return student == null || canUseStudentServices(student.getStatus());
    }

    static boolean canUseStudentServices(StudentStatus status) {
        return status == StudentStatus.ENROLLED;
    }

    public static String denialMessage(String action) {
        return "当前学籍状态无法" + action + "，只有在读学生可以使用该功能";
    }
}
