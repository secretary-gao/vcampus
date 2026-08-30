package vcampus.server.srv;

import vcampus.common.vo.hospital.Appointment;
import vcampus.common.vo.hospital.Doctor;
import java.util.ArrayList;
import java.util.List;

/**
 * 服务端医院业务，对齐说明书 IHospitalServerSrv
 * 本文件：接口 + 空壳实现合并，Socket跑通后修改内部HospitalServiceImpl
 */
public interface IHospitalServerSrv {

    Boolean queryDoctor(String department);
    Boolean makeAppointment(String userId, String doctorId, String time);
    Boolean cancelAppointment(String appointmentId);
    List<Appointment> queryAppointment(String userId);
    Boolean addDoctor(Doctor doctor);
    Boolean updateDoctor(Doctor doctor);
    Boolean deleteDoctor(String doctorId);

    /**
     * 空壳实现，内部static类，TODO后续填充DAO逻辑
     */
    static class HospitalServiceImpl implements IHospitalServerSrv {

        @Override
        public Boolean queryDoctor(String department) {
            // TODO Socket就绪，接入DAO
            return false;
        }

        @Override
        public Boolean makeAppointment(String userId, String doctorId, String time) {
            // TODO Socket就绪，接入DAO
            return false;
        }

        @Override
        public Boolean cancelAppointment(String appointmentId) {
            // TODO Socket就绪，接入DAO
            return false;
        }

        @Override
        public List<Appointment> queryAppointment(String userId) {
            // TODO Socket就绪，接入DAO
            return new ArrayList<>();
        }

        @Override
        public Boolean addDoctor(Doctor doctor) {
            // TODO Socket就绪，接入DAO
            return false;
        }

        @Override
        public Boolean updateDoctor(Doctor doctor) {
            // TODO Socket就绪，接入DAO
            return false;
        }

        @Override
        public Boolean deleteDoctor(String doctorId) {
            // TODO Socket就绪，接入DAO
            return false;
        }
    }
}
