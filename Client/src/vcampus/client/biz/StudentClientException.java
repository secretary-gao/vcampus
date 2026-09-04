package vcampus.client.biz;

/** 服务器已收到请求，但学籍业务处理未成功。 */
public class StudentClientException extends Exception {

    private static final long serialVersionUID = 1L;

    private final String _statusCode;

    public StudentClientException(String statusCode, String message) {
        super(message);
        this._statusCode = statusCode;
    }

    public String getStatusCode() {
        return _statusCode;
    }
}
