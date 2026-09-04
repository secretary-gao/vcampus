package vcampus.server.srv;

/** 学籍业务校验失败时抛出的异常，同时携带返回客户端的状态码。 */
public class StudentServiceException extends Exception {

    private static final long serialVersionUID = 1L;

    private final String _statusCode;

    public StudentServiceException(String statusCode, String message) {
        super(message);
        this._statusCode = statusCode;
    }

    public String getStatusCode() {
        return _statusCode;
    }
}
