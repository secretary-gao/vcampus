package vcampus.common.vo;
import java.io.Serializable;
public class Doctor implements Serializable {
    private static final long serialVersionUID = 1L;
    private String doctorId;
    private String name;
    private String department;
    private String title;
    //新增：擅长领域
    private String skill;

    public Doctor() {
    }

    public Doctor(String doctorId, String name, String department, String title,String skill) {
        this.doctorId = doctorId;
        this.name = name;
        this.department = department;
        this.title = title;
        this.skill = skill;
    }
    //兼容旧构造
    public Doctor(String doctorId, String name, String department, String title) {
        this.doctorId = doctorId;
        this.name = name;
        this.department = department;
        this.title = title;
    }

    public String getDoctorId() {
        return doctorId;
    }
    public void setDoctorId(String doctorId) {
        this.doctorId = doctorId;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getDepartment() {
        return department;
    }
    public void setDepartment(String department) {
        this.department = department;
    }
    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    //skill get set
    public String getSkill() {
        return skill;
    }
    public void setSkill(String skill) {
        this.skill = skill;
    }

    @Override
    public String toString() {
        return "Doctor{" +
                "doctorId='" + doctorId + '\'' +
                ", name='" + name + '\'' +
                ", department='" + department + '\'' +
                ", title='" + title + '\'' +
                ", skill='" + skill + '\'' +
                '}';
    }
}
