/*
 * CourseClientException
 *
 * Version 1.0
 *
 * 2026-09-04
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.client.biz;

import java.io.IOException;

/** 服务器返回非成功状态时抛出的选课客户端异常。 */
public class CourseClientException extends IOException {

    private static final long serialVersionUID = 1L;

    /** 服务器状态码。 */
    private final String _statusCode;

    /** 创建带服务器状态码的客户端异常。 */
    public CourseClientException(String statusCode, String message) {
        super(message);
        this._statusCode = statusCode;
    }

    /** 获取服务器状态码。 */
    public String getStatusCode() {
        return _statusCode;
    }
}
