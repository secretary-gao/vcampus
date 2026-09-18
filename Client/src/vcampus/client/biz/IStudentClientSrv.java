package vcampus.client.biz;

import vcampus.common.vo.Student;
import vcampus.common.vo.StudentCampusOverview;
import vcampus.common.vo.StudentFocus;

import java.io.IOException;
import java.util.List;

/** JavaFX 学籍界面调用的客户端业务接口。 */
public interface IStudentClientSrv {

    List<Student> findAll()
            throws IOException, ClassNotFoundException, StudentClientException;

    Student getMyStudentInfo()
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

    /** 按原学号定位档案，并保存包括新学号在内的修改。 */
    default Student updateStudent(String originalStudentId, Student student)
            throws IOException, ClassNotFoundException, StudentClientException {
        return updateStudent(student);
    }

    void deleteStudent(String studentId)
            throws IOException, ClassNotFoundException, StudentClientException;

    StudentCampusOverview loadOverview(String studentId)
            throws IOException, ClassNotFoundException, StudentClientException;

    List<StudentFocus> listFocusedStudents()
            throws IOException, ClassNotFoundException, StudentClientException;

    default void addStudentFocus(String studentId, String note)
            throws IOException, ClassNotFoundException, StudentClientException {
        addStudentFocus(studentId, null, note);
    }

    void addStudentFocus(String studentId, String tags, String note)
            throws IOException, ClassNotFoundException, StudentClientException;

    void removeStudentFocus(String studentId)
            throws IOException, ClassNotFoundException, StudentClientException;
}
