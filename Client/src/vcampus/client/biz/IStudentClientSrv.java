package vcampus.client.biz;

import vcampus.common.vo.Student;

import java.io.IOException;
import java.util.List;

/** JavaFX 学籍界面调用的客户端业务接口。 */
public interface IStudentClientSrv {

    List<Student> findAll()
            throws IOException, ClassNotFoundException, StudentClientException;

    Student findByStudentId(String studentId)
            throws IOException, ClassNotFoundException, StudentClientException;

    Student findByCampusCardNo(String campusCardNo)
            throws IOException, ClassNotFoundException, StudentClientException;

    List<Student> findByName(String name)
            throws IOException, ClassNotFoundException, StudentClientException;

    Student addStudent(Student student)
            throws IOException, ClassNotFoundException, StudentClientException;

    Student updateStudent(Student student)
            throws IOException, ClassNotFoundException, StudentClientException;

    void deleteStudent(String studentId)
            throws IOException, ClassNotFoundException, StudentClientException;
}
