package vcampus.server.dao;

import vcampus.common.vo.Student;
import vcampus.common.vo.StudentStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDate;

/** StudentDAO 的完整增删改查自测，结束时自动清理临时数据。 */
public class StudentDAOTest {

    public static void main(String[] args) throws Exception {
        long suffix = System.currentTimeMillis() % 10_000_000L;
        String userId = "T" + String.format("%07d", suffix);
        String studentId = "S" + String.format("%09d", System.currentTimeMillis() % 1_000_000_000L);
        String changedStudentId = "R" + studentId.substring(1);
        String campusCardNo = "CARD-" + System.currentTimeMillis();

        StudentDAO studentDAO = new StudentDAO();
        try {
            insertTestUser(userId);

            Student student = new Student();
            student.setStudentId(studentId);
            student.setCampusCardNo(campusCardNo);
            student.setUserId(userId);
            student.setName("测试学生");
            student.setClassName("测试班");
            student.setMajor("计算机科学与技术");
            student.setGrade("2024");
            student.setEnrollmentDate(LocalDate.of(2024, 9, 1));
            student.setStatus(StudentStatus.ENROLLED);

            require(studentDAO.insert(student), "新增失败");
            Student saved = studentDAO.findByStudentId(studentId);
            require(saved != null, "按学号查询失败");
            require(studentDAO.findByCampusCardNo(campusCardNo) != null, "按一卡通号查询失败");
            require(studentDAO.findByName("测试学生").stream()
                    .anyMatch(item -> studentId.equals(item.getStudentId())), "按姓名查询失败");

            saved.setClassName("测试班（已修改）");
            saved.setStatus(StudentStatus.SUSPENDED);
            saved.setStudentId(changedStudentId);
            require(studentDAO.update(studentId, saved), "修改失败");
            require(studentDAO.findByStudentId(studentId) == null, "原学号仍能查到记录");
            Student updated = studentDAO.findByStudentId(changedStudentId);
            require(updated != null && "测试班（已修改）".equals(updated.getClassName()),
                    "修改后的数据不正确");
            require(updated.getVersion() == 1, "版本号没有递增");

            require(studentDAO.deleteByStudentId(changedStudentId), "删除失败");
            require(studentDAO.findByStudentId(changedStudentId) == null, "删除后仍能查到记录");
            System.out.println("StudentDAO 自测通过：新增、查询、修改学号、删除均正常");
        } finally {
            cleanup(studentId, changedStudentId, userId);
        }
    }

    private static void insertTestUser(String userId) throws Exception {
        String sql = "INSERT INTO tblUser (uId, uName, uAge, uSex, uPwd, uRole) "
                + "VALUES (?, '测试学生', 20, '男', ?, '学生')";
        try (Connection connection = DbHelper.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, userId);
            statement.setString(2, "00000000000000000000000000000000");
            statement.executeUpdate();
        }
    }

    private static void cleanup(String studentId, String changedStudentId, String userId) {
        try (Connection connection = DbHelper.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM tblStudent WHERE studentId IN (?, ?)")) {
                statement.setString(1, studentId);
                statement.setString(2, changedStudentId);
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM tblUser WHERE uId = ?")) {
                statement.setString(1, userId);
                statement.executeUpdate();
            }
        } catch (Exception exception) {
            System.err.println("清理测试数据失败：" + exception.getMessage());
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
