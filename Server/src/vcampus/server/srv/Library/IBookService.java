package vcampus.server.srv.Library;

import vcampus.common.vo.Library.Book;
import vcampus.common.vo.Library.BorrowRecord;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * 图书馆业务服务接口
 * 
 * 定义图书馆模块的核心业务操作，由 BookServiceImpl 实现。
 * 每个方法对应一个完整的业务用例，内部组合调用 BookDAO 和 BorrowRecordDAO。
 */
public interface IBookService {

    /**
     * 按关键词查询图书
     * 
     * @param keyword 搜索关键词（书名/作者/分类模糊匹配），为空时返回所有图书
     * @return 匹配的图书列表，无结果时返回空列表
     * @throws SQLException 数据库操作异常
     * @throws IOException 配置文件读取异常
     */
    List<Book> queryBooks(String keyword) throws SQLException, IOException;

    /**
     * 借书
     * 
     * 业务逻辑：
     * 1. 检查图书是否存在
     * 2. 检查库存是否充足（availableCount > 0）
     * 3. 检查用户是否已借阅该书且未归还
     * 4. 扣减库存
     * 5. 生成借阅记录（状态为"借阅中"，应还日期为当前日期 + 14 天）
     * 
     * @param userId 借阅人 ID
     * @param bookId 图书 ID
     * @return true 借书成功，false 借书失败（库存不足/已借阅/图书不存在）
     * @throws SQLException 数据库操作异常
     * @throws IOException 配置文件读取异常
     */
    boolean borrowBook(String userId, String bookId) throws SQLException, IOException;

    /**
     * 还书
     * 
     * 业务逻辑：
     * 1. 检查借阅记录是否存在
     * 2. 检查该书是否已归还（避免重复还书）
     * 3. 更新借阅记录状态为"已归还"，记录归还日期
     * 4. 恢复图书库存（availableCount + 1）
     * 
     * @param recordId 借阅记录 ID
     * @return true 还书成功，false 还书失败（记录不存在/已归还）
     * @throws SQLException 数据库操作异常
     * @throws IOException 配置文件读取异常
     */
    boolean returnBook(String recordId) throws SQLException, IOException;

    /**
     * 查询某用户的借阅记录
     * 
     * @param userId 用户 ID
     * @return 该用户的借阅记录列表（按借阅日期降序），无记录时返回空列表
     * @throws SQLException 数据库操作异常
     * @throws IOException 配置文件读取异常
     */
    List<BorrowRecord> getBorrowRecords(String userId) throws SQLException, IOException;

    /**
     * 根据借阅记录 ID 查询单条记录
     * 
     * @param recordId 借阅记录 ID
     * @return 借阅记录对象，不存在时返回 null
     * @throws SQLException 数据库操作异常
     * @throws IOException 配置文件读取异常
     */
    BorrowRecord getRecordById(String recordId) throws SQLException, IOException;

    boolean addBook(Book book) throws SQLException, IOException;
    boolean updateBook(Book book) throws SQLException, IOException;
    boolean deleteBook(String bookId) throws SQLException, IOException;

}