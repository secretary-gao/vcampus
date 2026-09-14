package vcampus.server.dao.Library;

import vcampus.common.vo.Library.Book;
import vcampus.server.dao.DbHelper;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.io.IOException;

public class BookDAO {

    /**
     * 按条件查询图书
     */
    public List<Book> queryBooks(String keyword) throws SQLException, IOException {
        List<Book> books = new ArrayList<>();
        String sql = "SELECT * FROM tblBook WHERE bookId LIKE ? OR bookName LIKE ? OR author LIKE ? OR category LIKE ?";
        String likeKeyword = "%" + keyword + "%";

        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

        pstmt.setString(1, likeKeyword);  // bookId
        pstmt.setString(2, likeKeyword);  // bookName
        pstmt.setString(3, likeKeyword);  // author
        pstmt.setString(4, likeKeyword);  // category

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Book book = new Book();
                    book.setBookId(rs.getString("bookId"));
                    book.setBookName(rs.getString("bookName"));
                    book.setAuthor(rs.getString("author"));
                    book.setIsbn(rs.getString("isbn"));
                    book.setCategory(rs.getString("category"));
                    book.setTotalCount(rs.getInt("totalCount"));
                    book.setAvailableCount(rs.getInt("availableCount"));
                    books.add(book);
                }
            }
        }
        return books;
    }

    /**
     * 根据 bookId 查询单本图书
     */
    public Book getBookById(String bookId) throws SQLException, IOException {
        String sql = "SELECT * FROM tblBook WHERE bookId = ?";

        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, bookId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Book book = new Book();
                    book.setBookId(rs.getString("bookId"));
                    book.setBookName(rs.getString("bookName"));
                    book.setAuthor(rs.getString("author"));
                    book.setIsbn(rs.getString("isbn"));
                    book.setCategory(rs.getString("category"));
                    book.setTotalCount(rs.getInt("totalCount"));
                    book.setAvailableCount(rs.getInt("availableCount"));
                    return book;
                }
            }
        }
        return null;
    }

    /**
     * 新增图书
     */
    public boolean addBook(Book book) throws SQLException, IOException {
        String sql = "INSERT INTO tblBook (bookId, bookName, author, isbn, category, totalCount, availableCount) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, book.getBookId());
            pstmt.setString(2, book.getBookName());
            pstmt.setString(3, book.getAuthor());
            pstmt.setString(4, book.getIsbn());
            pstmt.setString(5, book.getCategory());
            pstmt.setInt(6, book.getTotalCount());
            pstmt.setInt(7, book.getAvailableCount());

            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 更新图书信息
     */
    public boolean updateBook(Book book) throws SQLException, IOException {
        String sql = "UPDATE tblBook SET bookName=?, author=?, isbn=?, category=?, totalCount=?, availableCount=? " +
                     "WHERE bookId=?";

        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, book.getBookName());
            pstmt.setString(2, book.getAuthor());
            pstmt.setString(3, book.getIsbn());
            pstmt.setString(4, book.getCategory());
            pstmt.setInt(5, book.getTotalCount());
            pstmt.setInt(6, book.getAvailableCount());
            pstmt.setString(7, book.getBookId());

            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 删除图书
     */
    public boolean deleteBook(String bookId) throws SQLException, IOException {
        String sql = "DELETE FROM tblBook WHERE bookId = ?";

        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, bookId);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 借书：扣减可借数量
     */
    public boolean decreaseAvailableCount(String bookId) throws SQLException, IOException {
        String sql = "UPDATE tblBook SET availableCount = availableCount - 1 " +
                     "WHERE bookId = ? AND availableCount > 0";

        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, bookId);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 还书：恢复可借数量
     */
    public boolean increaseAvailableCount(String bookId) throws SQLException, IOException {
        String sql = "UPDATE tblBook SET availableCount = availableCount + 1 WHERE bookId = ?";

        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, bookId);
            return pstmt.executeUpdate() > 0;
        }
    }
}