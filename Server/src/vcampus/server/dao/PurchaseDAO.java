/*
 * PurchaseDAO
 *
 * Version 1.0
 *
 * 2026-09-01
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.server.dao;

import vcampus.common.vo.PurchaseRecord;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * 商品购买记录表（tblPurchase）的数据访问类，封装对购买记录表的查询与写入操作。
 * 对上层业务服务层屏蔽具体的 SQL 语句与数据库细节，连接统一由 {@link DbHelper} 提供。
 *
 * <p>购买流程中"插入订单 + 扣减库存"需要处于同一事务，因此插入订单方法接收
 * {@link Connection} 参数，由业务层统一控制事务边界。查询时通过 LEFT JOIN
 * tblGoods 附带商品名称，便于界面展示。</p>
 */
public class PurchaseDAO {

    /** 查询购买记录时附带商品名称的公共 SELECT 片段。 */
    private static final String SELECT_FIELDS =
            "SELECT p.orderId, p.userId, p.goodsId, g.goodsName, p.quantity, p.totalPrice, p.orderTime "
                    + "FROM tblPurchase p LEFT JOIN tblGoods g ON p.goodsId = g.goodsId ";

    /**
     * 新增购买记录（事务内）。
     *
     * @param conn   当前事务使用的连接
     * @param record 待插入的购买记录对象
     * @return 插入成功返回 {@code true}，否则返回 {@code false}
     * @throws SQLException 数据库操作异常
     */
    public boolean insert(Connection conn, PurchaseRecord record) throws SQLException {
        String sql = "INSERT INTO tblPurchase (orderId, userId, goodsId, quantity, totalPrice, orderTime) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, record.getOrderId());
            pstmt.setString(2, record.getUserId());
            pstmt.setString(3, record.getGoodsId());
            pstmt.setInt(4, record.getQuantity());
            pstmt.setBigDecimal(5, record.getTotalPrice());
            pstmt.setTimestamp(6, Timestamp.valueOf(record.getOrderTime()));
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 查询某个用户的购买记录（按时间倒序）。
     *
     * @param userId 购买人ID
     * @return 该用户的购买记录列表
     * @throws SQLException 数据库操作异常
     * @throws IOException  数据库配置文件读取异常
     */
    public List<PurchaseRecord> findByUserId(String userId) throws SQLException, IOException {
        String sql = SELECT_FIELDS + "WHERE p.userId = ? ORDER BY p.orderTime DESC, p.orderId DESC";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return mapList(rs);
            }
        }
    }

    /**
     * 查询全部购买记录（管理员对账用，按时间倒序）。
     *
     * @return 全部购买记录列表
     * @throws SQLException 数据库操作异常
     * @throws IOException  数据库配置文件读取异常
     */
    public List<PurchaseRecord> findAll() throws SQLException, IOException {
        String sql = SELECT_FIELDS + "ORDER BY p.orderTime DESC, p.orderId DESC";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            return mapList(rs);
        }
    }

    /**
     * 判断某商品是否已存在购买记录（用于删除商品前的校验）。
     *
     * @param goodsId 商品编号
     * @return 若存在购买记录返回 {@code true}，否则返回 {@code false}
     * @throws SQLException 数据库操作异常
     * @throws IOException  数据库配置文件读取异常
     */
    public boolean existsByGoodsId(String goodsId) throws SQLException, IOException {
        String sql = "SELECT COUNT(*) FROM tblPurchase WHERE goodsId = ?";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, goodsId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    /**
     * 将结果集逐行映射为 PurchaseRecord 列表。
     *
     * @param rs 结果集
     * @return 购买记录列表
     * @throws SQLException 读取结果集时发生异常
     */
    private List<PurchaseRecord> mapList(ResultSet rs) throws SQLException {
        List<PurchaseRecord> list = new ArrayList<>();
        while (rs.next()) {
            PurchaseRecord r = new PurchaseRecord();
            r.setOrderId(rs.getString("orderId"));
            r.setUserId(rs.getString("userId"));
            r.setGoodsId(rs.getString("goodsId"));
            r.setGoodsName(rs.getString("goodsName"));
            r.setQuantity(rs.getInt("quantity"));
            r.setTotalPrice(rs.getBigDecimal("totalPrice"));
            Timestamp ts = rs.getTimestamp("orderTime");
            if (ts != null) {
                r.setOrderTime(ts.toLocalDateTime());
            }
            list.add(r);
        }
        return list;
    }
}
