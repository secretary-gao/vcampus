package vcampus.server.srv;

import vcampus.common.vo.Student;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/** 服务器端学籍业务接口。 */
public interface IStudentServerSrv {

    List<Student> findAll() throws SQLException, IOException;

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

    void deleteStudent(String studentId)
            throws SQLException, IOException, StudentServiceException;
}
