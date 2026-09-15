package vcampus.server.dao;

import vcampus.common.vo.Student;
import vcampus.common.vo.StudentCampusOverview;
import vcampus.common.vo.StudentStatus;
import vcampus.common.vo.User;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** 汇总学生在选课、图书馆、商店、医院和账号模块中的只读状态。 */
public class StudentCampusOverviewDAO {

    public StudentCampusOverview load(Student student) throws SQLException, IOException {
        StudentCampusOverview overview = new StudentCampusOverview();
        overview.setStudentId(student.getStudentId());
        overview.setUserId(student.getUserId());
        List<String> warnings = new ArrayList<>();
        try (Connection connection = DbHelper.getConnection()) {
            overview.setAccountStatus(text(connection,
                    "SELECT uStatus FROM tblUser WHERE uId=?", student.getUserId()));
            overview.setSelectedCourseCount(safeCount(connection,
                    "SELECT COUNT(*) FROM tblSelectCourse WHERE studentId=?", student.getStudentId(),
                    warnings, "选课模块尚未初始化"));
            overview.setActiveBorrowCount(safeCount(connection,
                    "SELECT COUNT(*) FROM tblBorrow WHERE userId=? AND status='借阅中'", student.getUserId(),
                    warnings, "图书馆模块尚未初始化"));
            overview.setOverdueBorrowCount(safeCount(connection,
                    "SELECT COUNT(*) FROM tblBorrow WHERE userId=? AND status='借阅中' AND dueDate<CURRENT_DATE",
                    student.getUserId(), warnings, "图书馆模块尚未初始化"));
            overview.setWalletBalance(safeDecimal(connection,
                    "SELECT balance FROM tblWallet WHERE userId=?", student.getUserId(),
                    warnings, "校园卡模块尚未初始化"));
            overview.setPurchaseCount(safeCount(connection,
                    "SELECT COUNT(*) FROM tblPurchase WHERE userId=?", student.getUserId(),
                    warnings, "商店模块尚未初始化"));
            overview.setPendingAppointmentCount(safeCount(connection,
                    "SELECT COUNT(*) FROM tblAppointment WHERE userId=? AND status='待就诊'",
                    student.getUserId(), warnings, "医院模块尚未初始化"));
        }
        if (overview.getOverdueBorrowCount() > 0) {
            warnings.add("有 " + overview.getOverdueBorrowCount() + " 本图书已经逾期");
        }
        if (student.getStatus() != StudentStatus.ENROLLED) {
            if (overview.getSelectedCourseCount() > 0) {
                warnings.add("非在读学籍仍保留 " + overview.getSelectedCourseCount() + " 条选课记录");
            }
            if (overview.getActiveBorrowCount() > 0) {
                warnings.add("非在读学籍仍有 " + overview.getActiveBorrowCount() + " 本图书未归还");
            }
            if (overview.getPendingAppointmentCount() > 0) {
                warnings.add("非在读学籍仍有待就诊预约");
            }
            if (User.STATUS_NORMAL.equals(overview.getAccountStatus())) {
                warnings.add("非在读学籍的账号仍处于正常状态");
            }
        } else if (!User.STATUS_NORMAL.equals(overview.getAccountStatus())) {
            warnings.add("在读学籍的账号不是正常状态");
        }
        overview.setWarnings(warnings);
        return overview;
    }

    private static int count(Connection connection, String sql, String value) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, value);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getInt(1) : 0;
            }
        }
    }

    private static String text(Connection connection, String sql, String value) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, value);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getString(1) : null;
            }
        }
    }

    private static BigDecimal decimal(Connection connection, String sql, String value)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, value);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() && result.getBigDecimal(1) != null
                        ? result.getBigDecimal(1) : BigDecimal.ZERO;
            }
        }
    }

    private static int safeCount(Connection connection, String sql, String value,
                                 List<String> warnings, String unavailableMessage)
            throws SQLException {
        try {
            return count(connection, sql, value);
        } catch (SQLException exception) {
            if (isMissingTable(exception)) {
                addWarning(warnings, unavailableMessage);
                return 0;
            }
            throw exception;
        }
    }

    private static BigDecimal safeDecimal(Connection connection, String sql, String value,
                                          List<String> warnings, String unavailableMessage)
            throws SQLException {
        try {
            return decimal(connection, sql, value);
        } catch (SQLException exception) {
            if (isMissingTable(exception)) {
                addWarning(warnings, unavailableMessage);
                return BigDecimal.ZERO;
            }
            throw exception;
        }
    }

    private static boolean isMissingTable(SQLException exception) {
        return "42S02".equals(exception.getSQLState()) || exception.getErrorCode() == 1146;
    }

    private static void addWarning(List<String> warnings, String warning) {
        if (!warnings.contains(warning)) {
            warnings.add(warning);
        }
    }
}
