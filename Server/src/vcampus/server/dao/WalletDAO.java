/*
 * WalletDAO
 *
 * Version 1.0
 *
 * 2026-09-09
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.server.dao;

import vcampus.common.vo.Wallet;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 校园卡钱包表（tblWallet）的数据访问类，封装余额的查询、充值与扣减。
 *
 * <p>购买扣款需要与"插入订单 + 扣减库存"处于同一事务，因此 {@code deduct}
 * 接收 {@link Connection} 参数，由业务层统一控制事务边界；扣减条件带
 * {@code balance >= ?}，从数据库层面避免余额被扣成负数。</p>
 */
public class WalletDAO {

    /**
     * 按用户ID查询钱包（不存在返回 {@code null}，表示该用户还没有余额记录）。
     *
     * @param userId 用户ID
     * @return 钱包对象；不存在时返回 {@code null}
     * @throws SQLException 数据库操作异常
     * @throws IOException  数据库配置文件读取异常
     */
    public Wallet findByUserId(String userId) throws SQLException, IOException {
        String sql = "SELECT userId, balance FROM tblWallet WHERE userId = ?";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Wallet(rs.getString("userId"), rs.getBigDecimal("balance"));
                }
                return null;
            }
        }
    }

    /**
     * 充值：钱包不存在则新建并写入，已存在则累加。幂等安全。
     *
     * @param userId 用户ID
     * @param amount 充值金额（正数）
     * @return 写入成功返回 {@code true}
     * @throws SQLException 数据库操作异常
     * @throws IOException  数据库配置文件读取异常
     */
    public boolean recharge(String userId, BigDecimal amount) throws SQLException, IOException {
        String sql = "INSERT INTO tblWallet (userId, balance) VALUES (?, ?) "
                + "ON DUPLICATE KEY UPDATE balance = balance + ?";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            pstmt.setBigDecimal(2, amount);
            pstmt.setBigDecimal(3, amount);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 购买扣款（事务内）：余额充足才扣减，避免扣成负数。
     *
     * @param conn   当前事务使用的连接
     * @param userId 用户ID
     * @param amount 扣减金额（正数）
     * @return 受影响行数（1 表示扣款成功；0 表示余额不足或钱包不存在）
     * @throws SQLException 数据库操作异常
     */
    public int deduct(Connection conn, String userId, BigDecimal amount) throws SQLException {
        String sql = "UPDATE tblWallet SET balance = balance - ? WHERE userId = ? AND balance >= ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setBigDecimal(1, amount);
            pstmt.setString(2, userId);
            pstmt.setBigDecimal(3, amount);
            return pstmt.executeUpdate();
        }
    }
}
