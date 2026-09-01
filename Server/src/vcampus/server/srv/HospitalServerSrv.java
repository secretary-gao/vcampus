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
        // 【新增业务校验】同一个用户同一时间不能重复预约
        boolean repeat = _appointDAO.existSameTimeAppointment(appoint.getUserId(), appoint.getAppointmentTime());
        if(repeat){
            // 重复预约，直接返回false，不执行插入
            return false;
        }
        return _appointDAO.insert(appoint);
    }

    @Override
    public List<Appointment> queryMyAppointment(String userId) throws SQLException, IOException {
        return _appointDAO.selectByUserId(userId);
    }

    @Override
    public List<Appointment> queryAllAppointment() throws SQLException, IOException {
        return _appointDAO.selectAll();
    }

    @Override
    public boolean cancelAppointment(String appointId) throws SQLException, IOException {
        // 1. 查询这条预约记录（注意方法名 findById）
        Appointment apt = _appointDAO.findById(appointId);
        if(apt == null){
            // 单号不存在
            return false;
        }
        // 2. 判断：只有待就诊才允许取消
        if(!"待就诊".equals(apt.getStatus())){
            return false;
        }
        // 3. 更新状态为已取消，不是删除！
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
}
