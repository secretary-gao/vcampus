package vcampus.server.srv;
import vcampus.common.vo.Appointment;
import vcampus.common.vo.Doctor;
import vcampus.common.vo.HealthArticle;
import vcampus.common.vo.Medicine;
import vcampus.common.vo.Prescription;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public interface IHospitalServerSrv {
    List<Doctor> queryAllDoctor() throws SQLException, IOException;
    List<Doctor> queryCanDeleteDoctor() throws SQLException, IOException;
    List<Doctor> queryDoctorByDept(String department) throws SQLException, IOException;
    /**
     * 新增预约：内部会校验同一用户相同时段不可重复预约
     * @param appoint 预约VO
     * @return true预约成功；false：重复预约/数据库异常
     */
    boolean addAppointment(Appointment appoint) throws SQLException, IOException;
    List<Appointment> queryMyAppointment(String userId) throws SQLException, IOException;
    // 在IHospitalServerSrv接口增加方法声明
    int autoUpdateExpiredAppointment() throws SQLException, IOException;

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

    /**
 * 删除预约：管理员可删除【已取消 / 已就诊】；待就诊记录不允许删除
 * @param appointId 预约编号
 * @return true删除成功；false：状态不允许/记录不存在
 */
boolean deleteCancelAppointment(String appointId) throws SQLException, IOException;

/**
 * 医生查看自己全部待就诊预约
 * @param doctorId 当前登录医生ID
 * @return 待就诊预约列表
 */
List<Appointment> queryDoctorPendingAppoint(String doctorId) throws SQLException,IOException;

/**
 * 完成就诊，待就诊→已就诊；做权限校验，只能操作自己医生的记录
 * @param appointId 预约id
 * @param doctorId 操作医生登录id
 * @return true成功 false权限/状态不满足
 */
boolean finishAppointment(String appointId,String doctorId) throws SQLException,IOException;

List<java.sql.Timestamp> getDoctorOccupiedTime(String doctorId) throws SQLException, IOException;
List<HealthArticle> queryAllHealthArticle() throws SQLException,IOException;
List<Medicine> queryAllMedicine() throws SQLException, IOException;
int savePrescriptionBatch(List<Prescription> presList) throws SQLException, IOException;
List<Prescription> queryPrescriptionByAppointId(String appointId) throws SQLException, IOException;
//药房取药
List<Prescription> queryUserNoTakePres(String userId) throws SQLException,IOException;
boolean takeMedicine(String presId,String userId) throws SQLException,IOException;
double getMemBalance(String userId);
boolean memRecharge(String userId,double money);
boolean memPayPrescription(String presId,String userId,double totalMoney) throws SQLException,IOException;
List<Medicine> adminQueryAllMedicine() throws SQLException,IOException;
boolean adminUpdateMedicineStock(String medId,int newStock) throws SQLException,IOException;
boolean adminAddMedicine(Medicine med) throws SQLException,IOException;
boolean adminUpdateMedicine(Medicine med) throws SQLException,IOException;
boolean adminDeleteMedicine(String medId) throws SQLException,IOException;

    // 管理员维护医生
    boolean addDoctor(Doctor doctor) throws SQLException, IOException;
    boolean updateDoctor(Doctor doctor) throws SQLException, IOException;
    boolean deleteDoctor(String doctorId) throws SQLException, IOException;
}
