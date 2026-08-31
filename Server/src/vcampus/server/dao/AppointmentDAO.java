/*
 * AppointmentDAO
 *
 * Version 1.0
 *
 * 2026-08-28
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.server.dao;
import vcampus.common.vo.Appointment;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;
import java.util.List;



/**
 * 预约表（tblappointment）的数据访问类，封装对预约记录的增删改查操作。
 * 对上层业务服务层屏蔽具体的 SQL 语句与数据库细节，连接统一由
 * {@link DbHelper} 提供。
 */
public class AppointmentDAO {

    /**
     * 生成12位预约编号：AP + yyMMdd(6位) + 4位随机数字，总长度固定12位
     * @return 12位 appointmentId
     */
    private String generateAppointmentId() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd");
        String datePart = sdf.format(new Date());
        // 4位随机数字，不足4位补0
        int rand = ThreadLocalRandom.current().nextInt(10000);
        String randPart = String.format("%04d", rand);
        return "AP" + datePart + randPart;
    }

    /**
     * 新增一条挂号预约记录
     *
     * @param appointment 待插入的预约对象，如果appointmentId为空，DAO内部自动生成12位编号
     * @return 插入成功返回 {@code true}，否则返回 {@code false}
     * @throws SQLException 数据库操作异常
     * @throws IOException  数据库配置文件读取异常
     */
    public boolean insert(Appointment appointment) throws SQLException, IOException {
        // 如果上层没有设置预约ID，则DAO自动生成；上层传了ID就用传入的值（方便测试）
        if(appointment.getAppointmentId() == null || appointment.getAppointmentId().isBlank()){
            appointment.setAppointmentId(generateAppointmentId());
        }

        String sql = "INSERT INTO tblappointment(appointmentId,userId,doctorId,appointmentTime,status) VALUES (?,?,?,?,?)";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, appointment.getAppointmentId());
            pstmt.setString(2, appointment.getUserId());
            pstmt.setString(3, appointment.getDoctorId());
            pstmt.setTimestamp(4, new Timestamp(appointment.getAppointmentTime().getTime()));
            pstmt.setString(5, appointment.getStatus());
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 根据用户编号查询该用户全部预约记录
     *
     * @param userId 用户编号
     * @return 预约记录列表
     * @throws SQLException 数据库操作异常
     * @throws IOException  数据库配置文件读取异常
     */
    public List<Appointment> selectByUserId(String userId) throws SQLException, IOException {
        String sql = "SELECT appointmentId,userId,doctorId,appointmentTime,status FROM tblappointment WHERE userId = ?";
        List<Appointment> list = new ArrayList<>();
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    /**
     * 根据预约编号查询单条预约记录
     *
     * @param appointmentId 预约编号
     * @return 查询到的预约对象；若不存在则返回 {@code null}
     * @throws SQLException 数据库操作异常
     * @throws IOException  数据库配置文件读取异常
     */
    public Appointment findById(String appointmentId) throws SQLException, IOException {
        String sql = "SELECT appointmentId,userId,doctorId,appointmentTime,status FROM tblappointment WHERE appointmentId = ?";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, appointmentId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    /**
     * 更新预约状态
     *
     * @param appointmentId 预约编号
     * @param newStatus     新状态
     * @return 更新成功返回 {@code true}，否则返回 {@code false}
     * @throws SQLException 数据库操作异常
     * @throws IOException  数据库配置文件读取异常
     */
    public boolean updateStatus(String appointmentId, String newStatus) throws SQLException, IOException {
        String sql = "UPDATE tblappointment SET status = ? WHERE appointmentId = ?";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newStatus);
            pstmt.setString(2, appointmentId);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 根据预约编号删除预约（取消预约）
     *
     * @param appointmentId 预约编号
     * @return 删除成功返回 {@code true}，否则返回 {@code false}
     * @throws SQLException 数据库操作异常
     * @throws IOException  数据库配置文件读取异常
     */
    public boolean deleteById(String appointmentId) throws SQLException, IOException {
        String sql = "DELETE FROM tblappointment WHERE appointmentId = ?";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, appointmentId);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 将结果集当前行映射为 Appointment 对象。
     *
     * @param rs 指向当前行的结果集
     * @return 映射后的 Appointment 对象
     * @throws SQLException 读取结果集时发生异常
     */
    private Appointment mapRow(ResultSet rs) throws SQLException {
        Appointment app = new Appointment();
        app.setAppointmentId(rs.getString("appointmentId"));
        app.setUserId(rs.getString("userId"));
        app.setDoctorId(rs.getString("doctorId"));
        app.setAppointmentTime(rs.getTimestamp("appointmentTime"));
        app.setStatus(rs.getString("status"));
        return app;
    }
}
