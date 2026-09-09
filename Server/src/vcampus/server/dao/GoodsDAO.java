/*
 * GoodsDAO
 *
 * Version 1.0
 *
 * 2026-09-01
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.server.dao;

import vcampus.common.vo.Goods;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 商品表（tblGoods）的数据访问类，封装对商品表的查询与写入操作。
 * 对上层业务服务层屏蔽具体的 SQL 语句与数据库细节，连接统一由
 * {@link DbHelper} 提供。
 *
 * <p>购买流程中的"锁定商品行 + 扣减库存"需要与订单插入处于同一事务，
 * 因此额外提供接收 {@link Connection} 参数的 {@code findByGoodsIdForUpdate}
 * 与 {@code updateStock}，由业务层统一控制事务边界。</p>
 */
public class GoodsDAO {

    /**
     * 新增商品。
     *
     * @param goods 待插入的商品对象
     * @return 插入成功返回 {@code true}，否则返回 {@code false}
     * @throws SQLException 数据库操作异常
     * @throws IOException  数据库配置文件读取异常
     */
    public boolean insert(Goods goods) throws SQLException, IOException {
        String sql = "INSERT INTO tblGoods (goodsId, goodsName, category, price, stock, imageUrl) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, goods.getGoodsId());
            pstmt.setString(2, goods.getGoodsName());
            pstmt.setString(3, goods.getCategory());
            pstmt.setBigDecimal(4, goods.getPrice());
            pstmt.setInt(5, goods.getStock());
            pstmt.setString(6, goods.getImageUrl());
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 修改商品（编号不可修改）。
     *
     * @param goods 待修改的商品对象（以 goodsId 定位）
     * @return 修改成功返回 {@code true}，否则返回 {@code false}
     * @throws SQLException 数据库操作异常
     * @throws IOException  数据库配置文件读取异常
     */
    public boolean update(Goods goods) throws SQLException, IOException {
        String sql = "UPDATE tblGoods SET goodsName = ?, category = ?, price = ?, stock = ?, imageUrl = ? "
                + "WHERE goodsId = ?";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, goods.getGoodsName());
            pstmt.setString(2, goods.getCategory());
            pstmt.setBigDecimal(3, goods.getPrice());
            pstmt.setInt(4, goods.getStock());
            pstmt.setString(5, goods.getImageUrl());
            pstmt.setString(6, goods.getGoodsId());
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 删除商品。
     *
     * @param goodsId 商品编号
     * @return 删除成功返回 {@code true}，否则返回 {@code false}
     * @throws SQLException 数据库操作异常
     * @throws IOException  数据库配置文件读取异常
     */
    public boolean delete(String goodsId) throws SQLException, IOException {
        String sql = "DELETE FROM tblGoods WHERE goodsId = ?";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, goodsId);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 按照商品编号查询商品。
     *
     * @param goodsId 商品编号
     * @return 查询到的商品对象；若不存在则返回 {@code null}
     * @throws SQLException 数据库操作异常
     * @throws IOException  数据库配置文件读取异常
     */
    public Goods findByGoodsId(String goodsId) throws SQLException, IOException {
        String sql = "SELECT goodsId, goodsName, category, price, stock, imageUrl FROM tblGoods WHERE goodsId = ?";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, goodsId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    /**
     * 按关键字（名称/类别模糊）与类别筛选查询商品列表。
     *
     * @param keyword  名称/类别关键字，可为 {@code null} 或空串（表示不筛选）
     * @param category 类别，可为 {@code null} 或空串（表示不筛选）
     * @return 符合条件的商品列表
     * @throws SQLException 数据库操作异常
     * @throws IOException  数据库配置文件读取异常
     */
    public List<Goods> queryByCondition(String keyword, String category) throws SQLException, IOException {
        StringBuilder sql = new StringBuilder(
                "SELECT goodsId, goodsName, category, price, stock, imageUrl FROM tblGoods WHERE 1 = 1");
        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (goodsName LIKE ? OR category LIKE ?)");
            String like = "%" + keyword.trim() + "%";
            params.add(like);
            params.add(like);
        }
        if (category != null && !category.isBlank()) {
            sql.append(" AND category = ?");
            params.add(category.trim());
        }
        sql.append(" ORDER BY goodsId");

        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                List<Goods> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
                return list;
            }
        }
    }

    /**
     * 按商品编号查询商品并加行锁（事务内），用于购买时防止并发超卖。
     *
     * @param conn    当前事务使用的连接
     * @param goodsId 商品编号
     * @return 查询到的商品对象；若不存在则返回 {@code null}
     * @throws SQLException 数据库操作异常
     */
    public Goods findByGoodsIdForUpdate(Connection conn, String goodsId) throws SQLException {
        String sql = "SELECT goodsId, goodsName, category, price, stock, imageUrl FROM tblGoods WHERE goodsId = ? FOR UPDATE";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, goodsId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    /**
     * 更新库存（事务内），delta 为增量（购买时传负数），并防止库存减为负。
     *
     * @param conn    当前事务使用的连接
     * @param goodsId 商品编号
     * @param delta   库存增量（购买时传负值）
     * @return 受影响行数（成功扣减返回 1）
     * @throws SQLException 数据库操作异常
     */
    public int updateStock(Connection conn, String goodsId, int delta) throws SQLException {
        String sql = "UPDATE tblGoods SET stock = stock + ? WHERE goodsId = ? AND stock + ? >= 0";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, delta);
            pstmt.setString(2, goodsId);
            pstmt.setInt(3, delta);
            return pstmt.executeUpdate();
        }
    }

    /**
     * 将结果集当前行映射为 Goods 对象。
     *
     * @param rs 指向当前行的结果集
     * @return 映射后的 Goods 对象
     * @throws SQLException 读取结果集时发生异常
     */
    private Goods mapRow(ResultSet rs) throws SQLException {
        Goods goods = new Goods();
        goods.setGoodsId(rs.getString("goodsId"));
        goods.setGoodsName(rs.getString("goodsName"));
        goods.setCategory(rs.getString("category"));
        goods.setPrice(rs.getBigDecimal("price"));
        goods.setStock(rs.getInt("stock"));
        goods.setImageUrl(rs.getString("imageUrl"));
        return goods;
    }
}
