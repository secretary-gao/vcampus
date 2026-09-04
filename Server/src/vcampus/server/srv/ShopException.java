/*
 * ShopException
 *
 * Version 1.0
 *
 * 2026-09-01
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.server.srv;

/**
 * 商店模块业务异常，用于把具体的业务失败原因（如商品不存在、库存不足、
 * 编号重复、存在购买记录禁止删除等）连同对应的状态码返回给客户端。
 *
 * <p>与 {@link java.sql.SQLException}（表示真正的数据库异常）区分开，
 * 便于 {@code ServerThread} 的商店处理器据此设置正确的 {@code statusCode}。</p>
 */
public class ShopException extends Exception {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 业务状态码（对应 {@link vcampus.common.constant.IConstant} 中的 STATUS_*）。 */
    private final String _statusCode;

    /**
     * 构造业务异常。
     *
     * @param statusCode 业务状态码
     * @param message    业务提示信息
     */
    public ShopException(String statusCode, String message) {
        super(message);
        this._statusCode = statusCode;
    }

    /**
     * 获取业务状态码。
     *
     * @return 业务状态码
     */
    public String getStatusCode() {
        return _statusCode;
    }
}
