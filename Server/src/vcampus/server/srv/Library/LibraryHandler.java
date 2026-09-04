package vcampus.server.srv.Library;

import vcampus.common.vo.Message;
import vcampus.common.vo.MessageType;
import vcampus.common.vo.Library.Book;
import vcampus.common.vo.Library.BorrowRecord;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * 图书馆请求处理器
 * 
 * 负责解析 Message，调用 IBookService，返回响应 Message
 */
public class LibraryHandler {

    private final IBookService bookService = new BookServiceImpl();

    /**
     * 处理图书馆模块的所有请求
     * 
     * @param request 客户端发来的请求 Message
     * @return 响应 Message
     */
    public Message handle(Message request) {
        String name = request.getName();

        try {
            switch (name) {
                case "queryBooks":
                    return handleQueryBooks(request);
                case "borrowBook":
                    return handleBorrowBook(request);
                case "returnBook":
                    return handleReturnBook(request);
                case "getBorrowRecords":
                    return handleGetBorrowRecords(request);
                default:
                    return new Message(
                        request.getUid(),
                        name,
                        MessageType.DATA,
                        "400",
                        "未知的图书馆操作：" + name,
                        "Server"
                    );
            }
        } catch (SQLException | IOException e) {
            e.printStackTrace();
            return new Message(
                request.getUid(),
                name,
                MessageType.DATA,
                "500",
                "服务器内部异常：" + e.getMessage(),
                "Server"
            );
        }
    }

    // ========== 各业务处理方法 ==========

    @SuppressWarnings("unchecked")
    private Message handleBorrowBook(Message request) throws SQLException, IOException {
        Map<String, String> params = (Map<String, String>) request.getData();
        String userId = params.get("userId");
        String bookId = params.get("bookId");

        boolean success = bookService.borrowBook(userId, bookId);

        if (success) {
            return new Message(
                request.getUid(),
                "borrowBook",
                MessageType.DATA,
                "200",
                "借书成功",
                "Server"
            );
        } else {
            return new Message(
                request.getUid(),
                "borrowBook",
                MessageType.DATA,
                "400",
                "借书失败（库存不足、已借阅或图书不存在）",
                "Server"
            );
        }
    }

    private Message handleReturnBook(Message request) throws SQLException, IOException {
        String recordId = (String) request.getData();

        boolean success = bookService.returnBook(recordId);

        if (success) {
            return new Message(
                request.getUid(),
                "returnBook",
                MessageType.DATA,
                "200",
                "还书成功",
                "Server"
            );
        } else {
            return new Message(
                request.getUid(),
                "returnBook",
                MessageType.DATA,
                "400",
                "还书失败（记录不存在或已归还）",
                "Server"
            );
        }
    }

    private Message handleQueryBooks(Message request) throws SQLException, IOException {
        String keyword = (String) request.getData();

        List<Book> books = bookService.queryBooks(keyword);

        return new Message(
            request.getUid(),
            "queryBooks",
            MessageType.DATA,
            "200",
            books,
            "Server"
        );
    }

    private Message handleGetBorrowRecords(Message request) throws SQLException, IOException {
        String userId = (String) request.getData();

        List<BorrowRecord> records = bookService.getBorrowRecords(userId);

        return new Message(
            request.getUid(),
            "getBorrowRecords",
            MessageType.DATA,
            "200",
            records,
            "Server"
        );
    }
}