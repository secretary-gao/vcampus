package vcampus.server.srv;

import vcampus.common.constant.StudentProtocol;
import vcampus.common.vo.Message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/** 学籍模块的独立联调服务器，后续可由组长的统一 Server 路由调用同一处理器。 */
public class StudentServer {

    public static void main(String[] args) {
        StudentRequestHandler handler = new StudentRequestHandler();
        try (ServerSocket serverSocket = new ServerSocket(StudentProtocol.DEFAULT_PORT)) {
            System.out.println("学生模块服务器已启动，端口：" + StudentProtocol.DEFAULT_PORT);
            while (true) {
                Socket socket = serverSocket.accept();
                Thread thread = new Thread(() -> handle(socket, handler));
                thread.setDaemon(false);
                thread.start();
            }
        } catch (IOException exception) {
            System.err.println("学生模块服务器启动失败：" + exception.getMessage());
        }
    }

    private static void handle(Socket socket, StudentRequestHandler handler) {
        try (Socket currentSocket = socket;
             ObjectOutputStream output = new ObjectOutputStream(currentSocket.getOutputStream())) {
            output.flush();
            try (ObjectInputStream input = new ObjectInputStream(currentSocket.getInputStream())) {
                Message request = (Message) input.readObject();
                Message response = handler.handle(request);
                output.writeObject(response);
                output.flush();
            }
        } catch (IOException | ClassNotFoundException exception) {
            System.err.println("处理学生模块请求失败：" + exception.getMessage());
        }
    }
}
