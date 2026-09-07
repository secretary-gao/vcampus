package vcampus.server.dao;
import vcampus.common.vo.Appointment;
import vcampus.common.vo.Doctor;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
public class HospitalDAO {
    //================ 医生相关方法 =================
    public List<Doctor> queryAllDoctor() {
        List<Doctor> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DbHelper.getConnection();
            String sql = "SELECT doctorId,name,department,title FROM tbldoctor";
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                Doctor d = new Doctor();
                d.setDoctorId(rs.getString("doctorId"));
                d.setName(rs.getString("name"));
                d.setDepartment(rs.getString("department"));
                d.setTitle(rs.getString("title"));
                list.add(d);
            }
        } catch (Exception e) {
            // 同时捕获 SQLException + IOException，和UserDAO保持一致
            e.printStackTrace();
        } finally {
            DbHelper.close(conn, pstmt, rs);
        }
        return list;
    }
    public Doctor findDoctorById(String doctorId) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DbHelper.getConnection();
            String sql = "SELECT doctorId,name,department,title FROM tbldoctor WHERE doctorId=?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, doctorId);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                Doctor d = new Doctor();
                d.setDoctorId(rs.getString("doctorId"));
                d.setName(rs.getString("name"));
                d.setDepartment(rs.getString("department"));
                d.setTitle(rs.getString("title"));
                return d;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            DbHelper.close(conn, pstmt, rs);
        }
        return null;
    }
    //================ 预约挂号方法 =================
    public int addAppointment(Appointment app) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = DbHelper.getConnection();
            String sql = "INSERT INTO tblappointment(appointmentId,userId,doctorId,appointmentTime,status) VALUES (?,?,?,?,?)";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, app.getAppointmentId());
            pstmt.setString(2, app.getUserId());
            pstmt.setString(3, app.getDoctorId());
            pstmt.setTimestamp(4, new Timestamp(app.getAppointmentTime().getTime()));
            pstmt.setString(5, app.getStatus());
            return pstmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            DbHelper.close(conn, pstmt, null);
        }
        return 0;
    }
    public List<Appointment> queryAppointmentByUserId(String userId) {
        List<Appointment> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DbHelper.getConnection();
            String sql = "SELECT appointmentId,userId,doctorId,appointmentTime,status FROM tblappointment WHERE userId=?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, userId);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                Appointment a = new Appointment();
                a.setAppointmentId(rs.getString("appointmentId"));
                a.setUserId(rs.getString("userId"));
                a.setDoctorId(rs.getString("doctorId"));
                a.setAppointmentTime(rs.getTimestamp("appointmentTime"));
                a.setStatus(rs.getString("status"));
                list.add(a);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            DbHelper.close(conn, pstmt, rs);
        }
        return list;
    }
    public Appointment findAppointmentById(String appointmentId) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DbHelper.getConnection();
            String sql = "SELECT appointmentId,userId,doctorId,appointmentTime,status FROM tblappointment WHERE appointmentId=?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, appointmentId);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                Appointment a = new Appointment();
                a.setAppointmentId(rs.getString("appointmentId"));
                a.setUserId(rs.getString("userId"));
                a.setDoctorId(rs.getString("doctorId"));
                a.setAppointmentTime(rs.getTimestamp("appointmentTime"));
                a.setStatus(rs.getString("status"));
                return a;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            DbHelper.close(conn, pstmt, rs);
        }
        return null;
    }
    public int updateAppointmentStatus(String appointmentId, String newStatus) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = DbHelper.getConnection();
            String sql = "UPDATE tblappointment SET status=? WHERE appointmentId=?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, newStatus);
            pstmt.setString(2, appointmentId);
            return pstmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            DbHelper.close(conn, pstmt, null);
        }
        return 0;
    }
    public int deleteAppointment(String appointmentId) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = DbHelper.getConnection();
            String sql = "DELETE FROM tblappointment WHERE appointmentId=?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, appointmentId);
            return pstmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            DbHelper.close(conn, pstmt, null);
        }
        return 0;
    }

}
