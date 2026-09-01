package vcampus.client.biz;

import vcampus.common.vo.Appointment;
import vcampus.common.vo.Doctor;
import vcampus.common.vo.Message;

import java.text.SimpleDateFormat;
import java.util.Date;

public class HospitalClientSrvTest {

    // 封装一个独立调用工具方法：单次请求，用完立刻断开
    private static Message sendRequest(IHospitalClientSrv client, Runnable task) throws Exception {
        HospitalClientSrv srv = new HospitalClientSrv();
        boolean ok = srv.connect("127.0.0.1", 8888);
        if (!ok) throw new RuntimeException("连接失败");
        srv.setLoginUserId("admin001");
        Message res = null;
        try {
            task.run();
        }finally {
            srv.close();
        }
        return res;
    }

    public static void main(String[] args) {
        String adminUserId = "admin001";
        String testUserId = "09010210";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");

        try {
            //====================【1】查询全部医生 ====================
            System.out.println("====================【1】查询全部医生 ====================");
            HospitalClientSrv c1 = new HospitalClientSrv();
            c1.connect("127.0.0.1",8888);
            c1.setLoginUserId(adminUserId);
            Message r1 = c1.queryAllDoctor();
            System.out.println("statusCode=" + r1.getStatusCode() + ", data=" + r1.getData());
            c1.close();

            //====================【2】按科室【内科】查询医生 ====================
            System.out.println("\n====================【2】按科室【内科】查询医生 ====================");
            HospitalClientSrv c2 = new HospitalClientSrv();
            c2.connect("127.0.0.1",8888);
            c2.setLoginUserId(adminUserId);
            Message r2 = c2.queryDoctorByDept("内科");
            System.out.println("statusCode=" + r2.getStatusCode() + ", data=" + r2.getData());
            c2.close();

            //====================【3】用户新增预约 ====================
            System.out.println("\n====================【3】用户新增预约 ====================");
            Appointment testAppoint = new Appointment();
            testAppoint.setUserId(testUserId);
            testAppoint.setDoctorId("D0000001");
            Date appointDate = sdf.parse("2026-09-02 09:30");
            testAppoint.setAppointmentTime(appointDate);
            testAppoint.setStatus("待就诊");

            HospitalClientSrv c3 = new HospitalClientSrv();
            c3.connect("127.0.0.1",8888);
            c3.setLoginUserId(adminUserId);
            Message r3 = c3.addAppointment(testAppoint);
            System.out.println("statusCode=" + r3.getStatusCode() + ", data=" + r3.getData());
            c3.close();

            //====================【4】查询我的预约记录 ====================
            System.out.println("\n====================【4】查询我的预约记录 ====================");
            HospitalClientSrv c4 = new HospitalClientSrv();
            c4.connect("127.0.0.1",8888);
            c4.setLoginUserId(adminUserId);
            Message r4 = c4.queryMyAppointment(testUserId);
            System.out.println("statusCode=" + r4.getStatusCode() + ", data=" + r4.getData());
            c4.close();

            //====================【5】取消预约 ====================
            System.out.println("\n====================【5】取消预约 ====================");
            String realAppointId = "AP2609012256";
            HospitalClientSrv c5 = new HospitalClientSrv();
            c5.connect("127.0.0.1",8888);
            c5.setLoginUserId(adminUserId);
            Message r5 = c5.cancelAppointment(realAppointId);
            System.out.println("statusCode=" + r5.getStatusCode() + ", data=" + r5.getData());
            c5.close();

            //====================【6 管理员】查询系统全部预约记录 ====================
            System.out.println("\n====================【6 管理员】查询系统全部预约记录 ====================");
            HospitalClientSrv c6 = new HospitalClientSrv();
            c6.connect("127.0.0.1",8888);
            c6.setLoginUserId(adminUserId);
            Message r6 = c6.queryAllAppointment();
            System.out.println("statusCode=" + r6.getStatusCode() + ", data=" + r6.getData());
            c6.close();

            //====================【7 管理员】新增测试医生 ====================
            System.out.println("\n====================【7 管理员】新增测试医生 ====================");
            Doctor tempDoctor = new Doctor();
            tempDoctor.setDoctorId("D9988001");
            tempDoctor.setName("测试管理员医生");
            tempDoctor.setDepartment("骨科");
            tempDoctor.setTitle("副主任医师");
            HospitalClientSrv c7 = new HospitalClientSrv();
            c7.connect("127.0.0.1",8888);
            c7.setLoginUserId(adminUserId);
            Message r7 = c7.addDoctor(tempDoctor);
            System.out.println("statusCode=" + r7.getStatusCode() + ", data=" + r7.getData());
            c7.close();

            //====================【8 管理员】修改刚刚新增的医生 ====================
            System.out.println("\n====================【8 管理员】修改刚刚新增的医生 ====================");
            Doctor updateDoc = new Doctor();
            updateDoc.setDoctorId("D9988001");
            updateDoc.setName("管理员医生_已修改");
            updateDoc.setDepartment("急诊科");
            updateDoc.setTitle("主任医师");
            HospitalClientSrv c8 = new HospitalClientSrv();
            c8.connect("127.0.0.1",8888);
            c8.setLoginUserId(adminUserId);
            Message r8 = c8.updateDoctor(updateDoc);
            System.out.println("statusCode=" + r8.getStatusCode() + ", data=" + r8.getData());
            c8.close();

            //====================【9 管理员】删除测试医生 ====================
            System.out.println("\n====================【9 管理员】删除测试医生 ====================");
            HospitalClientSrv c9 = new HospitalClientSrv();
            c9.connect("127.0.0.1",8888);
            c9.setLoginUserId(adminUserId);
            Message r9 = c9.deleteDoctor("D9988001");
            System.out.println("statusCode=" + r9.getStatusCode() + ", data=" + r9.getData());
            c9.close();

            System.out.println("\n>>>>>>>>>> 全部9个接口测试执行完毕 <<<<<<<<<<");

        } catch (Exception e) {
            System.err.println("【测试异常】客户端医院模块通信测试失败");
            e.printStackTrace();
        }
    }
}
