package vcampus.server.srv;

import vcampus.common.constant.StudentProtocol;
import vcampus.common.vo.Student;
import vcampus.common.vo.StudentCampusOverview;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/** 服务器端学籍业务接口。 */
public interface IStudentServerSrv {

    List<Student> findAll() throws SQLException, IOException;

    List<Student> findStudentsTaughtBy(String teacherUserId)
            throws SQLException, IOException, StudentServiceException;

    Student findStudentTaughtBy(String teacherUserId, String studentId)
            throws SQLException, IOException, StudentServiceException;

    List<Student> findStudentsTaughtByName(String teacherUserId, String name)
            throws SQLException, IOException, StudentServiceException;

    Student findByUserId(String userId)
            throws SQLException, IOException, StudentServiceException;

    Student findByStudentId(String studentId)
            throws SQLException, IOException, StudentServiceException;

    Student findByCampusCardNo(String campusCardNo)
            throws SQLException, IOException, StudentServiceException;

    List<Student> findByName(String name)
            throws SQLException, IOException, StudentServiceException;

    Student addStudent(Student student)
            throws SQLException, IOException, StudentServiceException;

    Student updateStudent(Student student)
            throws SQLException, IOException, StudentServiceException;

    /** 按原学号定位档案，并保存包括新学号在内的修改。 */
    default Student updateStudent(String originalStudentId, Student student)
            throws SQLException, IOException, StudentServiceException {
        return updateStudent(student);
    }

    void deleteStudent(String studentId)
            throws SQLException, IOException, StudentServiceException;

    default StudentCampusOverview loadOverview(String studentId)
            throws SQLException, IOException, StudentServiceException {
        throw new StudentServiceException(StudentProtocol.STATUS_ERROR, "跨模块摘要暂不可用");
    }
}
