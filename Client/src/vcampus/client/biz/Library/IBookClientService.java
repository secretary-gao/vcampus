package vcampus.client.biz.Library;

import vcampus.common.vo.Library.Book;
import vcampus.common.vo.Library.BorrowRecord;

import java.io.IOException;
import java.util.List;

/**
 * 图书馆模块客户端业务服务接口
 * 
 * 定义客户端调用服务器图书馆服务的所有方法。
 * 每个方法封装为一个网络请求，通过 Socket 发送 Message 到服务端，
 * 并等待返回结果。
 * 
 * 参考 UserClientSrv 的设计，保持一致的调用风格。
 */
public interface IBookClientService {

    /**
     * 按关键词查询图书
     * 
     * @param keyword 搜索关键词（书名/作者/分类模糊匹配），为空时返回所有图书
     * @return 匹配的图书列表，无结果时返回空列表
     * @throws IOException 网络通信异常
     * @throws ClassNotFoundException 反序列化异常
     */
    List<Book> queryBooks(String keyword) throws IOException, ClassNotFoundException;

    /**
     * 借书
     * 
     * @param userId 借阅人 ID
     * @param bookId 图书 ID
     * @return true 借书成功，false 借书失败
     * @throws IOException 网络通信异常
     * @throws ClassNotFoundException 反序列化异常
     */
    boolean borrowBook(String userId, String bookId) throws IOException, ClassNotFoundException;

    /**
     * 还书（根据借阅记录 ID）
     * 
     * @param recordId 借阅记录 ID
     * @return true 还书成功，false 还书失败
     * @throws IOException 网络通信异常
     * @throws ClassNotFoundException 反序列化异常
     */
    boolean returnBook(String recordId) throws IOException, ClassNotFoundException;

    /**
     * 查询某用户的借阅记录
     * 
     * @param userId 用户 ID
     * @return 该用户的借阅记录列表，无记录时返回空列表
     * @throws IOException 网络通信异常
     * @throws ClassNotFoundException 反序列化异常
     */
    List<BorrowRecord> getBorrowRecords(String userId) throws IOException, ClassNotFoundException;
}