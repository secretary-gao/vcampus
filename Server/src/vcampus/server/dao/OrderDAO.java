/*
 * OrderDAO
 *
 * Version 1.0
 *
 * 2026-09-14
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.server.dao;

import vcampus.common.vo.Order;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * 订单主表（tblOrder）的数据访问类，封装订单主记录的写入与查询。
 *
 * <p>购物车结算时，"插入订单主表 + 插入多条明细 + 扣减库存 + 扣减余额"必须处于同一事务，
 * 因此 {@link #insert(Connection, Order)} 接收 {@link Connection} 参数，由业务层统一控制
 * 事务边界；订单明细（tblPurchase）由 {@link PurchaseDAO} 负责写入。</p>
 */
public class OrderDAO {

    /** 查询订单主记录的公共 SELECT 片段。 */
    private static final String SELECT_FIELDS =
            "SELECT orderId, userId, totalAmount, orderTime FROM tblOrder ";

    /**
     * 新增订单主记录（事务内）。
     *
     * @param conn  当前事务使用的连接
     * @param order 待插入的订单对象
     * @return 插入成功返回 {@code true}，否则返回 {@code false}
     * @throws SQLException 数据库操作异常
     */
    public boolean insert(Connection conn, Order order) throws SQLException {
        String sql = "INSERT INTO tblOrder (orderId, userId, totalAmount, orderTime) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, order.getOrderId());
            pstmt.setString(2, order.getUserId());
            pstmt.setBigDecimal(3, order.getTotalAmount() == null ? BigDecimal.ZERO : order.getTotalAmount());
            pstmt.setTimestamp(4, Timestamp.valueOf(order.getOrderTime()));
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 查询某个用户的订单（按下单时间倒序），不含明细。
     *
     * @param userId 下单人ID
     * @return 该用户的订单列表
     * @throws SQLException 数据库操作异常
     * @throws IOException  数据库配置文件读取异常
     */
    public List<Order> findByUserId(String userId) throws SQLException, IOException {
        String sql = SELECT_FIELDS + "WHERE userId = ? ORDER BY orderTime DESC, orderId DESC";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return mapList(rs);
            }
        }
    }

    /**
     * 查询全部订单（管理员对账用，按下单时间倒序），不含明细。
     *
     * @return 全部订单列表
     * @throws SQLException 数据库操作异常
     * @throws IOException  数据库配置文件读取异常
     */
    public List<Order> findAll() throws SQLException, IOException {
        String sql = SELECT_FIELDS + "ORDER BY orderTime DESC, orderId DESC";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            return mapList(rs);
        }
    }

    /**
     * 将结果集逐行映射为 Order 列表（不含明细）。
     *
     * @param rs 结果集
     * @return 订单列表
     * @throws SQLException 读取结果集时发生异常
     */
    private List<Order> mapList(ResultSet rs) throws SQLException {
        List<Order> list = new ArrayList<>();
        while (rs.next()) {
            Order o = new Order();
            o.setOrderId(rs.getString("orderId"));
            o.setUserId(rs.getString("userId"));
            o.setTotalAmount(rs.getBigDecimal("totalAmount"));
            Timestamp ts = rs.getTimestamp("orderTime");
            if (ts != null) {
                o.setOrderTime(ts.toLocalDateTime());
            }
            list.add(o);
        }
        return list;
    }
}
