/*
 * User
 *
 * Version 1.0
 *
 * 2026-08-28
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.common.vo;

import java.io.Serializable;

/**
 * 用户实体类，对应数据库 tblUser 表的字段，用于在客户端与服务器端之间
 * 传输用户信息（登录、注册、登出等场景）。因为需要通过 Socket 传输，
 * 所以实现 {@link Serializable} 接口。
 *
 * <p>字段设计对应共享说明书中 tblUser 表：uId（登录ID）、uName（姓名）、
 * uAge（年龄）、uSex（性别）、uPwd（密码，存 MD5 摘要）、uRole（角色）。</p>
 */
public class User implements Serializable {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 登录ID，定长8位（如学号/工号）。 */
    private String _uId;

    /** 姓名。 */
    private String _uName;

    /** 年龄。 */
    private Integer _uAge;

    /** 性别（男/女）。 */
    private String _uSex;

    /** 密码，存 MD5 摘要（32位十六进制）。 */
    private String _uPwd;

    /** 用户角色（学生/教师/管理员）。 */
    private String _uRole;

    /**
     * 无参构造方法。
     */
    public User() {
    }

    /**
     * 全参构造方法。
     *
     * @param uId   登录ID
     * @param uName 姓名
     * @param uAge  年龄
     * @param uSex  性别
     * @param uPwd  密码（MD5 摘要）
     * @param uRole 角色
     */
    public User(String uId, String uName, Integer uAge, String uSex, String uPwd, String uRole) {
        this._uId = uId;
        this._uName = uName;
        this._uAge = uAge;
        this._uSex = uSex;
        this._uPwd = uPwd;
        this._uRole = uRole;
    }

    /**
     * 获取登录ID。
     *
     * @return 登录ID
     */
    public String getUId() {
        return _uId;
    }

    /**
     * 设置登录ID。
     *
     * @param uId 登录ID
     */
    public void setUId(String uId) {
        this._uId = uId;
    }

    /**
     * 获取姓名。
     *
     * @return 姓名
     */
    public String getUName() {
        return _uName;
    }

    /**
     * 设置姓名。
     *
     * @param uName 姓名
     */
    public void setUName(String uName) {
        this._uName = uName;
    }

    /**
     * 获取年龄。
     *
     * @return 年龄
     */
    public Integer getUAge() {
        return _uAge;
    }

    /**
     * 设置年龄。
     *
     * @param uAge 年龄
     */
    public void setUAge(Integer uAge) {
        this._uAge = uAge;
    }

    /**
     * 获取性别。
     *
     * @return 性别
     */
    public String getUSex() {
        return _uSex;
    }

    /**
     * 设置性别。
     *
     * @param uSex 性别
     */
    public void setUSex(String uSex) {
        this._uSex = uSex;
    }

    /**
     * 获取密码（MD5 摘要）。
     *
     * @return 密码
     */
    public String getUPwd() {
        return _uPwd;
    }

    /**
     * 设置密码（MD5 摘要）。
     *
     * @param uPwd 密码
     */
    public void setUPwd(String uPwd) {
        this._uPwd = uPwd;
    }

    /**
     * 获取用户角色。
     *
     * @return 用户角色
     */
    public String getURole() {
        return _uRole;
    }

    /**
     * 设置用户角色。
     *
     * @param uRole 用户角色
     */
    public void setURole(String uRole) {
        this._uRole = uRole;
    }

    /**
     * 返回该用户的可读字符串表示，便于调试时打印查看。
     *
     * @return 用户信息的字符串描述
     */
    @Override
    public String toString() {
        return "User{" +
                "uId='" + _uId + '\'' +
                ", uName='" + _uName + '\'' +
                ", uAge=" + _uAge +
                ", uSex='" + _uSex + '\'' +
                ", uPwd='" + _uPwd + '\'' +
                ", uRole='" + _uRole + '\'' +
                '}';
    }
}
