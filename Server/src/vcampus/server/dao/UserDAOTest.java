/*
 * UserDAOTest
 *
 * Version 1.0
 *
 * 2026-08-28
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.server.dao;

import vcampus.common.util.MD5Util;
import vcampus.common.vo.User;

import java.io.IOException;
import java.sql.SQLException;

/**
 * {@link UserDAO} 的功能验证程序：模拟一次"注册 → 登录查询"的数据层流程，
 * 证明用户表可以正常写入与读取，且密码已按 MD5 摘要存储。
 *
 * <p>运行前请确认：已执行 {@code sql/vcampus_schema.sql} 建库建表；已正确
 * 配置 {@code Server/db.properties}；运行时的工作目录为项目根目录。</p>
 */
public class UserDAOTest {

    /**
     * 程序入口：注册测试用户并查询验证。
     *
     * @param args 命令行参数（未使用）
     */
    public static void main(String[] args) {
        UserDAO dao = new UserDAO();

        String rawPwd = "123456";
        String md5Pwd = MD5Util.md5(rawPwd);
        System.out.println("明文密码 [" + rawPwd + "] 的 MD5 摘要：" + md5Pwd);

        User user = new User("09010210", "测试用户", 20, "男", md5Pwd, "学生");

        try {
            User exist = dao.findByUId(user.getUId());
            if (exist != null) {
                System.out.println("用户 [" + user.getUId() + "] 已存在，跳过注册。");
            } else {
                boolean ok = dao.insert(user);
                System.out.println(ok ? "注册成功：" + user.getUId() : "注册失败：" + user.getUId());
            }

            User queried = dao.findByUId(user.getUId());
            if (queried != null) {
                System.out.println("登录查询成功，数据库中的用户：" + queried);
            } else {
                System.out.println("查询失败：数据库中未找到用户 " + user.getUId());
            }
        } catch (SQLException | IOException e) {
            System.err.println("数据层测试失败：" + e.getMessage());
            System.err.println("请检查：1) MySQL 服务是否启动；2) Server/db.properties 是否已正确填写；"
                    + "3) 是否已执行 sql/vcampus_schema.sql。");
            e.printStackTrace();
        }
    }
}
