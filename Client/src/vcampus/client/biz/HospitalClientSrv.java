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

    public Message queryAllDoctor() throws IOException, ClassNotFoundException {
        Socket socket = null;
        ObjectOutputStream out = null;
        ObjectInputStream in = null;
        try {
            socket = new Socket(IConstant.SERVER_HOST, IConstant.SERVER_PORT);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            Message req = new Message();
            req.setUid(System.currentTimeMillis());
            req.setName(IConstant.MSG_HOSPITAL_QUERY_ALL_DOCTOR);
            req.setType(MessageType.DATA);
            req.setStatusCode(null);
            req.setData(null);
            req.setSender("Client");
            out.writeObject(req);
            out.flush();
            return (Message) in.readObject();
        } finally {
            if(in!=null) in.close();
            if(out!=null) out.close();
            if(socket!=null) socket.close();
        }
    }

    public Message queryDoctorByDept(String department) throws IOException, ClassNotFoundException {
        Socket socket = null;
        ObjectOutputStream out = null;
        ObjectInputStream in = null;
        try {
            socket = new Socket(IConstant.SERVER_HOST, IConstant.SERVER_PORT);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            Message req = new Message();
            req.setUid(System.currentTimeMillis());
            req.setName(IConstant.MSG_HOSPITAL_QUERY_DOCTOR_BY_DEPT);
            req.setType(MessageType.DATA);
            req.setStatusCode(null);
            req.setData(department);
            req.setSender("Client");
            out.writeObject(req);
            out.flush();
            return (Message) in.readObject();
        } finally {
            if(in!=null) in.close();
            if(out!=null) out.close();
            if(socket!=null) socket.close();
        }
    }

    public Message addAppointment(Appointment appointment) throws IOException, ClassNotFoundException {
        Socket socket = null;
        ObjectOutputStream out = null;
        ObjectInputStream in = null;
        try {
            socket = new Socket(IConstant.SERVER_HOST, IConstant.SERVER_PORT);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            Message req = new Message();
            req.setUid(System.currentTimeMillis());
            req.setName(IConstant.MSG_HOSPITAL_ADD_APPOINTMENT);
            req.setType(MessageType.DATA);
            req.setStatusCode(null);
            req.setData(appointment);
            req.setSender("Client");
            out.writeObject(req);
            out.flush();
            return (Message) in.readObject();
        } finally {
            if(in!=null) in.close();
            if(out!=null) out.close();
            if(socket!=null) socket.close();
        }
    }

    public Message getDoctorOccupiedTime(String doctorId) throws IOException, ClassNotFoundException {
    Socket socket = null;
    ObjectOutputStream out = null;
    ObjectInputStream in = null;
    try {
        socket = new Socket(IConstant.SERVER_HOST, IConstant.SERVER_PORT);
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
        Message req = new Message();
        req.setUid(System.currentTimeMillis());
        req.setName(IConstant.MSG_HOSPITAL_GET_OCCUPIED_TIME);
        req.setType(MessageType.DATA);
        req.setStatusCode(null);
        req.setData(doctorId);
        req.setSender("Client");
        out.writeObject(req);
        out.flush();
        return (Message) in.readObject();
    } finally {
        if(in!=null) in.close();
        if(out!=null) out.close();
        if(socket!=null) socket.close();
    }
}


    public Message queryMyAppointment(String userId) throws IOException, ClassNotFoundException {
        Socket socket = null;
        ObjectOutputStream out = null;
        ObjectInputStream in = null;
        try {
            socket = new Socket(IConstant.SERVER_HOST, IConstant.SERVER_PORT);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            Message req = new Message();
            req.setUid(System.currentTimeMillis());
            req.setName(IConstant.MSG_HOSPITAL_QUERY_MY_APPOINTMENT);
            req.setType(MessageType.DATA);
            req.setStatusCode(null);
            req.setData(userId);
            req.setSender("Client");
            out.writeObject(req);
            out.flush();
            return (Message) in.readObject();
        } finally {
            if(in!=null) in.close();
            if(out!=null) out.close();
            if(socket!=null) socket.close();
        }
    }

    //修改：增加loginUserId，传递数组
    public Message cancelAppointment(String loginUserId,String appointId) throws IOException, ClassNotFoundException {
        Socket socket = null;
        ObjectOutputStream out = null;
        ObjectInputStream in = null;
        try {
            socket = new Socket(IConstant.SERVER_HOST, IConstant.SERVER_PORT);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            Object[] payload = new Object[]{loginUserId, appointId};
            Message req = new Message();
            req.setUid(System.currentTimeMillis());
            req.setName(IConstant.MSG_HOSPITAL_CANCEL_APPOINTMENT);
            req.setType(MessageType.DATA);
            req.setStatusCode(null);
            req.setData(payload);
            req.setSender("Client");
            out.writeObject(req);
            out.flush();
            return (Message) in.readObject();
        } finally {
            if(in!=null) in.close();
            if(out!=null) out.close();
            if(socket!=null) socket.close();
        }
    }

    public Message queryAllAppointment(String loginUserId) throws IOException, ClassNotFoundException {
        Socket socket = null;
        ObjectOutputStream out = null;
        ObjectInputStream in = null;
        try {
            socket = new Socket(IConstant.SERVER_HOST, IConstant.SERVER_PORT);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

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
            return (Message) in.readObject();
        } finally {
            if(in!=null) in.close();
            if(out!=null) out.close();
            if(socket!=null) socket.close();
        }
    }

    public Message addDoctor(String loginUserId, Doctor doctor) throws IOException, ClassNotFoundException {
        Socket socket = null;
        ObjectOutputStream out = null;
        ObjectInputStream in = null;
        try {
            socket = new Socket(IConstant.SERVER_HOST, IConstant.SERVER_PORT);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

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
            return (Message) in.readObject();
        } finally {
            if(in!=null) in.close();
            if(out!=null) out.close();
            if(socket!=null) socket.close();
        }
    }

    public Message updateDoctor(String loginUserId, Doctor doctor) throws IOException, ClassNotFoundException {
        Socket socket = null;
        ObjectOutputStream out = null;
        ObjectInputStream in = null;
        try {
            socket = new Socket(IConstant.SERVER_HOST, IConstant.SERVER_PORT);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

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
            return (Message) in.readObject();
        } finally {
            if(in!=null) in.close();
            if(out!=null) out.close();
            if(socket!=null) socket.close();
        }
    }

    public Message deleteDoctor(String loginUserId, String doctorId) throws IOException, ClassNotFoundException {
        Socket socket = null;
        ObjectOutputStream out = null;
        ObjectInputStream in = null;
        try {
            socket = new Socket(IConstant.SERVER_HOST, IConstant.SERVER_PORT);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

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
            return (Message) in.readObject();
        } finally {
            if(in!=null) in.close();
            if(out!=null) out.close();
            if(socket!=null) socket.close();
        }
    }

    public Message queryCanDeleteDoctor() throws IOException, ClassNotFoundException {
    Socket socket = null;
    ObjectOutputStream out = null;
    ObjectInputStream in = null;
    try {
        socket = new Socket(IConstant.SERVER_HOST, IConstant.SERVER_PORT);
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
        Message req = new Message();
        req.setUid(System.currentTimeMillis());
        req.setName(IConstant.MSG_HOSPITAL_QUERY_CAN_DELETE_DOCTOR);
        req.setType(MessageType.DATA);
        req.setStatusCode(null);
        req.setData(null);
        req.setSender("Client");
        out.writeObject(req);
        out.flush();
        return (Message) in.readObject();
    } finally {
        if(in!=null) in.close();
        if(out!=null) out.close();
        if(socket!=null) socket.close();
    }
}

    public Message deleteCancelAppointment(String appointId) throws IOException, ClassNotFoundException {
        Socket socket = null;
        ObjectOutputStream out = null;
        ObjectInputStream in = null;
        try {
            socket = new Socket(IConstant.SERVER_HOST, IConstant.SERVER_PORT);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            Message req = new Message();
            req.setUid(System.currentTimeMillis());
            req.setName(IConstant.MSG_HOSPITAL_DELETE_CANCEL_APPOINT);
            req.setType(MessageType.DATA);
            req.setStatusCode(null);
            req.setData(appointId);
            req.setSender("Client");
            out.writeObject(req);
            out.flush();
            return (Message) in.readObject();
        } finally {
            if(in!=null) in.close();
            if(out!=null) out.close();
            if(socket!=null) socket.close();
        }
    }

/** 查询全部健康教育文章 */
public Message queryAllHealthArticle() throws IOException, ClassNotFoundException {
    Socket socket = null;
    ObjectOutputStream out = null;
    ObjectInputStream in = null;
    try {
        socket = new Socket(IConstant.SERVER_HOST, IConstant.SERVER_PORT);
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
        Message req = new Message();
        req.setUid(System.currentTimeMillis());
        req.setName(IConstant.MSG_HOSPITAL_QUERY_HEALTH_ARTICLE);
        req.setType(MessageType.DATA);
        req.setSender("Client");
        out.writeObject(req);
        out.flush();
        return (Message) in.readObject();
    } finally {
        if (in != null) in.close();
        if (out != null) out.close();
        if (socket != null) socket.close();
    }
}
}
