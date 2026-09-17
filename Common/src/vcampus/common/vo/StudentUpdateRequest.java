package vcampus.common.vo;

import java.io.Serializable;

/** 修改学籍时同时携带原学号，允许管理员更正学号。 */
public class StudentUpdateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String _originalStudentId;
    private final Student _student;

    public StudentUpdateRequest(String originalStudentId, Student student) {
        this._originalStudentId = originalStudentId;
        this._student = student;
    }

    public String getOriginalStudentId() {
        return _originalStudentId;
    }

    public Student getStudent() {
        return _student;
    }
}
