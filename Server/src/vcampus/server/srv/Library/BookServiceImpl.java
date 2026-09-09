package vcampus.server.srv.Library;

import vcampus.common.vo.Library.Book;
import vcampus.common.vo.Library.BorrowRecord;
import vcampus.server.dao.Library.BookDAO;
import vcampus.server.dao.Library.BorrowRecordDAO;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

/**
 * 图书馆业务服务实现类
 */
public class BookServiceImpl implements IBookService {

    private final BookDAO bookDAO = new BookDAO();
    private final BorrowRecordDAO borrowDAO = new BorrowRecordDAO();

    @Override
    public List<Book> queryBooks(String keyword) throws SQLException, IOException {
        return bookDAO.queryBooks(keyword);
    }

    @Override
    public boolean borrowBook(String userId, String bookId) throws SQLException, IOException {
        // 1. 检查图书是否存在
        Book book = bookDAO.getBookById(bookId);
        if (book == null) {
            return false;
        }

        // 2. 检查库存
        if (book.getAvailableCount() <= 0) {
            return false;
        }

        // 3. 检查用户是否已经借了这本书且未归还
        if (borrowDAO.isUserBorrowingBook(userId, bookId)) {
            return false;
        }

        // 4. 扣减库存
        boolean decreased = bookDAO.decreaseAvailableCount(bookId);
        if (!decreased) {
            return false;
        }

        // 5. 生成借阅记录
        BorrowRecord record = new BorrowRecord();
        record.setRecordId("R" + System.currentTimeMillis());
        record.setUserId(userId);
        record.setBookId(bookId);
        record.setBorrowDate(new Date());

        // 应还日期：当前日期 + 14 天
        long dueTime = System.currentTimeMillis() + 14L * 24 * 60 * 60 * 1000;
        record.setDueDate(new Date(dueTime));

        record.setReturnDate(null);
        record.setStatus("借阅中");

        return borrowDAO.addBorrowRecord(record);
    }

    @Override
    public boolean returnBook(String recordId) throws SQLException, IOException {
        // 1. 查询借阅记录
        BorrowRecord record = borrowDAO.getRecordById(recordId);
        if (record == null) {
            return false;
        }

        // 2. 检查是否已归还
        if ("已归还".equals(record.getStatus())) {
            return false;
        }

        // 3. 更新记录状态
        boolean updated = borrowDAO.updateRecordStatus(recordId, "已归还", new java.sql.Date(System.currentTimeMillis()));
        if (!updated) {
            return false;
        }

        // 4. 恢复库存
        return bookDAO.increaseAvailableCount(record.getBookId());
    }

    @Override
    public List<BorrowRecord> getBorrowRecords(String userId) throws SQLException, IOException {
        return borrowDAO.getRecordsByUserId(userId);
    }

    @Override
    public BorrowRecord getRecordById(String recordId) throws SQLException, IOException {
        return borrowDAO.getRecordById(recordId);
    }

    @Override
    public boolean addBook(Book book) throws SQLException, IOException {
        return bookDAO.addBook(book);
    }

    @Override
    public boolean updateBook(Book book) throws SQLException, IOException {
        return bookDAO.updateBook(book);
    }

    @Override
    public boolean deleteBook(String bookId) throws SQLException, IOException {
        return bookDAO.deleteBook(bookId);
    }
}