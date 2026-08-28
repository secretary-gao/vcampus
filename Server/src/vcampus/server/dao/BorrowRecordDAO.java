package vcampus.server.dao;

import vcampus.common.vo.BorrowRecord;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BorrowRecordDAO {

    public boolean addBorrowRecord(BorrowRecord record) throws SQLException, IOException {
        String sql = "INSERT INTO tblBorrow (recordId, userId, bookId, borrowDate, dueDate, returnDate, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, record.getRecordId());
            pstmt.setString(2, record.getUserId());
            pstmt.setString(3, record.getBookId());
            pstmt.setDate(4, new java.sql.Date(record.getBorrowDate().getTime()));
            pstmt.setDate(5, new java.sql.Date(record.getDueDate().getTime()));

            if (record.getReturnDate() != null) {
                pstmt.setDate(6, new java.sql.Date(record.getReturnDate().getTime()));
            } else {
                pstmt.setNull(6, Types.DATE);
            }

            pstmt.setString(7, record.getStatus());
            return pstmt.executeUpdate() > 0;
        }
    }

    public List<BorrowRecord> getRecordsByUserId(String userId) throws SQLException, IOException {
        List<BorrowRecord> records = new ArrayList<>();
        String sql = "SELECT * FROM tblBorrow WHERE userId = ? ORDER BY borrowDate DESC";

        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, userId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    records.add(mapResultSetToRecord(rs));
                }
            }
        }
        return records;
    }

    public List<BorrowRecord> getRecordsByBookId(String bookId) throws SQLException, IOException {
        List<BorrowRecord> records = new ArrayList<>();
        String sql = "SELECT * FROM tblBorrow WHERE bookId = ? ORDER BY borrowDate DESC";

        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, bookId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    records.add(mapResultSetToRecord(rs));
                }
            }
        }
        return records;
    }

    public List<BorrowRecord> getAllRecords() throws SQLException, IOException {
        List<BorrowRecord> records = new ArrayList<>();
        String sql = "SELECT * FROM tblBorrow ORDER BY borrowDate DESC";

        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                records.add(mapResultSetToRecord(rs));
            }
        }
        return records;
    }

    public BorrowRecord getRecordById(String recordId) throws SQLException, IOException {
        String sql = "SELECT * FROM tblBorrow WHERE recordId = ?";

        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, recordId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToRecord(rs);
                }
            }
        }
        return null;
    }

    public boolean updateRecordStatus(String recordId, String status, Date returnDate) throws SQLException, IOException {
        String sql = "UPDATE tblBorrow SET status = ?, returnDate = ? WHERE recordId = ?";

        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, status);
            if (returnDate != null) {
                pstmt.setDate(2, new java.sql.Date(returnDate.getTime()));
            } else {
                pstmt.setNull(2, Types.DATE);
            }
            pstmt.setString(3, recordId);

            return pstmt.executeUpdate() > 0;
        }
    }

    public boolean isUserBorrowingBook(String userId, String bookId) throws SQLException, IOException {
        String sql = "SELECT COUNT(*) FROM tblBorrow WHERE userId = ? AND bookId = ? AND status = '借阅中'";

        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, userId);
            pstmt.setString(2, bookId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    private BorrowRecord mapResultSetToRecord(ResultSet rs) throws SQLException {
        BorrowRecord record = new BorrowRecord();
        record.setRecordId(rs.getString("recordId"));
        record.setUserId(rs.getString("userId"));
        record.setBookId(rs.getString("bookId"));

        Date borrowDate = rs.getDate("borrowDate");
        if (borrowDate != null) {
            record.setBorrowDate(new java.util.Date(borrowDate.getTime()));
        }

        Date dueDate = rs.getDate("dueDate");
        if (dueDate != null) {
            record.setDueDate(new java.util.Date(dueDate.getTime()));
        }

        Date returnDate = rs.getDate("returnDate");
        if (returnDate != null) {
            record.setReturnDate(new java.util.Date(returnDate.getTime()));
        }

        record.setStatus(rs.getString("status"));
        return record;
    }
}