package vcampus.server.dao;
import vcampus.common.vo.Medicine;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MedicineDAO {
    public List<Medicine> selectAll() throws SQLException, IOException {
        List<Medicine> list = new ArrayList<>();
        String sql = "SELECT medicineId,medicineName,price,stock FROM tblMedicine";
        try(Connection conn = DbHelper.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            ResultSet rs = pstmt.executeQuery()){
            while(rs.next()){
                Medicine m = new Medicine();
                m.setMedicineId(rs.getString("medicineId"));
                m.setMedicineName(rs.getString("medicineName"));
                m.setPrice(rs.getBigDecimal("price"));
                m.setStock(rs.getInt("stock"));
                list.add(m);
            }
        }
        return list;
    }

    /** 查询药品库存 */
    public int getStockById(String medId) throws SQLException, IOException{
        String sql = "SELECT stock FROM tblMedicine WHERE medicineId=?";
        try(Connection conn = DbHelper.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)){
            pstmt.setString(1,medId);
            try(ResultSet rs = pstmt.executeQuery()){
                if(rs.next()){
                    return rs.getInt("stock");
                }
            }
        }
        return 0;
    }

    /** 扣减库存 */
    public boolean reduceStock(String medId,int num) throws SQLException, IOException{
        String sql = "UPDATE tblMedicine SET stock=stock-? WHERE medicineId=? AND stock >= ?";
        try(Connection conn = DbHelper.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)){
            pstmt.setInt(1,num);
            pstmt.setString(2,medId);
            pstmt.setInt(3,num);
            return pstmt.executeUpdate()>0;
        }
    }

    /** 更新药品库存 */
public boolean updateStock(String medicineId,int newStock) throws SQLException, IOException{
    String sql="UPDATE tblMedicine SET stock=? WHERE medicineId=?";
    try(Connection conn=DbHelper.getConnection();
        PreparedStatement pstmt=conn.prepareStatement(sql)){
        pstmt.setInt(1,newStock);
        pstmt.setString(2,medicineId);
        return pstmt.executeUpdate()>0;
    }
}

public boolean addMedicine(Medicine med) throws SQLException, IOException{
    String sql="INSERT INTO tblMedicine(medicineId,medicineName,department,price,stock) VALUES (?,?,?,?,?)";
    try(Connection conn=DbHelper.getConnection();
        PreparedStatement pstmt=conn.prepareStatement(sql)){
        pstmt.setString(1,med.getMedicineId());
        pstmt.setString(2,med.getMedicineName());
        pstmt.setString(3,med.getDepartment());
        pstmt.setBigDecimal(4, med.getPrice());
        pstmt.setInt(5,med.getStock());
        return pstmt.executeUpdate()>0;
    }
}

public boolean updateMedicine(Medicine med) throws SQLException, IOException{
    String sql="UPDATE tblMedicine SET medicineName=?,department=?,price=?,stock=? WHERE medicineId=?";
    try(Connection conn=DbHelper.getConnection();
        PreparedStatement pstmt=conn.prepareStatement(sql)){
        pstmt.setString(1,med.getMedicineName());
        pstmt.setString(2,med.getDepartment());
        pstmt.setBigDecimal(3, med.getPrice());
        pstmt.setInt(4,med.getStock());
        pstmt.setString(5,med.getMedicineId());
        return pstmt.executeUpdate()>0;
    }
}

/**删除药品*/
public boolean deleteMedicine(String medId) throws SQLException, IOException{
    String sql="DELETE FROM tblMedicine WHERE medicineId=?";
    try(Connection conn=DbHelper.getConnection();
        PreparedStatement pstmt=conn.prepareStatement(sql)){
        pstmt.setString(1,medId);
        return pstmt.executeUpdate()>0;
    }
}

}
