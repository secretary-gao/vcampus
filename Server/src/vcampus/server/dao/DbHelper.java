/*
 * DbHelper
 *
 * Version 1.0
 *
 * 2026-08-26
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.server.dao;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * 数据库工具类，封装 JDBC 与 MySQL 数据库的连接建立、关闭等公共操作。
 * 服务器端各业务模块的 DAO 类（如 UserDAO）均通过本类获取数据库连接，
 * 避免各模块重复编写数据库连接管理代码。
 *
 * <p>连接参数从项目根目录下的 {@code Server/db.properties} 读取，该文件
 * 不纳入版本管理（见 .gitignore），需要各开发者根据
 * {@code Server/db.properties.example} 自行复制并填写本地 MySQL 密码。
 * 因此运行本类相关的 main 方法或服务器程序时，须保证当前工作目录为
 * 项目根目录（即 coJava 目录）。</p>
 */
public final class DbHelper {

    /** db.properties 相对项目根目录的路径。 */
    private static final String CONFIG_PATH = "Server/db.properties";

    /** 缓存已加载的数据库连接配置，避免重复读取文件。 */
    private static Properties properties;

    /**
     * 私有构造方法，禁止实例化工具类。
     */
    private DbHelper() {
    }

    /**
     * 加载 {@code Server/db.properties} 配置文件（只在首次调用时读取）。
     *
     * @return 数据库连接配置
     * @throws IOException 当配置文件不存在或读取失败时抛出
     */
    private static synchronized Properties loadProperties() throws IOException {
        if (properties == null) {
            Properties p = new Properties();
            try (InputStream in = new FileInputStream(CONFIG_PATH)) {
                p.load(in);
            } catch (IOException e) {
                throw new IOException("未找到数据库配置文件：" + CONFIG_PATH
                        + "，请复制 Server/db.properties.example 为 Server/db.properties 并填写本地 MySQL 密码。", e);
            }
            properties = p;
        }
        return properties;
    }

    /**
     * 获取一个新的数据库连接。
     *
     * @return 与 vCampus 数据库建立的连接
     * @throws SQLException 当数据库连接失败时抛出
     * @throws IOException  当配置文件读取失败时抛出
     */
    public static Connection getConnection() throws SQLException, IOException {
        Properties p = loadProperties();
        String url = p.getProperty("jdbc.url");
        String driver = p.getProperty("jdbc.driver");
        String username = p.getProperty("jdbc.username");
        String password = p.getProperty("jdbc.password");

        try {
            Class.forName(driver);
        } catch (ClassNotFoundException e) {
            throw new SQLException("未找到数据库驱动类：" + driver + "，请确认 classpath 中包含 mysql-connector-j 的 jar 包。", e);
        }

        return DriverManager.getConnection(url, username, password);
    }

    /**
     * 安全关闭数据库相关资源，任意参数均可为 {@code null}。
     * 关闭过程中出现的异常只会打印日志，不会向上抛出。
     *
     * @param conn 数据库连接，可为 {@code null}
     * @param stmt SQL 语句对象，可为 {@code null}
     * @param rs   结果集，可为 {@code null}
     */
    public static void close(Connection conn, Statement stmt, ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                System.err.println("关闭 ResultSet 失败：" + e.getMessage());
            }
        }
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException e) {
                System.err.println("关闭 Statement 失败：" + e.getMessage());
            }
        }
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                System.err.println("关闭 Connection 失败：" + e.getMessage());
            }
        }
    }
}
