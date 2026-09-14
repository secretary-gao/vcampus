/*
 * TeachingClass
 *
 * Version 1.0
 *
 * 2026-09-09
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.common.vo;

import java.io.Serializable;

/** A concrete teaching class (section) that students can enroll in. */
public class TeachingClass implements Serializable {

    private static final long serialVersionUID = 1L;

    private String _teachingClassId;
    private String _courseId;
    private String _classNumber;
    private String _teacher;
    private int _capacity;
    private int _selectedCount;
    private String _teachingLanguage;
    private String _remark;

    /** No-argument constructor for serialization. */
    public TeachingClass() {
    }

    /** Creates a teaching class. */
    public TeachingClass(String teachingClassId, String courseId, String classNumber,
                         String teacher, int capacity, int selectedCount,
                         String teachingLanguage, String remark) {
        _teachingClassId = teachingClassId;
        _courseId = courseId;
        _classNumber = classNumber;
        _teacher = teacher;
        _capacity = capacity;
        _selectedCount = selectedCount;
        _teachingLanguage = teachingLanguage;
        _remark = remark;
    }

    public String getTeachingClassId() {
        return _teachingClassId;
    }

    public void setTeachingClassId(String teachingClassId) {
        _teachingClassId = teachingClassId;
    }

    public String getCourseId() {
        return _courseId;
    }

    public void setCourseId(String courseId) {
        _courseId = courseId;
    }

    public String getClassNumber() {
        return _classNumber;
    }

    public void setClassNumber(String classNumber) {
        _classNumber = classNumber;
    }

    public String getTeacher() {
        return _teacher;
    }

    public void setTeacher(String teacher) {
        _teacher = teacher;
    }

    public int getCapacity() {
        return _capacity;
    }

    public void setCapacity(int capacity) {
        _capacity = capacity;
    }

    public int getSelectedCount() {
        return _selectedCount;
    }

    public void setSelectedCount(int selectedCount) {
        _selectedCount = selectedCount;
    }

    public String getTeachingLanguage() {
        return _teachingLanguage;
    }

    public void setTeachingLanguage(String teachingLanguage) {
        _teachingLanguage = teachingLanguage;
    }

    public String getRemark() {
        return _remark;
    }

    public void setRemark(String remark) {
        _remark = remark;
    }
}
