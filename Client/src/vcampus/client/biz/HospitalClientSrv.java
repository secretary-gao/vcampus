package vcampus.client.biz;

import vcampus.common.constant.IConstant;
import vcampus.common.vo.Appointment;
import vcampus.common.vo.Doctor;
import vcampus.common.vo.HospitalAdminReq;
import vcampus.common.vo.Message;
import vcampus.common.vo.MessageType;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class HospitalClientSrv {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    // 登录用户ID，管理员接口使用
    private String loginUserId;

    public void setLoginUserId(String loginUserId) {
        this.loginUserId = loginUserId;
    }

    /**
     * 建立Socket连接
     */
    public boolean connect(String host, int port) {
        try {
            socket = new Socket(host, port);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 1. 查询全部医生（普通接口）
     */
    public Message queryAllDoctor() throws IOException {
        Message req = new Message();
        req.setUid(System.currentTimeMillis());
        req.setName(IConstant.MSG_HOSPITAL_QUERY_ALL_DOCTOR);
        req.setType(MessageType.DATA);
        req.setStatusCode(null);
        req.setData(null);
        req.setSender("Client");

        out.writeObject(req);
        out.flush();
        try {
            return (Message) in.readObject();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 2. 按科室查询医生（普通接口）
     */
    public Message queryDoctorByDept(String department) throws IOException {
        Message req = new Message();
        req.setUid(System.currentTimeMillis());
        req.setName(IConstant.MSG_HOSPITAL_QUERY_DOCTOR_BY_DEPT);
        req.setType(MessageType.DATA);
        req.setStatusCode(null);
        req.setData(department);
        req.setSender("Client");

        out.writeObject(req);
        out.flush();
        try {
            return (Message) in.readObject();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 3. 用户预约挂号（普通接口）
     */
    public Message addAppointment(Appointment appointment) throws IOException {
        Message req = new Message();
        req.setUid(System.currentTimeMillis());
        req.setName(IConstant.MSG_HOSPITAL_ADD_APPOINTMENT);
        req.setType(MessageType.DATA);
        req.setStatusCode(null);
        req.setData(appointment);
        req.setSender("Client");

        out.writeObject(req);
        out.flush();
        try {
            return (Message) in.readObject();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 4. 查询本人预约记录（普通接口）
     */
    public Message queryMyAppointment(String userId) throws IOException {
        Message req = new Message();
        req.setUid(System.currentTimeMillis());
        req.setName(IConstant.MSG_HOSPITAL_QUERY_MY_APPOINTMENT);
        req.setType(MessageType.DATA);
        req.setStatusCode(null);
        req.setData(userId);
        req.setSender("Client");

        out.writeObject(req);
        out.flush();
        try {
            return (Message) in.readObject();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 5. 取消预约（普通接口）
     */
    public Message cancelAppointment(String appointId) throws IOException {
        Message req = new Message();
        req.setUid(System.currentTimeMillis());
        req.setName(IConstant.MSG_HOSPITAL_CANCEL_APPOINTMENT);
        req.setType(MessageType.DATA);
        req.setStatusCode(null);
        req.setData(appointId);
        req.setSender("Client");

        out.writeObject(req);
        out.flush();
        try {
            return (Message) in.readObject();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    // ====================== 管理员接口（包装 HospitalAdminReq） ======================

    /**
     * 管理员：查询全部预约记录
     */
    public Message queryAllAppointment() throws IOException {
        HospitalAdminReq body = new HospitalAdminReq(loginUserId, null);
        Message req = new Message();
        req.setUid(System.currentTimeMillis());
        req.setName(IConstant.MSG_HOSPITAL_QUERY_ALL_APPOINTMENT);
        req.setType(MessageType.DATA);
        req.setStatusCode(null);
        req.setData(body);
        req.setSender("Client");

        out.writeObject(req);
        out.flush();
        try {
            return (Message) in.readObject();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 管理员：新增医生
     */
    public Message addDoctor(Doctor doctor) throws IOException {
        HospitalAdminReq body = new HospitalAdminReq(loginUserId, doctor);
        Message req = new Message();
        req.setUid(System.currentTimeMillis());
        req.setName(IConstant.MSG_HOSPITAL_ADD_DOCTOR);
        req.setType(MessageType.DATA);
        req.setStatusCode(null);
        req.setData(body);
        req.setSender("Client");

        out.writeObject(req);
        out.flush();
        try {
            return (Message) in.readObject();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 管理员：修改医生信息
     */
    public Message updateDoctor(Doctor doctor) throws IOException {
        HospitalAdminReq body = new HospitalAdminReq(loginUserId, doctor);
        Message req = new Message();
        req.setUid(System.currentTimeMillis());
        req.setName(IConstant.MSG_HOSPITAL_UPDATE_DOCTOR);
        req.setType(MessageType.DATA);
        req.setStatusCode(null);
        req.setData(body);
        req.setSender("Client");

        out.writeObject(req);
        out.flush();
        try {
            return (Message) in.readObject();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 管理员：删除医生
     */
    public Message deleteDoctor(String doctorId) throws IOException {
        HospitalAdminReq body = new HospitalAdminReq(loginUserId, doctorId);
        Message req = new Message();
        req.setUid(System.currentTimeMillis());
        req.setName(IConstant.MSG_HOSPITAL_DELETE_DOCTOR);
        req.setType(MessageType.DATA);
        req.setStatusCode(null);
        req.setData(body);
        req.setSender("Client");

        out.writeObject(req);
        out.flush();
        try {
            return (Message) in.readObject();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 关闭Socket资源
     */
    public void close() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
