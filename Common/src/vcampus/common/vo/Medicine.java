package vcampus.common.vo;
import java.io.Serializable;
import java.math.BigDecimal;

public class Medicine implements Serializable {
    private static final long serialVersionUID = 1L;
    private String medicineId;
    private String medicineName;
    private BigDecimal price;
    //新增库存
    private Integer stock;

    public Medicine(){}
    public Medicine(String medicineId, String medicineName, BigDecimal price,Integer stock) {
        this.medicineId = medicineId;
        this.medicineName = medicineName;
        this.price = price;
        this.stock = stock;
    }

    public String getMedicineId() { return medicineId; }
    public void setMedicineId(String medicineId) { this.medicineId = medicineId; }
    public String getMedicineName() { return medicineName; }
    public void setMedicineName(String medicineName) { this.medicineName = medicineName; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
   
}
