/*
 * Message
 *
 * Version 1.0
 *
 * 2026-08-26
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.common.vo;

import java.io.Serializable;

/**
 * 客户端与服务器端通信所使用的消息封装类，对应共享说明书公共模块设计中的
 * {@code Message} 类。客户端与服务器端通过 Socket 传输 {@code Message} 对象，
 * 因此该类必须实现 {@link Serializable} 接口。
 *
 * <p>字段设计严格按照共享说明书.docx 中"公共模块设计说明"一节的 Message 表格：
 * uid、name、type、statusCode、data、sender 共 6 个字段。</p>
 */
public class Message implements Serializable {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 消息唯一标识符。 */
    private Long _uid;

    /** 消息名称。 */
    private String _name;

    /** 消息类型（命令/数据），参见 {@link MessageType}。 */
    private MessageType _type;

    /** 状态码，用于标识请求处理结果（如成功/失败）。 */
    private String _statusCode;

    /** 传输的具体数据，必须是可序列化对象。 */
    private Object _data;

    /** 发送者，通常为用户名（String）或用户对象（User）。 */
    private Object _sender;

    /**
     * 无参构造方法。
     */
    public Message() {
    }

    /**
     * 全参构造方法。
     *
     * @param uid        消息唯一标识符
     * @param name       消息名称
     * @param type       消息类型
     * @param statusCode 状态码
     * @param data       传输数据
     * @param sender     发送者
     */
    public Message(Long uid, String name, MessageType type, String statusCode, Object data, Object sender) {
        this._uid = uid;
        this._name = name;
        this._type = type;
        this._statusCode = statusCode;
        this._data = data;
        this._sender = sender;
    }

    /**
     * 获取消息唯一标识符。
     *
     * @return 消息唯一标识符
     */
    public Long getUid() {
        return _uid;
    }

    /**
     * 设置消息唯一标识符。
     *
     * @param uid 消息唯一标识符
     */
    public void setUid(Long uid) {
        this._uid = uid;
    }

    /**
     * 获取消息名称。
     *
     * @return 消息名称
     */
    public String getName() {
        return _name;
    }

    /**
     * 设置消息名称。
     *
     * @param name 消息名称
     */
    public void setName(String name) {
        this._name = name;
    }

    /**
     * 获取消息类型。
     *
     * @return 消息类型
     */
    public MessageType getType() {
        return _type;
    }

    /**
     * 设置消息类型。
     *
     * @param type 消息类型
     */
    public void setType(MessageType type) {
        this._type = type;
    }

    /**
     * 获取状态码。
     *
     * @return 状态码
     */
    public String getStatusCode() {
        return _statusCode;
    }

    /**
     * 设置状态码。
     *
     * @param statusCode 状态码
     */
    public void setStatusCode(String statusCode) {
        this._statusCode = statusCode;
    }

    /**
     * 获取传输数据。
     *
     * @return 传输数据
     */
    public Object getData() {
        return _data;
    }

    /**
     * 设置传输数据。
     *
     * @param data 传输数据
     */
    public void setData(Object data) {
        this._data = data;
    }

    /**
     * 获取发送者。
     *
     * @return 发送者
     */
    public Object getSender() {
        return _sender;
    }

    /**
     * 设置发送者。
     *
     * @param sender 发送者
     */
    public void setSender(Object sender) {
        this._sender = sender;
    }

    /**
     * 返回该消息的可读字符串表示，便于调试时打印查看。
     *
     * @return 消息内容的字符串描述
     */
    @Override
    public String toString() {
        return "Message{" +
                "uid=" + _uid +
                ", name='" + _name + '\'' +
                ", type=" + _type +
                ", statusCode='" + _statusCode + '\'' +
                ", data=" + _data +
                ", sender=" + _sender +
                '}';
    }
}
