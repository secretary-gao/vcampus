package vcampus.server.srv.Library;

import vcampus.common.vo.Message;
import vcampus.common.vo.MessageType;
import vcampus.common.vo.Library.Book;
import vcampus.common.vo.Library.BorrowRecord;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * LibraryHandler 自测类
 * 
 * 不启动 Server，直接调用 LibraryHandler，快速验证业务逻辑。
 * 测通后再接入 ServerThread 做端到端测试。
 */
public class TestLibraryHandler {

    public static void main(String[] args) {
        LibraryHandler handler = new LibraryHandler();

        // ===== 测试 1：查询图书 =====
        System.out.println("========== 测试 1：查询图书 ==========");
        Message queryReq = new Message();
        queryReq.setUid(System.currentTimeMillis());
        queryReq.setName("queryBooks");
        queryReq.setType(MessageType.COMMAND);
        queryReq.setData("");
        queryReq.setSender("TestUser");

        Message queryResp = handler.handle(queryReq);
        System.out.println("状态码：" + queryResp.getStatusCode());
        if ("200".equals(queryResp.getStatusCode())) {
            @SuppressWarnings("unchecked")
            List<Book> books = (List<Book>) queryResp.getData();
            System.out.println("查到 " + books.size() + " 本书：");
            for (Book b : books) {
                System.out.println("  " + b.getBookId() + " | " + b.getBookName() + " | 可借: " + b.getAvailableCount());
            }
        } else {
            System.out.println("错误：" + queryResp.getData());
        }

        // ===== 测试 2：借书 =====
        System.out.println("\n========== 测试 2：借书 ==========");
        Map<String, String> borrowParams = new HashMap<>();
        borrowParams.put("userId", "zhangsan");
        borrowParams.put("bookId", "B003");

        Message borrowReq = new Message();
        borrowReq.setUid(System.currentTimeMillis());
        borrowReq.setName("borrowBook");
        borrowReq.setType(MessageType.COMMAND);
        borrowReq.setData(borrowParams);
        borrowReq.setSender("zhangsan");

        Message borrowResp = handler.handle(borrowReq);
        System.out.println("状态码：" + borrowResp.getStatusCode());
        System.out.println("消息：" + borrowResp.getData());

        // ===== 测试 3：查询借阅记录 =====
        System.out.println("\n========== 测试 3：查询借阅记录 ==========");
        Message recordReq = new Message();
        recordReq.setUid(System.currentTimeMillis());
        recordReq.setName("getBorrowRecords");
        recordReq.setType(MessageType.COMMAND);
        recordReq.setData("zhangsan");
        recordReq.setSender("zhangsan");

        Message recordResp = handler.handle(recordReq);
        System.out.println("状态码：" + recordResp.getStatusCode());
        if ("200".equals(recordResp.getStatusCode())) {
            @SuppressWarnings("unchecked")
            List<BorrowRecord> records = (List<BorrowRecord>) recordResp.getData();
            System.out.println("共 " + records.size() + " 条借阅记录：");
            for (BorrowRecord r : records) {
                System.out.println("  " + r.getRecordId() + " | " + r.getBookId() + " | " + r.getStatus());
            }
        }

        System.out.println("\n========== 测试 4：还书 ==========");
        String testRecordId = "R1788167496423";  // 用 R001 测试，B001 库存会 +1

        Message returnReq = new Message();
        returnReq.setUid(System.currentTimeMillis());
        returnReq.setName("returnBook");
        returnReq.setType(MessageType.COMMAND);
        returnReq.setData(testRecordId);
        returnReq.setSender("zhangsan");

        Message returnResp = handler.handle(returnReq);
        System.out.println("状态码：" + returnResp.getStatusCode());
        System.out.println("消息：" + returnResp.getData());

        // 验证：还书后再查一次 zhangsan 的借阅记录
        System.out.println("\n========== 验证：还书后查询借阅记录 ==========");
        Message recordReq2 = new Message();
        recordReq2.setUid(System.currentTimeMillis());
        recordReq2.setName("getBorrowRecords");
        recordReq2.setType(MessageType.COMMAND);
        recordReq2.setData("zhangsan");
        recordReq2.setSender("zhangsan");

        Message recordResp2 = handler.handle(recordReq2);
        System.out.println("状态码：" + recordResp2.getStatusCode());
        if ("200".equals(recordResp2.getStatusCode())) {
            @SuppressWarnings("unchecked")
            List<BorrowRecord> records2 = (List<BorrowRecord>) recordResp2.getData();
            System.out.println("共 " + records2.size() + " 条借阅记录：");
            for (BorrowRecord r : records2) {
                System.out.println("  " + r.getRecordId() + " | " + r.getBookId() + " | " + r.getStatus());
            }
        }
    }
}