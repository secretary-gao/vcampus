package vcampus.common.constant;

/** 学籍模块客户端和服务器端共同使用的通信常量。 */
public final class StudentProtocol {

    public static final String DEFAULT_HOST = "127.0.0.1";
    public static final int DEFAULT_PORT = 8888;

    public static final String LIST = "student.list";
    public static final String QUERY_BY_ID = "student.query.id";
    public static final String QUERY_BY_CARD = "student.query.card";
    public static final String QUERY_BY_NAME = "student.query.name";
    public static final String ADD = "student.add";
    public static final String UPDATE = "student.update";
    public static final String DELETE = "student.delete";

    public static final String STATUS_SUCCESS = "200";
    public static final String STATUS_BAD_REQUEST = "400";
    public static final String STATUS_NOT_FOUND = "404";
    public static final String STATUS_CONFLICT = "409";
    public static final String STATUS_ERROR = "500";

    private StudentProtocol() {
    }
}
