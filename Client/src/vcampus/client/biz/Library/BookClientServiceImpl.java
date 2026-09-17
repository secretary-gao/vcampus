package vcampus.client.biz.Library;

import vcampus.common.constant.IConstant;
import vcampus.common.vo.Message;
import vcampus.common.vo.MessageType;
import vcampus.common.vo.Library.Book;
import vcampus.common.vo.Library.BorrowRecord;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * 图书馆模块客户端业务服务实现类
 * 
 * 实现 IBookClientService 接口，封装所有图书馆相关的网络请求。
 * 每个方法构建对应的 Message，通过 Socket 发送到服务端，
 * 并解析返回的 Message 提取结果。
 * 
 * 参考 UserClientSrv 的实现风格。
 */
public class BookClientServiceImpl implements IBookClientService {

    private static final String SERVER_HOST = IConstant.SERVER_HOST;
    private static final int SERVER_PORT = IConstant.SERVER_PORT;

    @Override
    @SuppressWarnings("unchecked")
    public List<Book> queryBooks(String keyword) throws IOException, ClassNotFoundException {
        Message request = new Message();
        request.setUid(System.currentTimeMillis());
        request.setName("queryBooks");
        request.setType(MessageType.COMMAND);
        request.setData(keyword);
        request.setSender("Client");

        Message response = sendAndReceive(request);

        if ("200".equals(response.getStatusCode())) {
            return (List<Book>) response.getData();
        } else {
            throw new IOException("查询图书失败：" + response.getData());
        }
    }

    @Override
    public boolean borrowBook(String userId, String bookId) throws IOException, ClassNotFoundException {
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

        return "200".equals(response.getStatusCode());
    }

    @Override
    public boolean returnBook(String recordId) throws IOException, ClassNotFoundException {
        Message request = new Message();
        request.setUid(System.currentTimeMillis());
        request.setName("returnBook");
        request.setType(MessageType.COMMAND);
        request.setData(recordId);
        request.setSender("Client");

        Message response = sendAndReceive(request);

        return "200".equals(response.getStatusCode());
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<BorrowRecord> getBorrowRecords(String userId) throws IOException, ClassNotFoundException {
        Message request = new Message();
        request.setUid(System.currentTimeMillis());
        request.setName("getBorrowRecords");
        request.setType(MessageType.COMMAND);
        request.setData(userId);
        request.setSender(userId);

        Message response = sendAndReceive(request);

        if ("200".equals(response.getStatusCode())) {
            return (List<BorrowRecord>) response.getData();
        } else {
            throw new IOException("查询借阅记录失败：" + response.getData());
        }
    }

    /**
     * 发送请求并接收响应（短连接，每次新建 Socket）
     * 
     * @param request 请求 Message
     * @return 响应 Message
     * @throws IOException 网络通信异常
     * @throws ClassNotFoundException 反序列化异常
     */
    private Message sendAndReceive(Message request) throws IOException, ClassNotFoundException {
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            out.writeObject(request);
            out.flush();

            return (Message) in.readObject();
        }
    }

    @Override
    public boolean addBook(Book book) throws IOException, ClassNotFoundException {
        Message request = new Message();
        request.setUid(System.currentTimeMillis());
        request.setName(IConstant.MSG_ADD_BOOK);
        request.setType(MessageType.COMMAND);
        request.setData(book);
        request.setSender("admin");

        Message response = sendAndReceive(request);
        return IConstant.STATUS_SUCCESS.equals(response.getStatusCode());
    }

    @Override
    public boolean updateBook(Book book) throws IOException, ClassNotFoundException {
        Message request = new Message();
        request.setUid(System.currentTimeMillis());
        request.setName(IConstant.MSG_UPDATE_BOOK);
        request.setType(MessageType.COMMAND);
        request.setData(book);
        request.setSender("admin");

        Message response = sendAndReceive(request);
        return IConstant.STATUS_SUCCESS.equals(response.getStatusCode());
    }

    @Override
    public boolean deleteBook(String bookId) throws IOException, ClassNotFoundException {
        Message request = new Message();
        request.setUid(System.currentTimeMillis());
        request.setName(IConstant.MSG_DELETE_BOOK);
        request.setType(MessageType.COMMAND);
        request.setData(bookId);
        request.setSender("admin");

        Message response = sendAndReceive(request);
        return IConstant.STATUS_SUCCESS.equals(response.getStatusCode());
    }
}