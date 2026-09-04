/*
 * Server
 *
 * Version 1.0
 *
 * 2026-08-30
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.server.srv;
import vcampus.common.constant.IConstant;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
/**
 * 服务器程序入口。主线程创建 {@link ServerSocket} 并绑定固定端口
 * （见 {@link IConstant#SERVER_PORT}），循环监听客户端连接请求；
 * 每接受一个新连接，就创建一个 {@link ServerThread} 交给独立线程处理，
 * 主线程立刻回到 {@code accept()} 继续等待下一个客户端，从而实现
 * "一个客户端一个线程"的并发模型。
 */
public class Server {
    /**
     * 程序入口：启动服务器并进入accept循环。
     *
     * @param args 命令行参数（未使用）
     */
    public static void main(String[] args) {
        System.out.println("Vcampus 服务器启动中...");
        try (ServerSocket serverSocket = new ServerSocket(IConstant.SERVER_PORT)) {
            System.out.println("服务器已启动，正在监听端口：" + IConstant.SERVER_PORT);
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("客户端已连接：" + socket.getRemoteSocketAddress());
                new Thread(new ServerThread(socket)).start();
            }
        } catch (IOException e) {
            System.err.println("服务器启动失败：" + e.getMessage());
            e.printStackTrace();
        }
    }
}
