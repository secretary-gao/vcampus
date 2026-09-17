package vcampus.common.constant;

/** 学籍模块客户端和服务器端共同使用的通信常量。 */
public final class StudentProtocol {

    /** 默认服务器地址，统一复用系统公共常量。 */
    public static final String DEFAULT_HOST = IConstant.SERVER_HOST;

    /** 默认服务器端口，统一复用系统公共常量。 */
    public static final int DEFAULT_PORT = IConstant.SERVER_PORT;

    public static final String LIST = "student.list";
    public static final String GET_SELF = "student.self";
    public static final String QUERY_BY_ID = "student.query.id";
    public static final String QUERY_BY_CARD = "student.query.card";
    public static final String QUERY_BY_NAME = "student.query.name";
    public static final String ADD = "student.add";
    public static final String UPDATE = "student.update";
    public static final String DELETE = "student.delete";
    public static final String OVERVIEW = "student.overview";

    public static final String STATUS_SUCCESS = IConstant.STATUS_SUCCESS;
    public static final String STATUS_BAD_REQUEST = IConstant.STATUS_BAD_REQUEST;
    public static final String STATUS_FORBIDDEN = IConstant.STATUS_FORBIDDEN;
    public static final String STATUS_NOT_FOUND = IConstant.STATUS_USER_NOT_FOUND;
    public static final String STATUS_CONFLICT = IConstant.STATUS_USER_EXISTS;
    public static final String STATUS_ERROR = IConstant.STATUS_ERROR;

    private StudentProtocol() {
    }
}
