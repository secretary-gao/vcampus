package vcampus.common.vo;
import java.io.Serializable;
import java.util.Date;

public class Prescription implements Serializable {
    private static final long serialVersionUID = 1L;
    private String presId;
    private String appointId;
    private String doctorId;
    private String userId;
    private String medicineId;
    private String medicineName;
    private Integer medicineNum;
    private Date createTime;
    //新增 是否取药 0未取，1已取
    private Integer isTake;
    //新增 药品小计金额（price * medicineNum）
    private Double subTotal;

    public Prescription(){}

    public String getPresId() { return presId; }
    public void setPresId(String presId) { this.presId = presId; }
    public String getAppointId() { return appointId; }
    public void setAppointId(String appointId) { this.appointId = appointId; }
    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getMedicineId() { return medicineId; }
    public void setMedicineId(String medicineId) { this.medicineId = medicineId; }
    public String getMedicineName() { return medicineName; }
    public void setMedicineName(String medicineName) { this.medicineName = medicineName; }
    public Integer getMedicineNum() { return medicineNum; }
    public void setMedicineNum(Integer medicineNum) { this.medicineNum = medicineNum; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public Integer getIsTake() { return isTake; }
    public void setIsTake(Integer isTake) { this.isTake = isTake; }
    public Double getSubTotal() { return subTotal; }
    public void setSubTotal(Double subTotal) { this.subTotal = subTotal; }


}
