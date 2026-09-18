package vcampus.common.vo;

import java.io.Serializable;
import java.time.LocalDateTime;

/** 教师关注的学生关系。 */
public class StudentFocus implements Serializable {
    private static final long serialVersionUID = 1L;

    private String _teacherUserId;
    private String _studentId;
    private String _tags;
    private String _note;
    private LocalDateTime _createdAt;

    public StudentFocus() {
    }

    public StudentFocus(String teacherUserId, String studentId, String tags, String note,
                        LocalDateTime createdAt) {
        _teacherUserId = teacherUserId;
        _studentId = studentId;
        _tags = tags;
        _note = note;
        _createdAt = createdAt;
    }

    public String getTeacherUserId() { return _teacherUserId; }
    public void setTeacherUserId(String value) { _teacherUserId = value; }
    public String getStudentId() { return _studentId; }
    public void setStudentId(String value) { _studentId = value; }
    public String getTags() { return _tags; }
    public void setTags(String value) { _tags = value; }
    public String getNote() { return _note; }
    public void setNote(String value) { _note = value; }
    public LocalDateTime getCreatedAt() { return _createdAt; }
    public void setCreatedAt(LocalDateTime value) { _createdAt = value; }
}
