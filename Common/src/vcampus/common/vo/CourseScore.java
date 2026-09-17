package vcampus.common.vo;

import java.io.Serializable;
import java.time.LocalDateTime;

/** A teacher-submitted score awaiting (or having received) academic approval. */
public class CourseScore implements Serializable {
    private static final long serialVersionUID = 1L;
    private String scoreId;
    private String studentId;
    private String studentName;
    private String courseId;
    private String courseName;
    private String teachingClassId;
    private String teacher;
    private Integer score;
    private String status;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;

    public String getScoreId() { return scoreId; }
    public void setScoreId(String value) { scoreId = value; }
    public String getStudentId() { return studentId; }
    public void setStudentId(String value) { studentId = value; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String value) { studentName = value; }
    public String getCourseId() { return courseId; }
    public void setCourseId(String value) { courseId = value; }
    public String getCourseName() { return courseName; }
    public void setCourseName(String value) { courseName = value; }
    public String getTeachingClassId() { return teachingClassId; }
    public void setTeachingClassId(String value) { teachingClassId = value; }
    public String getTeacher() { return teacher; }
    public void setTeacher(String value) { teacher = value; }
    public Integer getScore() { return score; }
    public void setScore(Integer value) { score = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime value) { submittedAt = value; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime value) { reviewedAt = value; }
}
