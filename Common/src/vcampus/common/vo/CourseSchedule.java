/*
 * CourseSchedule
 *
 * Version 1.0
 *
 * 2026-09-04
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.common.vo;

import java.io.Serializable;
import java.time.LocalTime;

/**
 * 课程排课实体类，对应 tblCourseSchedule。排课关联具体教学班，授课教师由
 * {@link TeachingClass} 提供，不在排课表重复保存。
 */
public class CourseSchedule implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 排课记录号。 */
    private String _scheduleId;

    /** 课程号。 */
    private String _courseId;

    /** 所属教学班。 */
    private String _teachingClassId;

    /** 起止教学周。 */
    private int _weekStart;
    private int _weekEnd;

    /** 起止节次。 */
    private int _startPeriod;
    private int _endPeriod;

    /** 教室。 */
    private String _classroom;

    /** 星期，1 表示星期一，7 表示星期日。 */
    private int _dayOfWeek;

    /** 开始时间。 */
    private LocalTime _startTime;

    /** 结束时间。 */
    private LocalTime _endTime;

    /** 无参构造方法。 */
    public CourseSchedule() {
    }

    /** 全参构造方法。 */
    public CourseSchedule(String scheduleId, String courseId, String classroom,
                          int dayOfWeek, LocalTime startTime, LocalTime endTime) {
        this._scheduleId = scheduleId;
        this._courseId = courseId;
        this._classroom = classroom;
        this._dayOfWeek = dayOfWeek;
        this._startTime = startTime;
        this._endTime = endTime;
    }

    public String getScheduleId() {
        return _scheduleId;
    }

    public void setScheduleId(String scheduleId) {
        this._scheduleId = scheduleId;
    }

    public String getCourseId() {
        return _courseId;
    }

    public void setCourseId(String courseId) {
        this._courseId = courseId;
    }

    public String getTeachingClassId() {
        return _teachingClassId;
    }

    public void setTeachingClassId(String teachingClassId) {
        _teachingClassId = teachingClassId;
    }

    public int getWeekStart() {
        return _weekStart;
    }

    public void setWeekStart(int weekStart) {
        _weekStart = weekStart;
    }

    public int getWeekEnd() {
        return _weekEnd;
    }

    public void setWeekEnd(int weekEnd) {
        _weekEnd = weekEnd;
    }

    public int getStartPeriod() {
        return _startPeriod;
    }

    public void setStartPeriod(int startPeriod) {
        _startPeriod = startPeriod;
    }

    public int getEndPeriod() {
        return _endPeriod;
    }

    public void setEndPeriod(int endPeriod) {
        _endPeriod = endPeriod;
    }

    public String getClassroom() {
        return _classroom;
    }

    public void setClassroom(String classroom) {
        this._classroom = classroom;
    }

    public int getDayOfWeek() {
        return _dayOfWeek;
    }

    public void setDayOfWeek(int dayOfWeek) {
        this._dayOfWeek = dayOfWeek;
    }

    public LocalTime getStartTime() {
        return _startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this._startTime = startTime;
    }

    public LocalTime getEndTime() {
        return _endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this._endTime = endTime;
    }

    /** 返回便于调试的排课信息。 */
    @Override
    public String toString() {
        return "CourseSchedule{" +
                "scheduleId='" + _scheduleId + '\'' +
                ", courseId='" + _courseId + '\'' +
                ", classroom='" + _classroom + '\'' +
                ", dayOfWeek=" + _dayOfWeek +
                ", startTime=" + _startTime +
                ", endTime=" + _endTime +
                '}';
    }
}
