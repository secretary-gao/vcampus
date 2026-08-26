/*
 * DbHelperTest
 *
 * Version 1.0
 *
 * 2026-08-26
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.server.dao;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * {@link DbHelper} 的连通性验证程序：确认能够连接本地 vCampus 数据库，
 * 并能查询到 tblUser 表（哪怕表内暂无数据）。
 *
 * <p>运行前请确认：已执行 {@code sql/vcampus_schema.sql} 建库建表；
 * 已根据 {@code Server/db.properties.example} 复制出 {@code Server/db.properties}
 * 并填写正确的本地 MySQL 密码；运行时的工作目录为项目根目录。</p>
 */
public class DbHelperTest {

    /**
     * 程序入口：依次验证数据库连接、tblUser 表是否存在、当前记录数。
     *
     * @param args 命令行参数（未使用）
     */
    public static void main(String[] args) {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            conn = DbHelper.getConnection();
            System.out.println("数据库连接成功，当前 catalog：" + conn.getCatalog());

            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet tables = meta.getTables(null, null, "tblUser", null)) {
                if (tables.next()) {
                    System.out.println("已找到数据表：tblUser");
                } else {
                    System.out.println("未找到数据表 tblUser，请先执行 sql/vcampus_schema.sql 建库建表。");
                    return;
                }
            }

            stmt = conn.createStatement();
            rs = stmt.executeQuery("SELECT COUNT(*) AS cnt FROM tblUser");
            if (rs.next()) {
                System.out.println("tblUser 当前记录数：" + rs.getInt("cnt"));
            }
        } catch (SQLException | IOException e) {
            System.err.println("数据库连接测试失败：" + e.getMessage());
            System.err.println("请检查：1) MySQL 服务是否启动；2) Server/db.properties 是否已正确填写；"
                    + "3) 是否已执行 sql/vcampus_schema.sql。");
            e.printStackTrace();
        } finally {
            DbHelper.close(conn, stmt, rs);
        }
    }
}
