/*
 * HospitalServerSrv
 *
 * Version 1.0
 *
 * 2026-08-31
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.server.srv;
import vcampus.common.vo.Appointment;
import vcampus.common.vo.Doctor;
import vcampus.server.dao.AppointmentDAO;
import vcampus.server.dao.DoctorDAO;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Date;

/**
 * {@link IHospitalServerSrv} 的实现类，承载医院模块业务逻辑，
 * 数据库读写委托给 DoctorDAO 和 AppointmentDAO。
 */
public class HospitalServerSrv implements IHospitalServerSrv {
    private final DoctorDAO _doctorDAO = new DoctorDAO();
    private final AppointmentDAO _appointDAO = new AppointmentDAO();

    @Override
    public List<Doctor> queryAllDoctor() throws SQLException, IOException {
        return _doctorDAO.selectAll();
    }
    @Override
    public List<Doctor> queryDoctorByDept(String department) throws SQLException, IOException {
        return _doctorDAO.selectByDepartment(department);
    }
    
    @Override
    public boolean addAppointment(Appointment appoint) throws SQLException, IOException {
    if(!StudentStatusGuard.canUseStudentServices(appoint.getUserId())){
        throw new IOException(StudentStatusGuard.denialMessage("预约校医院"));
    }
    Date appointTime = appoint.getAppointmentTime();
    Date now = new Date();
    //①校验：不能预约过去时间
    if(appointTime.before(now)){
        throw new IOException("无法选择过去的时间");
    }
    //②校验：同一用户该时间已经预约
    boolean repeat = _appointDAO.existSameTimeAppointment(appoint.getUserId(), appoint.getAppointmentTime());
    if(repeat){
        throw new IOException("该时段该医生已有预约");
    }
    return _appointDAO.insert(appoint);
}


   @Override
public List<Appointment> queryMyAppointment(String userId) throws SQLException, IOException {
    // 查询个人预约前：自动刷新过期预约状态
    _appointDAO.autoUpdateExpiredAppointment();
    return _appointDAO.selectByUserId(userId);
}

@Override
public List<Appointment> queryAllAppointment() throws SQLException, IOException {
    // 查询全部预约前：自动刷新过期预约状态
    _appointDAO.autoUpdateExpiredAppointment();
    return _appointDAO.selectAll();
}

    @Override
public int autoUpdateExpiredAppointment() throws SQLException, IOException {
    return _appointDAO.autoUpdateExpiredAppointment();
}

    @Override
    public Appointment findById(String appointId) throws SQLException, IOException {
        return _appointDAO.findById(appointId);
    }

    @Override
    public boolean cancelAppointment(String loginUserId, String appointId) throws SQLException, IOException {
        Appointment apt = _appointDAO.findById(appointId);
        //记录不存在
        if(apt == null){
            return false;
        }
        //权限：只能取消自己的预约
        if(!loginUserId.equals(apt.getUserId())){
            return false;
        }
        //状态：只有待就诊可以取消
        if(!"待就诊".equals(apt.getStatus())){
            return false;
        }
        return _appointDAO.updateStatus(appointId, "已取消");
    }

    @Override
    public boolean addDoctor(Doctor doctor) throws SQLException, IOException {
        return _doctorDAO.insert(doctor);
    }
    @Override
    public boolean updateDoctor(Doctor doctor) throws SQLException, IOException {
        return _doctorDAO.update(doctor);
    }
    @Override
    public boolean deleteDoctor(String doctorId) throws SQLException, IOException {
        return _doctorDAO.delete(doctorId);
    }

    @Override
   public List<Doctor> queryCanDeleteDoctor() throws SQLException, IOException {
    DoctorDAO doctorDAO = new DoctorDAO();
    return doctorDAO.selectCanDeleteDoctor();
}

    @Override
public boolean deleteCancelAppointment(String appointId) throws SQLException, IOException {
    Appointment apt = _appointDAO.findById(appointId);
    // 记录不存在
    if(apt == null){
        return false;
    }
    // 修改规则：允许删除【已取消、已就诊】；待就诊不允许删除
    String status = apt.getStatus();
    if(!"已取消".equals(status) && !"已就诊".equals(status)){
        return false;
    }
    return _appointDAO.deleteById(appointId);
}





}
