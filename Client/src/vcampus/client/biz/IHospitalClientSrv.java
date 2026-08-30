package vcampus.client.biz;

import vcampus.common.vo.hospital.Appointment;
import vcampus.common.vo.hospital.Doctor;
import java.util.ArrayList;
import java.util.List;

/**
 * 客户端医院业务接口，对齐说明书 IHospitalClientSrv
 * 接口 + 空壳实现合并，Socket跑通后修改内部HospitalClientSrvImpl
 */
public interface IHospitalClientSrv {

    Boolean queryDoctor(String department);
    Boolean makeAppointment(String userId, String doctorId, String time);
    Boolean cancelAppointment(String appointmentId);
    List<Appointment> queryAppointment(String userId);
    Boolean addDoctor(Doctor doctor);
    Boolean updateDoctor(Doctor doctor);
    Boolean deleteDoctor(String doctorId);

    static class HospitalClientSrvImpl implements IHospitalClientSrv {

        @Override
        public Boolean queryDoctor(String department) {
            // TODO Socket就绪，组装发送网络消息
            return false;
        }

        @Override
        public Boolean makeAppointment(String userId, String doctorId, String time) {
            // TODO Socket就绪，组装发送网络消息
            return false;
        }

        @Override
        public Boolean cancelAppointment(String appointmentId) {
            // TODO Socket就绪，组装发送网络消息
            return false;
        }

        @Override
        public List<Appointment> queryAppointment(String userId) {
            // TODO Socket就绪，组装发送网络消息
            return new ArrayList<>();
        }

        @Override
        public Boolean addDoctor(Doctor doctor) {
            // TODO Socket就绪，组装发送网络消息
            return false;
        }

        @Override
        public Boolean updateDoctor(Doctor doctor) {
            // TODO Socket就绪，组装发送网络消息
            return false;
        }

        @Override
        public Boolean deleteDoctor(String doctorId) {
            // TODO Socket就绪，组装发送网络消息
            return false;
        }
    }
}
