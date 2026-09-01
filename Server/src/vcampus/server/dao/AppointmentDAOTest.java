/*
 * AppointmentDAOTest
 *
 * Version 1.0
 *
 * 2026-08-31
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.server.dao;
import vcampus.common.vo.Appointment;
import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * {@link AppointmentDAO} 的功能验证程序
 * 测试：新增预约、用户预约查询、全部预约查询(管理员)、findById、状态更新取消预约、重复预约拦截
 */
public class AppointmentDAOTest {
    public static void main(String[] args) throws ParseException {
        AppointmentDAO dao = new AppointmentDAO();
        String testUserId = "09010210";
        String testDoctorId = "D0000001";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date testTime = sdf.parse("2026-09-02 09:30:00");

        try {
            //=====前置自动清理：每次运行清除历史脏数据=====
            System.out.println("===== 前置清理：清空本次测试旧预约数据 =====");
            dao.cleanTestAppointment(testUserId, testTime);
            System.out.println("✅自动清理完成");

            System.out.println("\n========== 测试1：新增预约 ==========");
            Appointment testAppoint = new Appointment(null, testUserId, testDoctorId, testTime, "待就诊");
            boolean insertOk = dao.insert(testAppoint);
            System.out.println(insertOk ? "✅【AppointmentDAO】新增预约成功，生成编号："+testAppoint.getAppointmentId() : "❌【AppointmentDAO】新增预约失败");
            String targetAppointId = testAppoint.getAppointmentId();

            System.out.println("\n========== 测试2：根据ID单条查询findById ==========");
            Appointment aptById = dao.findById(targetAppointId);
            if(aptById != null){
                System.out.println("✅查询单条预约成功："+aptById);
            }else{
                System.out.println("❌查询单条预约失败");
            }

            System.out.println("\n========== 测试3：查询该用户全部预约 selectByUserId ==========");
            List<Appointment> myAppointList = dao.selectByUserId(testUserId);
            if(myAppointList == null || myAppointList.isEmpty()){
                System.out.println("⚠️【AppointmentDAO】用户["+testUserId+"]暂无预约记录");
            }else{
                System.out.println("✅【AppointmentDAO】查询用户["+testUserId+"]预约列表，共"+myAppointList.size()+"条");
                for (Appointment ap : myAppointList) {
                    System.out.println("\t"+ap);
                }
            }

            System.out.println("\n========== 测试4：管理员查询全部预约 selectAll ==========");
            List<Appointment> allAppointList = dao.selectAll();
            System.out.println("✅管理员查询全部预约，总条数："+allAppointList.size());

            System.out.println("\n========== 测试5：取消预约（修改状态为【已取消】，不是物理删除） ==========");
            boolean updateOk = dao.updateStatus(targetAppointId,"已取消");
            System.out.println(updateOk ? "✅状态更新成功 →已取消" : "❌状态更新失败");
            Appointment afterCancel = dao.findById(targetAppointId);
            System.out.println("取消后记录状态："+afterCancel.getStatus());

            System.out.println("\n========== 测试6：重复预约校验 existSameTimeAppointment ==========");
            //此时数据库只有一条【已取消】记录，应当允许再次预约同一时间
            boolean repeatCheck1 = dao.existSameTimeAppointment(testUserId, testTime);
            if(repeatCheck1){
                System.out.println("❌异常：已取消记录不应该拦截新预约");
            }else{
                System.out.println("✅校验：已取消记录，同一时间可以再次预约");
            }

            //插入一条待就诊同时间预约，触发拦截
            Appointment testRepeatApt = new Appointment(null,testUserId,testDoctorId,testTime,"待就诊");
            dao.insert(testRepeatApt);
            boolean repeatCheck2 = dao.existSameTimeAppointment(testUserId, testTime);
            if(repeatCheck2){
                System.out.println("✅校验生效：检测到【待就诊】重复时间预约，拦截");
            }else{
                System.out.println("❌异常：没有拦截重复预约");
            }

            Date newTime = sdf.parse("2026-09-03 10:00:00");
            boolean repeatCheck3 = dao.existSameTimeAppointment(testUserId, newTime);
            if(!repeatCheck3){
                System.out.println("✅新时间校验通过，可以挂号");
            }else{
                System.out.println("❌异常：新时间判定重复");
            }

        } catch (SQLException | IOException | ParseException e) {
            System.err.println("\nAppointmentDAO数据层测试失败：" + e.getMessage());
            System.err.println("排查清单：");
            System.err.println("1. tblUser 是否存在用户 09010210");
            System.err.println("2. tblDoctor 是否存在 D0000001");
            System.err.println("3. db.properties数据库配置正确，MySQL已启动");
            e.printStackTrace();
        }
    }
}
