/*
 * PromotionDAO
 *
 * Version 1.0
 *
 * 2026-09-16
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.server.dao;

import vcampus.common.vo.Promotion;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 商品促销表（tblPromotion）的数据访问类，封装"每日特价"活动的查询。
 *
 * <p>活动按"商品 × 星期"配置：weekday 1=周一 … 7=周日，0=每天。
 * 查询当日活动时把 {@code weekday = 0}（每天特价）也一并取出，由业务层决定最终取哪条
 * （见 {@link vcampus.server.srv.StoreServerSrv}：同一商品有多条活动时取折扣最大的一条）。</p>
 *
 * <p>结算需要按当日活动计价，因此提供接收 {@link Connection} 的重载，让促销读取与
 * 扣库存、扣余额处于同一个事务；查询商品时则用自带连接的版本。</p>
 */
public class PromotionDAO {

    /** 查询促销活动的公共 SELECT 片段（附带商品名称，便于界面与日志展示）。 */
    private static final String SELECT_FIELDS =
            "SELECT p.promoId, p.goodsId, g.goodsName, p.discountRate, p.weekday, p.remark "
                    + "FROM tblPromotion p LEFT JOIN tblGoods g ON p.goodsId = g.goodsId ";

    /**
     * 查询指定星期的促销活动（含"每天特价"），使用独立连接。
     *
     * @param weekday 星期（1=周一 … 7=周日）
     * @return 当日生效的活动列表
     * @throws SQLException 数据库操作异常
     * @throws IOException  数据库配置文件读取异常
     */
    public List<Promotion> findByWeekday(int weekday) throws SQLException, IOException {
        try (Connection conn = DbHelper.getConnection()) {
            return findByWeekday(conn, weekday);
        }
    }

    /**
     * 查询指定星期的促销活动（含"每天特价"），使用调用方传入的连接（事务内使用）。
     *
     * @param conn    数据库连接
     * @param weekday 星期（1=周一 … 7=周日）
     * @return 当日生效的活动列表
     * @throws SQLException 数据库操作异常
     */
    public List<Promotion> findByWeekday(Connection conn, int weekday) throws SQLException {
        String sql = SELECT_FIELDS + "WHERE p.weekday = ? OR p.weekday = 0 "
                + "ORDER BY p.discountRate ASC, p.goodsId";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, weekday);
            try (ResultSet rs = pstmt.executeQuery()) {
                return mapList(rs);
            }
        }
    }

    /**
     * 查询全部促销活动（管理/自测用）。
     *
     * @return 全部活动列表
     * @throws SQLException 数据库操作异常
     * @throws IOException  数据库配置文件读取异常
     */
    public List<Promotion> findAll() throws SQLException, IOException {
        String sql = SELECT_FIELDS + "ORDER BY p.weekday, p.goodsId";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            return mapList(rs);
        }
    }

    /**
     * 按促销编号查询活动。
     *
     * @param promoId 促销编号
     * @return 活动；不存在返回 {@code null}
     * @throws SQLException 数据库操作异常
     * @throws IOException  数据库配置文件读取异常
     */
    public Promotion findByPromoId(String promoId) throws SQLException, IOException {
        String sql = SELECT_FIELDS + "WHERE p.promoId = ?";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, promoId);
            try (ResultSet rs = pstmt.executeQuery()) {
                List<Promotion> list = mapList(rs);
                return list.isEmpty() ? null : list.get(0);
            }
        }
    }

    /**
     * 新增促销活动（管理员配置"每日特价"用）。
     *
     * @param promotion 活动对象
     * @return 写入成功返回 {@code true}
     * @throws SQLException 数据库操作异常（含"同一商品同一天已有活动"的唯一约束冲突）
     * @throws IOException  数据库配置文件读取异常
     */
    public boolean insert(Promotion promotion) throws SQLException, IOException {
        String sql = "INSERT INTO tblPromotion (promoId, goodsId, discountRate, weekday, remark) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, promotion.getPromoId());
            pstmt.setString(2, promotion.getGoodsId());
            pstmt.setBigDecimal(3, promotion.getDiscountRate());
            pstmt.setInt(4, promotion.getWeekday());
            pstmt.setString(5, promotion.getRemark());
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 修改促销活动（按促销编号定位；商品、折扣率、星期、说明都可改）。
     *
     * @param promotion 活动对象
     * @return 受影响行数（1 表示修改成功）
     * @throws SQLException 数据库操作异常（含唯一约束冲突）
     * @throws IOException  数据库配置文件读取异常
     */
    public int update(Promotion promotion) throws SQLException, IOException {
        String sql = "UPDATE tblPromotion SET goodsId = ?, discountRate = ?, weekday = ?, remark = ? "
                + "WHERE promoId = ?";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, promotion.getGoodsId());
            pstmt.setBigDecimal(2, promotion.getDiscountRate());
            pstmt.setInt(3, promotion.getWeekday());
            pstmt.setString(4, promotion.getRemark());
            pstmt.setString(5, promotion.getPromoId());
            return pstmt.executeUpdate();
        }
    }

    /**
     * 删除促销活动。
     *
     * @param promoId 促销编号
     * @return 受影响行数（1 表示删除成功）
     * @throws SQLException 数据库操作异常
     * @throws IOException  数据库配置文件读取异常
     */
    public int delete(String promoId) throws SQLException, IOException {
        String sql = "DELETE FROM tblPromotion WHERE promoId = ?";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, promoId);
            return pstmt.executeUpdate();
        }
    }

    /**
     * 将结果集逐行映射为 Promotion 列表。
     *
     * @param rs 结果集
     * @return 活动列表
     * @throws SQLException 读取结果集时发生异常
     */
    private List<Promotion> mapList(ResultSet rs) throws SQLException {
        List<Promotion> list = new ArrayList<>();
        while (rs.next()) {
            Promotion p = new Promotion();
            p.setPromoId(rs.getString("promoId"));
            p.setGoodsId(rs.getString("goodsId"));
            p.setGoodsName(rs.getString("goodsName"));
            p.setDiscountRate(rs.getBigDecimal("discountRate"));
            p.setWeekday(rs.getInt("weekday"));
            p.setRemark(rs.getString("remark"));
            list.add(p);
        }
        return list;
    }
}
