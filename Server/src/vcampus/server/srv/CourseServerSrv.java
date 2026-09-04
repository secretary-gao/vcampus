/*
 * CourseServerSrv
 *
 * Version 1.0
 *
 * 2026-09-04
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.server.srv;

import vcampus.common.vo.Course;
import vcampus.common.vo.SelectCourse;
import vcampus.server.dao.CourseDAO;
import vcampus.server.dao.DbHelper;
import vcampus.server.dao.SelectCourseDAO;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * {@link ICourseServerSrv} 的实现类。课程查询直接委托 DAO；选课和退课负责业务校验，
 * 并在同一个 JDBC 事务内同步修改选课记录与课程已选人数。
 */
public class CourseServerSrv implements ICourseServerSrv {

    /** 课程数据访问对象。 */
    private final CourseDAO _courseDAO;

    /** 选课记录数据访问对象。 */
    private final SelectCourseDAO _selectCourseDAO;

    /**
     * 使用默认 DAO 创建业务服务。
     */
    public CourseServerSrv() {
        this(new CourseDAO(), new SelectCourseDAO());
    }

    /**
     * 注入 DAO，便于独立验证事务回滚。
     *
     * @param courseDAO       课程 DAO
     * @param selectCourseDAO 选课记录 DAO
     */
    CourseServerSrv(CourseDAO courseDAO, SelectCourseDAO selectCourseDAO) {
        this._courseDAO = courseDAO;
        this._selectCourseDAO = selectCourseDAO;
    }

    /** {@inheritDoc} */
    @Override
    public List<Course> queryCourse(String keyword) throws SQLException, IOException {
        return _courseDAO.findByKeyword(keyword);
    }

    /** {@inheritDoc} */
    @Override
    public boolean selectCourse(String studentId, String courseId)
            throws SQLException, IOException, CourseServiceException {
        validateIds(studentId, courseId);
        String normalizedStudentId = studentId.trim();
        String normalizedCourseId = courseId.trim();

        try (Connection conn = DbHelper.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Course course = _courseDAO.findByIdForUpdate(conn, normalizedCourseId);
                if (course == null) {
                    throw new CourseServiceException("课程不存在：" + normalizedCourseId);
                }
                if (_selectCourseDAO.findByStudentAndCourse(
                        conn, normalizedStudentId, normalizedCourseId) != null) {
                    throw new CourseServiceException("请勿重复选择同一门课程");
                }
                if (course.getSelectedCount() >= course.getCapacity()) {
                    throw new CourseServiceException("课程已满");
                }

                SelectCourse record = new SelectCourse(
                        newSelectId(), normalizedStudentId, normalizedCourseId, LocalDateTime.now());
                if (!_selectCourseDAO.insertSelectCourse(conn, record)) {
                    throw new SQLException("插入选课记录失败");
                }

                course.setSelectedCount(course.getSelectedCount() + 1);
                if (!_courseDAO.updateCourse(conn, course)) {
                    throw new SQLException("更新课程已选人数失败");
                }

                conn.commit();
                return true;
            } catch (SQLException | CourseServiceException | RuntimeException e) {
                rollback(conn, e);
                throw e;
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean dropCourse(String studentId, String courseId)
            throws SQLException, IOException, CourseServiceException {
        validateIds(studentId, courseId);
        String normalizedStudentId = studentId.trim();
        String normalizedCourseId = courseId.trim();

        try (Connection conn = DbHelper.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Course course = _courseDAO.findByIdForUpdate(conn, normalizedCourseId);
                if (course == null) {
                    throw new CourseServiceException("课程不存在：" + normalizedCourseId);
                }
                if (_selectCourseDAO.findByStudentAndCourse(
                        conn, normalizedStudentId, normalizedCourseId) == null) {
                    throw new CourseServiceException("未找到对应选课记录");
                }
                if (course.getSelectedCount() <= 0) {
                    throw new CourseServiceException("课程已选人数异常，不能继续退课");
                }

                if (!_selectCourseDAO.deleteSelectCourse(
                        conn, normalizedStudentId, normalizedCourseId)) {
                    throw new SQLException("删除选课记录失败");
                }
                course.setSelectedCount(course.getSelectedCount() - 1);
                if (!_courseDAO.updateCourse(conn, course)) {
                    throw new SQLException("更新课程已选人数失败");
                }

                conn.commit();
                return true;
            } catch (SQLException | CourseServiceException | RuntimeException e) {
                rollback(conn, e);
                throw e;
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public List<SelectCourse> querySelectedCourse(String studentId)
            throws SQLException, IOException, CourseServiceException {
        if (studentId == null || studentId.isBlank()) {
            throw new CourseServiceException("学号不能为空");
        }
        return _selectCourseDAO.findByStudentId(studentId.trim());
    }

    /** 校验选课和退课使用的业务标识。 */
    private void validateIds(String studentId, String courseId) throws CourseServiceException {
        if (studentId == null || studentId.isBlank()) {
            throw new CourseServiceException("学号不能为空");
        }
        if (courseId == null || courseId.isBlank()) {
            throw new CourseServiceException("课程号不能为空");
        }
    }

    /** 生成不超过 varchar(20) 的选课记录号。 */
    private String newSelectId() {
        return "SC" + UUID.randomUUID().toString().replace("-", "").substring(0, 18);
    }

    /** 回滚事务；若回滚本身失败，将异常附加到原异常上。 */
    private void rollback(Connection conn, Exception cause) {
        try {
            conn.rollback();
        } catch (SQLException rollbackError) {
            cause.addSuppressed(rollbackError);
        }
    }
}
