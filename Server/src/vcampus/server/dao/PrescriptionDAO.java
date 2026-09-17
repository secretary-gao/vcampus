package vcampus.server.dao;
import vcampus.common.vo.Prescription;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PrescriptionDAO {
    /**批量保存多条处方（一次就诊开多种药）*/
    public int batchInsert(List<Prescription> presList) throws SQLException, IOException {
        if(presList==null || presList.isEmpty()) return 0;
        String sql = "INSERT INTO tblPrescription(presId,appointId,doctorId,userId,medicineId,medicineName,medicineNum) " +
                "VALUES (?,?,?,?,?,?,?)";
        int count =0;
        try(Connection conn = DbHelper.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)){
            for(Prescription p : presList){
                p.setPresId(UUID.randomUUID().toString().replace("-",""));
                pstmt.setString(1,p.getPresId());
                pstmt.setString(2,p.getAppointId());
                pstmt.setString(3,p.getDoctorId());
                pstmt.setString(4,p.getUserId());
                pstmt.setString(5,p.getMedicineId());
                pstmt.setString(6,p.getMedicineName());
                pstmt.setInt(7,p.getMedicineNum());
                pstmt.addBatch();
            }
            int[] res = pstmt.executeBatch();
            for(int i : res) if(i>0) count++;
        }
        return count;
    }

    /** 根据预约id查询该预约下所有处方记录 */
    public List<Prescription> selectByAppointId(String appointId) throws SQLException, IOException {
        List<Prescription> list = new ArrayList<>();
        String sql = "SELECT presId,appointId,doctorId,userId,medicineId,medicineName,medicineNum,createTime,isTake " +
                "FROM tblPrescription WHERE appointId=? ORDER BY createTime DESC";
        try(Connection conn = DbHelper.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)){
            pstmt.setString(1,appointId);
            try(ResultSet rs = pstmt.executeQuery()){
                while(rs.next()){
                    Prescription p = new Prescription();
                    p.setPresId(rs.getString("presId"));
                    p.setAppointId(rs.getString("appointId"));
                    p.setDoctorId(rs.getString("doctorId"));
                    p.setUserId(rs.getString("userId"));
                    p.setMedicineId(rs.getString("medicineId"));
                    p.setMedicineName(rs.getString("medicineName"));
                    p.setMedicineNum(rs.getInt("medicineNum"));
                    p.setCreateTime(rs.getTimestamp("createTime"));
                    p.setIsTake(rs.getInt("isTake"));
                    list.add(p);
                }
            }
        }
        return list;
    }

        /** 用户查询自己所有【未取药】的处方，关联药品表拿price计算小计 */
    public List<Prescription> selectUserNoTakePres(String userId) throws SQLException, IOException{
        List<Prescription> list = new ArrayList<>();
        String sql = "SELECT p.presId,p.appointId,p.doctorId,p.userId,p.medicineId,p.medicineName,p.medicineNum,p.createTime,p.isTake,m.price " +
                "FROM tblPrescription p LEFT JOIN tblMedicine m ON p.medicineId=m.medicineId " +
                "WHERE p.userId=? AND p.isTake=0 ORDER BY p.createTime DESC";
        try(Connection conn = DbHelper.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)){
            pstmt.setString(1,userId);
            try(ResultSet rs = pstmt.executeQuery()){
                while(rs.next()){
                    Prescription p = new Prescription();
                    p.setPresId(rs.getString("presId"));
                    p.setAppointId(rs.getString("appointId"));
                    p.setDoctorId(rs.getString("doctorId"));
                    p.setUserId(rs.getString("userId"));
                    p.setMedicineId(rs.getString("medicineId"));
                    p.setMedicineName(rs.getString("medicineName"));
                    p.setMedicineNum(rs.getInt("medicineNum"));
                    p.setCreateTime(rs.getTimestamp("createTime"));
                    p.setIsTake(rs.getInt("isTake"));
                    double price = rs.getDouble("price");
                    int num = rs.getInt("medicineNum");
                    p.setSubTotal(price * num);
                    list.add(p);
                }
            }
        }
        return list;
    }


    /** 用户确认取药，修改isTake=1 */
    public boolean takeMedicine(String presId,String userId) throws SQLException, IOException{
        String sql = "UPDATE tblPrescription SET isTake=1 WHERE presId=? AND userId=? AND isTake=0";
        try(Connection conn = DbHelper.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)){
            pstmt.setString(1,presId);
            pstmt.setString(2,userId);
            return pstmt.executeUpdate()>0;
        }
    }

public boolean payPrescription(String presId,String userId)throws SQLException,IOException{
    String sql="UPDATE tblPrescription SET isTake=1 WHERE presId=? AND userId=? AND isTake=0";
    try(Connection conn=DbHelper.getConnection()){
        conn.setAutoCommit(false);
        try(PreparedStatement pstmt=conn.prepareStatement(sql)){
            pstmt.setString(1,presId);
            pstmt.setString(2,userId);
            int row=pstmt.executeUpdate();
            if(row<=0){
                conn.rollback();
                return false;
            }
            conn.commit();
            return true;
        }catch (Exception e){
            conn.rollback();
            throw e;
        }
    }
}

}
