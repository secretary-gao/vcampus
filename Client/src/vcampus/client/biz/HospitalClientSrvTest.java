package vcampus.client.biz;
import vcampus.common.vo.Appointment;
import vcampus.common.vo.Doctor;
import vcampus.common.vo.Message;
import java.text.SimpleDateFormat;
import java.util.Date;

public class HospitalClientSrvTest {
    public static void main(String[] args) {
        String adminUserId = "admin001";
        String testUserId = "09010210";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy‑MM‑dd HH:mm");
        try {
            //====================【1】查询全部医生 ====================
            System.out.println("====================【1】查询全部医生 ====================");
            HospitalClientSrv c1 = new HospitalClientSrv();
            Message r1 = c1.queryAllDoctor();
            System.out.println("statusCode=" + r1.getStatusCode() + ", data=" + r1.getData());

            //====================【2】按科室【内科】查询医生 ====================
            System.out.println("\n====================【2】按科室【内科】查询医生 ====================");
            HospitalClientSrv c2 = new HospitalClientSrv();
            Message r2 = c2.queryDoctorByDept("内科");
            System.out.println("statusCode=" + r2.getStatusCode() + ", data=" + r2.getData());

            //====================【3】用户新增预约 ====================
            System.out.println("\n====================【3】用户新增预约 ====================");
            Appointment testAppoint = new Appointment();
            testAppoint.setUserId(testUserId);
            testAppoint.setDoctorId("D0000001");
            Date appointDate = sdf.parse("2026‑09‑02 09:30");
            testAppoint.setAppointmentTime(appointDate);
            testAppoint.setStatus("待就诊");
            HospitalClientSrv c3 = new HospitalClientSrv();
            Message r3 = c3.addAppointment(testAppoint);
            System.out.println("statusCode=" + r3.getStatusCode() + ", data=" + r3.getData());

            //====================【4】查询我的预约记录 ====================
            System.out.println("\n====================【4】查询我的预约记录 ====================");
            HospitalClientSrv c4 = new HospitalClientSrv();
            Message r4 = c4.queryMyAppointment(testUserId);
            System.out.println("statusCode=" + r4.getStatusCode() + ", data=" + r4.getData());

            //====================【5】取消预约（！！增加loginUserId参数） ====================
            System.out.println("\n====================【5】取消预约 ====================");
            String realAppointId = "AP2609035001";
            HospitalClientSrv c5 = new HospitalClientSrv();
            // 新版：第一个参数登录用户ID，第二个预约编号
            Message r5 = c5.cancelAppointment(testUserId, realAppointId);
            System.out.println("statusCode=" + r5.getStatusCode() + ", data=" + r5.getData());

            //====================【6 管理员】查询系统全部预约记录 ====================
            System.out.println("\n====================【6 管理员】查询系统全部预约记录 ====================");
            HospitalClientSrv c6 = new HospitalClientSrv();
            Message r6 = c6.queryAllAppointment(adminUserId);
            System.out.println("statusCode=" + r6.getStatusCode() + ", data=" + r6.getData());

            //====================【7 管理员】新增测试医生 ====================
            System.out.println("\n====================【7 管理员】新增测试医生 ====================");
            Doctor tempDoctor = new Doctor();
            tempDoctor.setDoctorId("D9988001");
            tempDoctor.setName("测试管理员医生");
            tempDoctor.setDepartment("骨科");
            tempDoctor.setTitle("副主任医师");
            HospitalClientSrv c7 = new HospitalClientSrv();
            Message r7 = c7.addDoctor(adminUserId, tempDoctor);
            System.out.println("statusCode=" + r7.getStatusCode() + ", data=" + r7.getData());

            //====================【8 管理员】修改刚刚新增的医生 ====================
            System.out.println("\n====================【8 管理员】修改刚刚新增的医生 ====================");
            Doctor updateDoc = new Doctor();
            updateDoc.setDoctorId("D9988001");
            updateDoc.setName("管理员医生_已修改");
            updateDoc.setDepartment("急诊科");
            updateDoc.setTitle("主任医师");
            HospitalClientSrv c8 = new HospitalClientSrv();
            Message r8 = c8.updateDoctor(adminUserId, updateDoc);
            System.out.println("statusCode=" + r8.getStatusCode() + ", data=" + r8.getData());

            //====================【9 管理员】删除测试医生 ====================
            System.out.println("\n====================【9 管理员】删除测试医生 ====================");
            HospitalClientSrv c9 = new HospitalClientSrv();
            Message r9 = c9.deleteDoctor(adminUserId, "D9988001");
            System.out.println("statusCode=" + r9.getStatusCode() + ", data=" + r9.getData());

            System.out.println("\n>>>>>>>>>> 全部9个接口测试执行完毕 <<<<<<<<<<");
        } catch (Exception e) {
            System.err.println("【测试异常】客户端医院模块通信测试失败");
            e.printStackTrace();
        }
    }
}
