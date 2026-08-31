package vcampus.server.srv;

import vcampus.common.vo.Appointment;
import vcampus.common.vo.Doctor;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public interface IHospitalServerSrv {

    List<Doctor> queryAllDoctor() throws SQLException, IOException;

    List<Doctor> queryDoctorByDept(String department) throws SQLException, IOException;

    boolean addAppointment(Appointment appoint) throws SQLException, IOException;

    List<Appointment> queryMyAppointment(String userId) throws SQLException, IOException;

    boolean cancelAppointment(String appointId) throws SQLException, IOException;

}
