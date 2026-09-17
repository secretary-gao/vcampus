package vcampus.client.biz;
import vcampus.common.constant.IConstant;
import vcampus.common.vo.Appointment;
import vcampus.common.vo.Doctor;
import vcampus.common.vo.HospitalAdminReq;
import vcampus.common.vo.Medicine;
import vcampus.common.vo.Message;
import vcampus.common.vo.MessageType;
import vcampus.common.vo.Prescription;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;

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

/**
 * 医生获取自己待就诊预约
 */
public Message queryDoctorPendingAppoint(String doctorId) throws IOException, ClassNotFoundException {
    Socket socket = null;
    ObjectOutputStream out = null;
    ObjectInputStream in = null;
    try {
        socket = new Socket(IConstant.SERVER_HOST, IConstant.SERVER_PORT);
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
        Message req = new Message();
        req.setUid(System.currentTimeMillis());
        req.setName(IConstant.MSG_HOSPITAL_DOCTOR_GET_MY_PENDING_APPOINT);
        req.setType(MessageType.DATA);
        req.setData(doctorId);
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

/**
 * 完成就诊，把待就诊修改已就诊
 */
public Message finishAppointment(String appointId,String doctorId) throws IOException, ClassNotFoundException {
    Socket socket = null;
    ObjectOutputStream out = null;
    ObjectInputStream in = null;
    try {
        socket = new Socket(IConstant.SERVER_HOST, IConstant.SERVER_PORT);
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
        Message req = new Message();
        req.setUid(System.currentTimeMillis());
        req.setName(IConstant.MSG_HOSPITAL_FINISH_APPOINT);
        req.setType(MessageType.DATA);
        Object[] arr = new Object[]{appointId,doctorId};
        req.setData(arr);
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
/** 获取全部药品 */
public Message queryAllMedicine() throws IOException, ClassNotFoundException {
    Socket socket = null;
    ObjectOutputStream out = null;
    ObjectInputStream in = null;
    try {
        socket = new Socket(IConstant.SERVER_HOST, IConstant.SERVER_PORT);
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
        Message req = new Message();
        req.setUid(System.currentTimeMillis());
        req.setName(IConstant.MSG_HOSPITAL_QUERY_ALL_MEDICINE);
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

/** 批量保存处方 */
public Message savePrescriptionBatch(List<Prescription> presList) throws IOException, ClassNotFoundException {
    Socket socket = null;
    ObjectOutputStream out = null;
    ObjectInputStream in = null;
    try {
        socket = new Socket(IConstant.SERVER_HOST, IConstant.SERVER_PORT);
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
        Message req = new Message();
        req.setUid(System.currentTimeMillis());
        req.setName(IConstant.MSG_HOSPITAL_SAVE_PRESCRIPTION);
        req.setType(MessageType.DATA);
        req.setData(presList);
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

/** 根据预约ID查询处方 */
public Message queryPrescriptionByAppointId(String appointId) throws IOException, ClassNotFoundException {
    Socket socket = null;
    ObjectOutputStream out = null;
    ObjectInputStream in = null;
    try {
        socket = new Socket(IConstant.SERVER_HOST, IConstant.SERVER_PORT);
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
        Message req = new Message();
        req.setUid(System.currentTimeMillis());
        req.setName(IConstant.MSG_HOSPITAL_QUERY_PRES_BY_APPOINT);
        req.setType(MessageType.DATA);
        req.setData(appointId);
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
/** 用户查询自己未取药处方 */
public Message queryUserNoTakePres(String userId) throws IOException, ClassNotFoundException {
    Socket socket = null;
    ObjectOutputStream out = null;
    ObjectInputStream in = null;
    try {
        socket = new Socket(IConstant.SERVER_HOST, IConstant.SERVER_PORT);
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
        Message req = new Message();
        req.setUid(System.currentTimeMillis());
        req.setName(IConstant.MSG_HOSPITAL_USER_QUERY_MY_PRES);
        req.setType(MessageType.DATA);
        req.setData(userId);
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

/** 用户确认取药 */
public Message takeMedicine(String presId,String userId) throws IOException, ClassNotFoundException {
    Socket socket = null;
    ObjectOutputStream out = null;
    ObjectInputStream in = null;
    try {
        socket = new Socket(IConstant.SERVER_HOST, IConstant.SERVER_PORT);
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
        Message req = new Message();
        req.setUid(System.currentTimeMillis());
        req.setName(IConstant.MSG_HOSPITAL_TAKE_MEDICINE);
        req.setType(MessageType.DATA);
        Object[] arr = new Object[]{presId,userId};
        req.setData(arr);
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
/** 获取内存模拟余额 */
public Message getMemBalance(String userId) throws IOException, ClassNotFoundException {
    Socket socket = null;
    ObjectOutputStream out = null;
    ObjectInputStream in = null;
    try {
        socket = new Socket(IConstant.SERVER_HOST, IConstant.SERVER_PORT);
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
        Message req = new Message();
        req.setUid(System.currentTimeMillis());
        req.setName(IConstant.MSG_HOSPITAL_GET_MEM_BALANCE);
        req.setType(MessageType.DATA);
        req.setData(userId);
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

public Message memRecharge(String userId, double money) throws IOException, ClassNotFoundException {
    Socket socket = null;
    ObjectOutputStream out = null;
    ObjectInputStream in = null;
    try {
        socket = new Socket(IConstant.SERVER_HOST, IConstant.SERVER_PORT);
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
        Message req = new Message();
        req.setUid(System.currentTimeMillis());
        req.setName(IConstant.MSG_HOSPITAL_MEM_RECHARGE);
        req.setType(MessageType.DATA);
        //这里！！用 Double.valueOf(money)包装成对象，序列化安全
        req.setData(new Object[]{userId, Double.valueOf(money)});
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


/** 内存模拟支付处方 */
public Message memPayPrescription(String presId,String userId,double totalMoney) throws IOException, ClassNotFoundException {
    Socket socket = null;
    ObjectOutputStream out = null;
    ObjectInputStream in = null;
    try {
        socket = new Socket(IConstant.SERVER_HOST, IConstant.SERVER_PORT);
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
        Message req = new Message();
        req.setUid(System.currentTimeMillis());
        req.setName(IConstant.MSG_HOSPITAL_MEM_PAY_PRES);
        req.setType(MessageType.DATA);
        Object[] arr=new Object[]{presId,userId,totalMoney};
        req.setData(arr);
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
/**管理员查询全部药品（库存管理）*/
public Message adminQueryAllMedicine() throws IOException, ClassNotFoundException {
    Socket socket = null;
    ObjectOutputStream out = null;
    ObjectInputStream in = null;
    try {
        socket = new Socket(IConstant.SERVER_HOST, IConstant.SERVER_PORT);
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
        Message req = new Message();
        req.setUid(System.currentTimeMillis());
        req.setName(IConstant.MSG_HOSPITAL_ADMIN_QUERY_ALL_MED);
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

/**管理员修改药品库存*/
public Message adminUpdateMedicineStock(String medId,int newStock) throws IOException, ClassNotFoundException {
    Socket socket = null;
    ObjectOutputStream out = null;
    ObjectInputStream in = null;
    try {
        socket = new Socket(IConstant.SERVER_HOST, IConstant.SERVER_PORT);
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
        Message req = new Message();
        req.setUid(System.currentTimeMillis());
        req.setName(IConstant.MSG_HOSPITAL_ADMIN_UPDATE_STOCK);
        req.setType(MessageType.DATA);
        Object[] arr=new Object[]{medId,newStock};
        req.setData(arr);
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
public Message adminAddMedicine(Medicine med) throws IOException, ClassNotFoundException {
    Socket socket = null;
    ObjectOutputStream out = null;
    ObjectInputStream in = null;
    try {
        socket = new Socket(IConstant.SERVER_HOST, IConstant.SERVER_PORT);
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
        Message req = new Message();
        req.setUid(System.currentTimeMillis());
        req.setName(IConstant.MSG_HOSPITAL_ADMIN_ADD_MED);
        req.setType(MessageType.DATA);
        req.setData(med);
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
public Message adminUpdateMedicine(Medicine med) throws IOException, ClassNotFoundException {
    Socket socket = null;
    ObjectOutputStream out = null;
    ObjectInputStream in = null;
    try {
        socket = new Socket(IConstant.SERVER_HOST, IConstant.SERVER_PORT);
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
        Message req = new Message();
        req.setUid(System.currentTimeMillis());
        req.setName(IConstant.MSG_HOSPITAL_ADMIN_UPDATE_MED);
        req.setType(MessageType.DATA);
        req.setData(med);
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
public Message adminDeleteMedicine(String medId) throws IOException, ClassNotFoundException {
    Socket socket = null;
    ObjectOutputStream out = null;
    ObjectInputStream in = null;
    try {
        socket = new Socket(IConstant.SERVER_HOST, IConstant.SERVER_PORT);
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
        Message req = new Message();
        req.setUid(System.currentTimeMillis());
        req.setName(IConstant.MSG_HOSPITAL_ADMIN_DELETE_MED);
        req.setType(MessageType.DATA);
        req.setData(medId);
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
