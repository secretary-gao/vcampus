/*
 * UserClientSrvTest
 *
 * Version 1.0
 *
 * 2026-08-30
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.client.biz;

import vcampus.common.util.MD5Util;
import vcampus.common.vo.Message;
import vcampus.common.vo.User;

/**
 * {@link UserClientSrv} 的端到端验证程序：不涉及界面，直接通过 Socket
 * 向服务器发起一次注册请求和一次登录请求，打印服务器返回的响应，用于
 * 确认"客户端 → Socket → 服务器 → 数据库"整条链路是否打通。
 *
 * <p>运行前请先启动 {@code vcampus.server.srv.Server}。</p>
 */
public class UserClientSrvTest {

    /**
     * 程序入口：依次测试注册与登录两个请求。
     *
     * @param args 命令行参数（未使用）
     */
    public static void main(String[] args) {
        IUserClientSrv userClientSrv = new UserClientSrv();
        String uid = "88888888";
        String rawPwd = "abc123";

        User newUser = new User();
        newUser.setUId(uid);
        newUser.setUName("客户端测试用户");
        newUser.setUAge(21);
        newUser.setUSex("女");
        newUser.setUPwd(MD5Util.md5(rawPwd));
        newUser.setURole("学生");

        try {
            System.out.println("=== 测试注册 ===");
            Message registerResponse = userClientSrv.register(newUser);
            System.out.println("注册响应：statusCode=" + registerResponse.getStatusCode()
                    + ", data=" + registerResponse.getData());

            System.out.println("=== 测试登录（正确密码）===");
            User loginUser = new User();
            loginUser.setUId(uid);
            loginUser.setUPwd(MD5Util.md5(rawPwd));
            Message loginResponse = userClientSrv.login(loginUser);
            System.out.println("登录响应：statusCode=" + loginResponse.getStatusCode()
                    + ", data=" + loginResponse.getData());

            System.out.println("=== 测试登录（错误密码）===");
            User wrongUser = new User();
            wrongUser.setUId(uid);
            wrongUser.setUPwd(MD5Util.md5("wrong-password"));
            Message wrongResponse = userClientSrv.login(wrongUser);
            System.out.println("登录响应：statusCode=" + wrongResponse.getStatusCode()
                    + ", data=" + wrongResponse.getData());
        } catch (Exception e) {
            System.err.println("客户端通信测试失败：" + e.getMessage());
            System.err.println("请确认服务器 vcampus.server.srv.Server 是否已启动。");
            e.printStackTrace();
        }
    }
}
