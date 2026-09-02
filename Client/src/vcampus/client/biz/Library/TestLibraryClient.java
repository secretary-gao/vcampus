package vcampus.client.biz.Library;

import vcampus.common.vo.Message;
import vcampus.common.vo.MessageType;
import vcampus.common.vo.Library.Book;
import vcampus.common.vo.Library.BorrowRecord;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 图书馆模块 Socket 链路测试
 * 
 * 直接连接 Server，发送 Message，验证 ServerThread 路由是否正确
 */
public class TestLibraryClient {

    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8888;

    public static void main(String[] args) {
        System.out.println("========== 图书馆模块 Socket 链路测试 ==========\n");

        // ===== 测试 1：查询图书 =====
        testQueryBooks("Java");

        // ===== 测试 2：借书 =====
        testBorrowBook("zhangsan", "B001");

        // ===== 测试 3：查询借阅记录 =====
        testGetBorrowRecords("zhangsan");

        // ===== 测试 4：还书 =====
        testReturnBook("R1788168701047");
    }

    private static void testQueryBooks(String keyword) {
        System.out.println("【测试 1】查询图书：keyword=" + keyword);
        try {
            Message request = new Message();
            request.setUid(System.currentTimeMillis());
            request.setName("queryBooks");
            request.setType(MessageType.COMMAND);
            request.setData(keyword);
            request.setSender("TestUser");

            Message response = sendAndReceive(request);
            System.out.println("  状态码：" + response.getStatusCode());
            if ("200".equals(response.getStatusCode())) {
                @SuppressWarnings("unchecked")
                List<Book> books = (List<Book>) response.getData();
                System.out.println("  查到 " + books.size() + " 本书");
                for (Book b : books) {
                    System.out.println("    " + b.getBookId() + " | " + b.getBookName() + " | 可借: " + b.getAvailableCount());
                }
            } else {
                System.out.println("  错误：" + response.getData());
            }
        } catch (Exception e) {
            System.err.println("  ❌ 异常：" + e.getMessage());
            e.printStackTrace();
        }
        System.out.println();
    }

    private static void testBorrowBook(String userId, String bookId) {
        System.out.println("【测试 2】借书：userId=" + userId + ", bookId=" + bookId);
        try {
            Map<String, String> params = new HashMap<>();
            params.put("userId", userId);
            params.put("bookId", bookId);

            Message request = new Message();
            request.setUid(System.currentTimeMillis());
            request.setName("borrowBook");
            request.setType(MessageType.COMMAND);
            request.setData(params);
            request.setSender(userId);

            Message response = sendAndReceive(request);
            System.out.println("  状态码：" + response.getStatusCode());
            System.out.println("  消息：" + response.getData());
        } catch (Exception e) {
            System.err.println("  ❌ 异常：" + e.getMessage());
            e.printStackTrace();
        }
        System.out.println();
    }

    private static void testGetBorrowRecords(String userId) {
        System.out.println("【测试 3】查询借阅记录：userId=" + userId);
        try {
            Message request = new Message();
            request.setUid(System.currentTimeMillis());
            request.setName("getBorrowRecords");
            request.setType(MessageType.COMMAND);
            request.setData(userId);
            request.setSender(userId);

            Message response = sendAndReceive(request);
            System.out.println("  状态码：" + response.getStatusCode());
            if ("200".equals(response.getStatusCode())) {
                @SuppressWarnings("unchecked")
                List<BorrowRecord> records = (List<BorrowRecord>) response.getData();
                System.out.println("  共 " + records.size() + " 条记录");
                for (BorrowRecord r : records) {
                    System.out.println("    " + r.getRecordId() + " | " + r.getBookId() + " | " + r.getStatus());
                }
            } else {
                System.out.println("  错误：" + response.getData());
            }
        } catch (Exception e) {
            System.err.println("  ❌ 异常：" + e.getMessage());
            e.printStackTrace();
        }
        System.out.println();
    }

    private static void testReturnBook(String recordId) {
        System.out.println("【测试 4】还书：recordId=" + recordId);
        try {
            Message request = new Message();
            request.setUid(System.currentTimeMillis());
            request.setName("returnBook");
            request.setType(MessageType.COMMAND);
            request.setData(recordId);
            request.setSender("zhangsan");

            Message response = sendAndReceive(request);
            System.out.println("  状态码：" + response.getStatusCode());
            System.out.println("  消息：" + response.getData());
        } catch (Exception e) {
            System.err.println("  ❌ 异常：" + e.getMessage());
            e.printStackTrace();
        }
        System.out.println();
    }

    /**
     * 发送请求并接收响应（短连接，每次新建 Socket）
     */
    private static Message sendAndReceive(Message request) throws Exception {
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT)) {
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

            out.writeObject(request);
            out.flush();

            return (Message) in.readObject();
        }
    }
}