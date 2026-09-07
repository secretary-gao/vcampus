/*
 * Course
 *
 * Version 1.0
 *
 * 2026-09-02
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.common.vo;

import java.io.Serializable;

/**
 * 课程实体类，对应数据库 tblCourse 表，用于在客户端与服务器端之间传输课程信息。
 * 因为课程信息后续需要通过 Socket 传输，所以实现 {@link Serializable} 接口。
 *
 * <p>字段设计对应共享说明书中的 tblCourse 表：courseId（课程号）、
 * courseName（课程名）、teacher（授课教师）、credit（学分）、capacity（课容量）和
 * selectedCount（已选人数）。</p>
 */
public class Course implements Serializable {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 课程号（主键）。 */
    private String _courseId;

    /** 课程名称。 */
    private String _courseName;

    /** 授课教师。 */
    private String _teacher;

    /** 学分（大于 0）。 */
    private int _credit;

    /** 课程容量（大于 0）。 */
    private int _capacity;

    /** 已选人数（不小于 0，且不超过课程容量）。 */
    private int _selectedCount;

    /**
     * 无参构造方法。
     */
    public Course() {
    }

    /**
     * 全参构造方法。
     *
     * @param courseId     课程号
     * @param courseName   课程名称
     * @param teacher      授课教师
     * @param credit       学分
     * @param capacity     课程容量
     * @param selectedCount 已选人数
     */
    public Course(String courseId, String courseName, String teacher, int credit,
                  int capacity, int selectedCount) {
        this._courseId = courseId;
        this._courseName = courseName;
        this._teacher = teacher;
        this._credit = credit;
        this._capacity = capacity;
        this._selectedCount = selectedCount;
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
     * 获取课程名称。
     *
     * @return 课程名称
     */
    public String getCourseName() {
        return _courseName;
    }

    /**
     * 设置课程名称。
     *
     * @param courseName 课程名称
     */
    public void setCourseName(String courseName) {
        this._courseName = courseName;
    }

    /**
     * 获取授课教师。
     *
     * @return 授课教师
     */
    public String getTeacher() {
        return _teacher;
    }

    /**
     * 设置授课教师。
     *
     * @param teacher 授课教师
     */
    public void setTeacher(String teacher) {
        this._teacher = teacher;
    }

    /**
     * 获取学分。
     *
     * @return 学分
     */
    public int getCredit() {
        return _credit;
    }

    /**
     * 设置学分。
     *
     * @param credit 学分
     */
    public void setCredit(int credit) {
        this._credit = credit;
    }

    /**
     * 获取课程容量。
     *
     * @return 课程容量
     */
    public int getCapacity() {
        return _capacity;
    }

    /**
     * 设置课程容量。
     *
     * @param capacity 课程容量
     */
    public void setCapacity(int capacity) {
        this._capacity = capacity;
    }

    /**
     * 获取已选人数。
     *
     * @return 已选人数
     */
    public int getSelectedCount() {
        return _selectedCount;
    }

    /**
     * 设置已选人数。
     *
     * @param selectedCount 已选人数
     */
    public void setSelectedCount(int selectedCount) {
        this._selectedCount = selectedCount;
    }

    /**
     * 返回该课程的可读字符串表示。
     *
     * @return 课程信息的字符串描述
     */
    @Override
    public String toString() {
        return "Course{" +
                "courseId='" + _courseId + '\'' +
                ", courseName='" + _courseName + '\'' +
                ", teacher='" + _teacher + '\'' +
                ", credit=" + _credit +
                ", capacity=" + _capacity +
                ", selectedCount=" + _selectedCount +
                '}';
    }
}
