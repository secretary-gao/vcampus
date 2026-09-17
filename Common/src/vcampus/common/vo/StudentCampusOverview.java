package vcampus.common.vo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** 学籍页展示的学生跨模块只读摘要。 */
public class StudentCampusOverview implements Serializable {
    private static final long serialVersionUID = 1L;

    private String _studentId;
    private String _userId;
    private String _accountStatus;
    private int _selectedCourseCount;
    private int _activeBorrowCount;
    private int _overdueBorrowCount;
    private BigDecimal _walletBalance = BigDecimal.ZERO;
    private int _purchaseCount;
    private int _pendingAppointmentCount;
    private List<String> _warnings = new ArrayList<>();

    public String getStudentId() { return _studentId; }
    public void setStudentId(String value) { _studentId = value; }
    public String getUserId() { return _userId; }
    public void setUserId(String value) { _userId = value; }
    public String getAccountStatus() { return _accountStatus; }
    public void setAccountStatus(String value) { _accountStatus = value; }
    public int getSelectedCourseCount() { return _selectedCourseCount; }
    public void setSelectedCourseCount(int value) { _selectedCourseCount = value; }
    public int getActiveBorrowCount() { return _activeBorrowCount; }
    public void setActiveBorrowCount(int value) { _activeBorrowCount = value; }
    public int getOverdueBorrowCount() { return _overdueBorrowCount; }
    public void setOverdueBorrowCount(int value) { _overdueBorrowCount = value; }
    public BigDecimal getWalletBalance() { return _walletBalance; }
    public void setWalletBalance(BigDecimal value) {
        _walletBalance = value == null ? BigDecimal.ZERO : value;
    }
    public int getPurchaseCount() { return _purchaseCount; }
    public void setPurchaseCount(int value) { _purchaseCount = value; }
    public int getPendingAppointmentCount() { return _pendingAppointmentCount; }
    public void setPendingAppointmentCount(int value) { _pendingAppointmentCount = value; }
    public List<String> getWarnings() { return _warnings; }
    public void setWarnings(List<String> value) {
        _warnings = value == null ? new ArrayList<>() : new ArrayList<>(value);
    }
}
