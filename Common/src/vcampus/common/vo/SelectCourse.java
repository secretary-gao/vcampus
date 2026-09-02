/*
 * SelectCourse
 *
 * Version 1.0
 *
 * 2026-09-02
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.common.vo;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 选课记录实体类，对应数据库 tblSelectCourse 表，用于在客户端与服务器端之间
 * 传输学生选课记录。因为选课记录后续需要通过 Socket 传输，所以实现
 * {@link Serializable} 接口。
 *
 * <p>字段设计对应共享说明书中的 tblSelectCourse 表：selectId（选课记录号）、
 * studentId（学号）、courseId（课程号）和 selectTime（选课时间）。</p>
 */
public class SelectCourse implements Serializable {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 选课记录号（主键）。 */
    private String _selectId;

    /** 学号，后续外键关联 tblStudent.studentId。 */
    private String _studentId;

    /** 课程号（外键关联 tblCourse.courseId）。 */
    private String _courseId;

    /** 选课时间。 */
    private LocalDateTime _selectTime;

    /**
     * 无参构造方法。
     */
    public SelectCourse() {
    }

    /**
     * 全参构造方法。
     *
     * @param selectId  选课记录号
     * @param studentId 学号
     * @param courseId  课程号
     * @param selectTime 选课时间
     */
    public SelectCourse(String selectId, String studentId, String courseId,
                        LocalDateTime selectTime) {
        this._selectId = selectId;
        this._studentId = studentId;
        this._courseId = courseId;
        this._selectTime = selectTime;
    }

    /**
     * 获取选课记录号。
     *
     * @return 选课记录号
     */
    public String getSelectId() {
        return _selectId;
    }

    /**
     * 设置选课记录号。
     *
     * @param selectId 选课记录号
     */
    public void setSelectId(String selectId) {
        this._selectId = selectId;
    }

    /**
     * 获取学号。
     *
     * @return 学号
     */
    public String getStudentId() {
        return _studentId;
    }

    /**
     * 设置学号。
     *
     * @param studentId 学号
     */
    public void setStudentId(String studentId) {
        this._studentId = studentId;
    }

    /**
     * 获取课程号。
     *
     * @return 课程号
     */
    public String getCourseId() {
        return _courseId;
    }

    /**
     * 设置课程号。
     *
     * @param courseId 课程号
     */
    public void setCourseId(String courseId) {
        this._courseId = courseId;
    }

    /**
     * 获取选课时间。
     *
     * @return 选课时间
     */
    public LocalDateTime getSelectTime() {
        return _selectTime;
    }

    /**
     * 设置选课时间。
     *
     * @param selectTime 选课时间
     */
    public void setSelectTime(LocalDateTime selectTime) {
        this._selectTime = selectTime;
    }

    /**
     * 返回该选课记录的可读字符串表示。
     *
     * @return 选课记录的字符串描述
     */
    @Override
    public String toString() {
        return "SelectCourse{" +
                "selectId='" + _selectId + '\'' +
                ", studentId='" + _studentId + '\'' +
                ", courseId='" + _courseId + '\'' +
                ", selectTime=" + _selectTime +
                '}';
    }
}
