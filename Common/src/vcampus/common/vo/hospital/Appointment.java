package vcampus.common.vo.hospital;

import java.io.Serializable;
import java.util.Date;

public class Appointment implements Serializable {
    private static final long serialVersionUID = 1L;

    private String appointmentId;
    private String userId;
    private String doctorId;
    private Date appointmentTime;
    private String status;

    public Appointment() {
    }

    public Appointment(String appointmentId, String userId, String doctorId, Date appointmentTime, String status) {
        this.appointmentId = appointmentId;
        this.userId = userId;
        this.doctorId = doctorId;
        this.appointmentTime = appointmentTime;
        this.status = status;
    }

    public String getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(String appointmentId) {
        this.appointmentId = appointmentId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(String doctorId) {
        this.doctorId = doctorId;
    }

    public Date getAppointmentTime() {
        return appointmentTime;
    }

    public void setAppointmentTime(Date appointmentTime) {
        this.appointmentTime = appointmentTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Appointment{" +
                "appointmentId='" + appointmentId + '\'' +
                ", userId='" + userId + '\'' +
                ", doctorId='" + doctorId + '\'' +
                ", appointmentTime=" + appointmentTime +
                ", status='" + status + '\'' +
                '}';
    }
}
