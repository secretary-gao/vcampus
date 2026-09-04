/*
 * DoctorDAO
 *
 * Version 1.0
 *
 * 2026-08-28
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.server.dao;
import vcampus.common.vo.Doctor;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
/**
 * 医生表（tbldoctor）的数据访问类，封装对医生表的增删改查操作。
 * 对上层业务服务层屏蔽具体的 SQL 语句与数据库细节，连接统一由
 * {@link DbHelper} 提供。
 */
public class DoctorDAO {
    /**
     * 查询全部医生信息
     *
     * @return 医生对象列表
     * @throws SQLException 数据库操作异常
     * @throws IOException  数据库配置文件读取异常
     */
    public List<Doctor> selectAll() throws SQLException, IOException {
        String sql = "SELECT doctorId, name, department, title FROM tbldoctor";
        List<Doctor> list = new ArrayList<>();
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }
    /**
     * 根据科室查询医生列表
     *
     * @param department 科室名称
     * @return 同科室医生对象列表
     * @throws SQLException 数据库操作异常
     * @throws IOException  数据库配置文件读取异常
     */
    public List<Doctor> selectByDepartment(String department) throws SQLException, IOException {
        String sql = "SELECT doctorId, name, department, title FROM tbldoctor WHERE department = ?";
        List<Doctor> list = new ArrayList<>();
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, department);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }
    /**
     * 根据doctorId查询单个医生
     *
     * @param doctorId 医生编号
     * @return 查询到的医生对象；若不存在则返回 {@code null}
     * @throws SQLException 数据库操作异常
     * @throws IOException  数据库配置文件读取异常
     */
    public Doctor findById(String doctorId) throws SQLException, IOException {
        String sql = "SELECT doctorId, name, department, title FROM tbldoctor WHERE doctorId = ?";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, doctorId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    /**
     * 新增医生
     * @param doctor
     * @return true成功
     * @throws SQLException
     * @throws IOException
     */
    public boolean insert(Doctor doctor) throws SQLException, IOException{
        String sql = "INSERT INTO tbldoctor(doctorId,name,department,title) VALUES (?,?,?,?)";
        try(Connection conn = DbHelper.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)){
            pstmt.setString(1,doctor.getDoctorId());
            pstmt.setString(2,doctor.getName());
            pstmt.setString(3,doctor.getDepartment());
            pstmt.setString(4,doctor.getTitle());
            return pstmt.executeUpdate()>0;
        }
    }

    /**
     * 修改医生信息
     * @param doctor
     * @return true成功
     * @throws SQLException
     * @throws IOException
     */
    public boolean update(Doctor doctor) throws SQLException, IOException{
        String sql = "UPDATE tbldoctor SET name=?,department=?,title=? WHERE doctorId=?";
        try(Connection conn = DbHelper.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)){
            pstmt.setString(1,doctor.getName());
            pstmt.setString(2,doctor.getDepartment());
            pstmt.setString(3,doctor.getTitle());
            pstmt.setString(4,doctor.getDoctorId());
            return pstmt.executeUpdate()>0;
        }
    }

    /**
     * 删除医生
     * @param doctorId
     * @return true成功
     * @throws SQLException
     * @throws IOException
     */
    public boolean delete(String doctorId) throws SQLException, IOException{
        String sql = "DELETE FROM tbldoctor WHERE doctorId=?";
        try(Connection conn = DbHelper.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)){
            pstmt.setString(1,doctorId);
            return pstmt.executeUpdate()>0;
        }
    }

    /**
     * 将结果集当前行映射为 Doctor 对象。
     *
     * @param rs 指向当前行的结果集
     * @return 映射后的 Doctor 对象
     * @throws SQLException 读取结果集时发生异常
     */
    private Doctor mapRow(ResultSet rs) throws SQLException, IOException {
        Doctor doctor = new Doctor();
        doctor.setDoctorId(rs.getString("doctorId"));
        doctor.setName(rs.getString("name"));
        doctor.setDepartment(rs.getString("department"));
        doctor.setTitle(rs.getString("title"));
        return doctor;
    }

    /**
 * 查询所有不存在待就诊预约的医生（允许管理员安全删除）
 */
/**
 * 查询所有【完全没有任何预约记录】的医生（允许管理员安全删除）
 * 只要tblappointment存在该医生任意记录（已取消/待就诊都算）就不会出现在列表
 */
public List<Doctor> selectCanDeleteDoctor() throws SQLException, IOException {
    List<Doctor> doctorList = new ArrayList<>();
    // 去掉 AND a.status='待就诊'，只要存在任意一条预约记录就排除
    String sql = "SELECT d.doctorId, d.name, d.department, d.title " +
            "FROM tbldoctor d " +
            "WHERE NOT EXISTS (" +
            "    SELECT 1 FROM tblappointment a " +
            "    WHERE a.doctorId = d.doctorId" +
            ")";
    try (Connection conn = DbHelper.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql);
         ResultSet rs = pstmt.executeQuery()) {
        while (rs.next()) {
            Doctor doctor = new Doctor();
            doctor.setDoctorId(rs.getString("doctorId"));
            doctor.setName(rs.getString("name"));
            doctor.setDepartment(rs.getString("department"));
            doctor.setTitle(rs.getString("title"));
            doctorList.add(doctor);
        }
    }
    return doctorList;
}


}


