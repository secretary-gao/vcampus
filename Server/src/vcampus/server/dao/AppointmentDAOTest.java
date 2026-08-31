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
import java.util.Date;
import java.util.List;

/**
 * {@link AppointmentDAO} 的功能验证程序：测试新增预约、根据用户ID查询预约、取消预约。
 *
 * <p>运行前请确认：已执行医院模块sql脚本；已正确
 * 配置 {@code Server/db.properties}；运行时的工作目录为项目根目录。</p>
 */
public class AppointmentDAOTest {
    public static void main(String[] args) {
        AppointmentDAO dao = new AppointmentDAO();

        // 和你SQL初始化数据严格对齐！
        String testUserId = "09010210";
        // 重点：必须是8位医生编号 D0000001，不是D001
        String testDoctorId = "D0000001";
        Date testTime = new Date();
        String testAppointId = "AP2026083101";
        Appointment testAppoint = new Appointment(testAppointId, testUserId, testDoctorId, testTime, "待就诊");

        try {
            // 测试1：新增预约
            boolean insertOk = dao.insert(testAppoint);
            System.out.println(insertOk ? "【AppointmentDAO】新增预约成功" : "【AppointmentDAO】新增预约失败");

            // 测试2：查询该用户全部预约
            List<Appointment> myAppointList = dao.selectByUserId(testUserId);
            if(myAppointList == null || myAppointList.isEmpty()){
                System.out.println("【AppointmentDAO】用户["+testUserId+"]暂无预约记录");
            }else{
                System.out.println("【AppointmentDAO】查询用户["+testUserId+"]预约列表，共"+myAppointList.size()+"条");
                for (Appointment ap : myAppointList) {
                    System.out.println("\t"+ap);
                }
            }

            // 取出第一条预约id用于取消测试
            if(myAppointList != null && !myAppointList.isEmpty()){
                String appointId = myAppointList.get(0).getAppointmentId();
                boolean cancelOk = dao.deleteById(appointId);
                System.out.println(cancelOk ? "【AppointmentDAO】取消预约id="+appointId+" 成功" : "【AppointmentDAO】取消预约失败");
            }

        } catch (SQLException | IOException e) {
            System.err.println("AppointmentDAO数据层测试失败：" + e.getMessage());
            System.err.println("排查清单：");
            System.err.println("1. tblUser 是否存在用户 09010210");
            System.err.println("2. tblDoctor 是否存在 D0000001");
            System.err.println("3. db.properties数据库配置正确，MySQL已启动");
            e.printStackTrace();
        }
    }
}
