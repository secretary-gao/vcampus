/*
 * TeacherCourseEnrollment
 *
 * Version 1.0
 *
 * 2026-09-07
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.common.vo;

import java.io.Serializable;
import java.time.LocalDateTime;

/** 教师端课程名单行；没有学生选课时学生字段为空。 */
public class TeacherCourseEnrollment implements Serializable {

    private static final long serialVersionUID = 1L;

    private String _courseId;
    private String _courseName;
    private String _teachingClassId;
    private String _classNumber;
    private String _studentId;
    private String _studentName;
    private String _className;
    private String _major;
    private LocalDateTime _selectTime;

    /** 无参构造方法。 */
    public TeacherCourseEnrollment() {
    }

    /** 全参构造方法。 */
    public TeacherCourseEnrollment(String courseId, String courseName, String studentId,
                                   String studentName, String className, String major,
                                   LocalDateTime selectTime) {
        this._courseId = courseId;
        this._courseName = courseName;
        this._studentId = studentId;
        this._studentName = studentName;
        this._className = className;
        this._major = major;
        this._selectTime = selectTime;
    }

    public String getCourseId() {
        return _courseId;
    }

    public void setCourseId(String courseId) {
        this._courseId = courseId;
    }

    public String getCourseName() {
        return _courseName;
    }

    public void setCourseName(String courseName) {
        this._courseName = courseName;
    }

    public String getTeachingClassId() {
        return _teachingClassId;
    }

    public void setTeachingClassId(String teachingClassId) {
        _teachingClassId = teachingClassId;
    }

    public String getClassNumber() {
        return _classNumber;
    }

    public void setClassNumber(String classNumber) {
        _classNumber = classNumber;
    }

    public String getStudentId() {
        return _studentId;
    }

    public void setStudentId(String studentId) {
        this._studentId = studentId;
    }

    public String getStudentName() {
        return _studentName;
    }

    public void setStudentName(String studentName) {
        this._studentName = studentName;
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

    public LocalDateTime getSelectTime() {
        return _selectTime;
    }

    public void setSelectTime(LocalDateTime selectTime) {
        this._selectTime = selectTime;
    }
}
