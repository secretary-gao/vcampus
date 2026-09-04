package vcampus.common.vo;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 客户端、服务器端共用的学生学籍信息实体。 */
public class Student implements Serializable {

    private static final long serialVersionUID = 1L;

    private String _studentId;
    private String _campusCardNo;
    private String _userId;
    private String _name;
    private String _className;
    private String _major;
    private String _grade;
    private LocalDate _enrollmentDate;
    private StudentStatus _status;
    private long _version;
    private LocalDateTime _updatedAt;

    public Student() {
    }

    public String getStudentId() {
        return _studentId;
    }

    public void setStudentId(String studentId) {
        this._studentId = studentId;
    }

    public String getCampusCardNo() {
        return _campusCardNo;
    }

    public void setCampusCardNo(String campusCardNo) {
        this._campusCardNo = campusCardNo;
    }

    public String getUserId() {
        return _userId;
    }

    public void setUserId(String userId) {
        this._userId = userId;
    }

    public String getName() {
        return _name;
    }

    public void setName(String name) {
        this._name = name;
    }

    public String getClassName() {
        return _className;
    }

    public void setClassName(String className) {
        this._className = className;
    }

    public String getMajor() {
        return _major;
    }

    public void setMajor(String major) {
        this._major = major;
    }

    public String getGrade() {
        return _grade;
    }

    public void setGrade(String grade) {
        this._grade = grade;
    }

    public LocalDate getEnrollmentDate() {
        return _enrollmentDate;
    }

    public void setEnrollmentDate(LocalDate enrollmentDate) {
        this._enrollmentDate = enrollmentDate;
    }

    public StudentStatus getStatus() {
        return _status;
    }

    public void setStatus(StudentStatus status) {
        this._status = status;
    }

    public long getVersion() {
        return _version;
    }

    public void setVersion(long version) {
        this._version = version;
    }

    public LocalDateTime getUpdatedAt() {
        return _updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this._updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "Student{" +
                "studentId='" + _studentId + '\'' +
                ", campusCardNo='" + _campusCardNo + '\'' +
                ", name='" + _name + '\'' +
                ", className='" + _className + '\'' +
                ", major='" + _major + '\'' +
                ", grade='" + _grade + '\'' +
                ", status=" + _status +
                ", version=" + _version +
                '}';
    }
}
