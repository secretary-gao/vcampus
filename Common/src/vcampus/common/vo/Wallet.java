/*
 * Wallet
 *
 * Version 1.0
 *
 * 2026-09-09
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.common.vo;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 校园卡钱包实体，对应商店模块的 tblWallet 表，用于在客户端与服务器端之间
 * 传输用户余额（显示余额、充值、购买扣款等场景）。因为需要通过 Socket 传输，
 * 所以实现 {@link Serializable} 接口。
 */
public class Wallet implements Serializable {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 用户ID（主键，关联 tblUser.uId）。 */
    private String _userId;

    /** 余额（>=0）。 */
    private BigDecimal _balance;

    /**
     * 无参构造方法。
     */
    public Wallet() {
    }

    /**
     * 全参构造方法。
     *
     * @param userId  用户ID
     * @param balance 余额
     */
    public Wallet(String userId, BigDecimal balance) {
        this._userId = userId;
        this._balance = balance;
    }

    /**
     * 获取用户ID。
     *
     * @return 用户ID
     */
    public String getUserId() {
        return _userId;
    }

    /**
     * 设置用户ID。
     *
     * @param userId 用户ID
     */
    public void setUserId(String userId) {
        this._userId = userId;
    }

    /**
     * 获取余额。
     *
     * @return 余额
     */
    public BigDecimal getBalance() {
        return _balance;
    }

    /**
     * 设置余额。
     *
     * @param balance 余额
     */
    public void setBalance(BigDecimal balance) {
        this._balance = balance;
    }

    /**
     * 返回该钱包的可读字符串表示。
     *
     * @return 字符串描述
     */
    @Override
    public String toString() {
        return "Wallet{userId='" + _userId + "', balance=" + _balance + '}';
    }
}
