package vcampus.server.srv;
import vcampus.common.vo.Appointment;
import vcampus.common.vo.Doctor;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public interface IHospitalServerSrv {
    List<Doctor> queryAllDoctor() throws SQLException, IOException;
    List<Doctor> queryDoctorByDept(String department) throws SQLException, IOException;
    /**
     * 新增预约：内部会校验同一用户相同时段不可重复预约
     * @param appoint 预约VO
     * @return true预约成功；false：重复预约/数据库异常
     */
    boolean addAppointment(Appointment appoint) throws SQLException, IOException;
    List<Appointment> queryMyAppointment(String userId) throws SQLException, IOException;
    /**
     * 【管理员】查询全部挂号记录
     * @return
     * @throws SQLException
     * @throws IOException
     */
    List<Appointment> queryAllAppointment() throws SQLException, IOException;

    Appointment findById(String appointId) throws SQLException, IOException;

    /**
     * 取消预约：仅待就诊可以取消，更新状态为已取消，不删除数据
     * @param loginUserId 当前登录用户ID
     * @param appointId 预约编号
     * @return true取消成功；false：不存在/不是本人/状态不允许取消
     */
    boolean cancelAppointment(String loginUserId,String appointId) throws SQLException, IOException;

    // 管理员维护医生
    boolean addDoctor(Doctor doctor) throws SQLException, IOException;
    boolean updateDoctor(Doctor doctor) throws SQLException, IOException;
    boolean deleteDoctor(String doctorId) throws SQLException, IOException;
}
