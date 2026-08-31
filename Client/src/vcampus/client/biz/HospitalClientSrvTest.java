/*
 * HospitalClientSrvTest
 *
 * Version 1.0
 *
 * 2026-08-31
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.client.biz;

import vcampus.common.vo.Appointment;
import vcampus.common.vo.Message;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * {@link HospitalClientSrv} 的端到端验证程序：不涉及界面，直接通过 Socket
 * 向服务器发起医院模块各类请求，打印服务器返回的响应，用于
 * 确认"客户端 → Socket → 服务器 → 数据库"整条链路是否打通。
 *
 * <p>运行前请先启动 {@code vcampus.server.srv.Server}。</p>
 */
public class HospitalClientSrvTest {
    /**
     * 程序入口：依次测试医院全部5个业务请求。
     *
     * @param args 命令行参数（未使用）
     */
    public static void main(String[] args) {
        IHospitalClientSrv hospitalClientSrv = new HospitalClientSrv();
        String testUserId = "09010210";

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            Appointment testAppoint = new Appointment();
            testAppoint.setUserId(testUserId);
            testAppoint.setDoctorId("D0000001");
            Date appointDate = sdf.parse("2026-09-02 09:30");
            testAppoint.setAppointmentTime(appointDate);
            testAppoint.setStatus("待就诊"); // ❗必须设置状态，数据库不允许null
            // 不需要 setAppointmentId，DAO自动生成12位编号


            System.out.println("=== 测试1：查询全部医生 ===");
            Message respAllDoc = hospitalClientSrv.queryAllDoctor();
            System.out.println("查询全部医生响应：statusCode=" + respAllDoc.getStatusCode()
                    + ", data=" + respAllDoc.getData());

            System.out.println("\n=== 测试2：根据科室查询医生 ===");
            Message respByDept = hospitalClientSrv.queryDoctorByDept("内科");
            System.out.println("按科室查询医生响应：statusCode=" + respByDept.getStatusCode()
                    + ", data=" + respByDept.getData());

            System.out.println("\n=== 测试3：新增预约 ===");
            Message respAdd = hospitalClientSrv.addAppointment(testAppoint);
            System.out.println("新增预约响应：statusCode=" + respAdd.getStatusCode()
                    + ", data=" + respAdd.getData());

            System.out.println("\n=== 测试4：查询我的预约记录 ===");
            Message respMyAppoint = hospitalClientSrv.queryMyAppointment(testUserId);
            System.out.println("查询我的预约响应：statusCode=" + respMyAppoint.getStatusCode()
                    + ", data=" + respMyAppoint.getData());

            System.out.println("\n=== 测试5：取消预约（初次运行注释，拿到真实appointmentId再打开）===");
            
            String realAppointId = "AP2608311952";
            Message respCancel = hospitalClientSrv.cancelAppointment(realAppointId);
            System.out.println("取消预约响应：statusCode=" + respCancel.getStatusCode()
                    + ", data=" + respCancel.getData());
            

        } catch (Exception e) {
            System.err.println("客户端医院模块通信测试失败：" + e.getMessage());
            System.err.println("请确认服务器 vcampus.server.srv.Server 是否已启动。");
            e.printStackTrace();
        }
    }
}
