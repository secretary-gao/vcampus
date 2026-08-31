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
        return _appointDAO.insert(appoint);
    }

    @Override
    public List<Appointment> queryMyAppointment(String userId) throws SQLException, IOException {
        return _appointDAO.selectByUserId(userId);
    }

    @Override
    public boolean cancelAppointment(String appointId) throws SQLException, IOException {
        return _appointDAO.deleteById(appointId);
    }
}
