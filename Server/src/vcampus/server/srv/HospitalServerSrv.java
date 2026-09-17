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
import vcampus.common.vo.HealthArticle;
import vcampus.common.vo.Medicine;
import vcampus.common.vo.Prescription;
import vcampus.server.dao.AppointmentDAO;
import vcampus.server.dao.DbHelper;
import vcampus.server.dao.DoctorDAO;
import vcampus.server.dao.HealthArticleDAO;
import vcampus.server.dao.MedicineDAO;
import vcampus.server.dao.PrescriptionDAO;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Date;
import java.util.HashMap;

/**
 * {@link IHospitalServerSrv} 的实现类，承载医院模块业务逻辑，
 * 数据库读写委托给 DoctorDAO 和 AppointmentDAO。
 */
public class HospitalServerSrv implements IHospitalServerSrv {
    private final DoctorDAO _doctorDAO = new DoctorDAO();
    private final AppointmentDAO _appointDAO = new AppointmentDAO();
    private final MedicineDAO medicineDAO = new MedicineDAO();
    private final PrescriptionDAO prescriptionDAO = new PrescriptionDAO();
//模拟校园卡余额，key=userId，value=余额，内存存储，重启丢失，模仿StoreFrame
//静态！全局共享内存余额，所有连接共用这一张Map，模仿StoreFrame商店写法
private static Map<String,Double> memUserBalanceMap=new HashMap<>();




    @Override
public List<Medicine> queryAllMedicine() throws SQLException, IOException {
    return medicineDAO.selectAll();
}

@Override
public List<Prescription> queryPrescriptionByAppointId(String appointId) throws SQLException, IOException {
    return prescriptionDAO.selectByAppointId(appointId);
}
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
    //②校验：同一个用户，同一时间不能重复预约（原有逻辑保留）
    boolean userRepeat = _appointDAO.existSameTimeAppointment(appoint.getUserId(), appoint.getAppointmentTime());
    if(userRepeat){
        throw new IOException("该时段您已经有预约");
    }
    //=====【新增】③校验：该医生该时间已经存在待就诊预约（已取消不算占用）=====
    boolean doctorOccupied = _appointDAO.existDoctorTimeOccupied(appoint.getDoctorId(), appointTime);
    if(doctorOccupied){
        throw new IOException("该医生此时间段已经存在待就诊预约，无法预约");
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

@Override
public List<java.sql.Timestamp> getDoctorOccupiedTime(String doctorId) throws SQLException, IOException {
    return _appointDAO.getOccupiedAppointTimeByDoctor(doctorId);
}

private final HealthArticleDAO healthArticleDAO = new HealthArticleDAO();

@Override
public List<HealthArticle> queryAllHealthArticle() throws SQLException,IOException {
    return healthArticleDAO.findAll();
}

@Override
public List<Appointment> queryDoctorPendingAppoint(String doctorId) throws SQLException, IOException {
    // 查询前自动刷新过期预约
    _appointDAO.autoUpdateExpiredAppointment();
    return _appointDAO.selectDoctorPendingAppoint(doctorId);
}

@Override
public boolean finishAppointment(String appointId, String doctorId) throws SQLException, IOException {
    return _appointDAO.finishAppointment(appointId,doctorId);
}

@Override
public List<Prescription> queryUserNoTakePres(String userId) throws SQLException, IOException {
    return prescriptionDAO.selectUserNoTakePres(userId);
}

@Override
public boolean takeMedicine(String presId, String userId) throws SQLException, IOException {
    return prescriptionDAO.takeMedicine(presId,userId);
}

//---------- 修改保存处方，增加库存校验+事务 ----------
@Override
public int savePrescriptionBatch(List<Prescription> presList) throws SQLException, IOException {
    if(presList==null || presList.isEmpty()) return 0;
    //开启事务
    try (Connection conn = DbHelper.getConnection()){
        conn.setAutoCommit(false);
        int okCount =0;
        for(Prescription pres : presList){
            //校验库存是否足够
            int stock = medicineDAO.getStockById(pres.getMedicineId());
            if(stock < pres.getMedicineNum()){
                conn.rollback();
                throw new IOException("药品【"+pres.getMedicineName()+"】库存不足！剩余库存："+stock);
            }
            //扣减库存
            boolean reduceOk = medicineDAO.reduceStock(pres.getMedicineId(),pres.getMedicineNum());
            if(!reduceOk){
                conn.rollback();
                throw new IOException("药品【"+pres.getMedicineName()+"】扣减库存失败");
            }
        }
        //全部库存校验通过，插入处方
        okCount = prescriptionDAO.batchInsert(presList);
        conn.commit();
        return okCount;
    }catch (Exception e){
        throw new IOException(e.getMessage());
    }
}
/** 获取内存模拟余额，没有用户默认初始0.0 */
public double getMemBalance(String userId){
    return memUserBalanceMap.getOrDefault(userId, 0.0);
}

/** 内存充值，增加余额 */
public boolean memRecharge(String userId, double money) {
    if(money <= 0){
        return false;
    }
    // 用户不存在则初始余额0
    if(!memUserBalanceMap.containsKey(userId)){
        memUserBalanceMap.put(userId, 0.0);
    }
    double oldBal = memUserBalanceMap.get(userId);
    memUserBalanceMap.put(userId, oldBal + money);
    return true;
}


/**
 * 内存模拟支付处方
 * 校验内存余额足够，扣内存余额，更新处方isTake=1（只改处方表，不改user表）
 */
public boolean memPayPrescription(String presId,String userId,double totalMoney) throws SQLException, IOException {
    double bal=memUserBalanceMap.getOrDefault(userId,100.0);
    if(bal < totalMoney){
        return false; //余额不足
    }
    //扣内存余额
    memUserBalanceMap.put(userId,bal-totalMoney);
    //只更新处方表：设置isTake=1 已支付已取药
    return prescriptionDAO.payPrescription(presId,userId);
}
@Override
public List<Medicine> adminQueryAllMedicine() throws SQLException, IOException {
    return medicineDAO.selectAll();
}

@Override
public boolean adminUpdateMedicineStock(String medId, int newStock) throws SQLException, IOException {
    if(newStock <0){
        return false; //库存不能负数
    }
    return medicineDAO.updateStock(medId,newStock);
}
@Override
public boolean adminAddMedicine(Medicine med) throws SQLException, IOException {
    return medicineDAO.addMedicine(med);
}
@Override
public boolean adminUpdateMedicine(Medicine med) throws SQLException, IOException {
    return medicineDAO.updateMedicine(med);
}
@Override
public boolean adminDeleteMedicine(String medId) throws SQLException, IOException {
    return medicineDAO.deleteMedicine(medId);
}

}
