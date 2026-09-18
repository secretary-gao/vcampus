/*
 * IConstant
 *
 * Version 1.0
 *
 * 2026-08-28
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.common.constant;

/**
 * 系统公共常量接口，集中定义客户端与服务器端共用的常量，包括服务器地址、
 * 监听端口，以及通信状态码。接口中的字段隐式为 {@code public static final}。
 *
 * <p>
 * 客户端与服务器端都必须引用这里的常量，确保两侧使用相同的端口和状态码
 * 约定，避免硬编码造成的不一致。
 * </p>
 */
public interface IConstant {
    /**
     * 服务器地址。默认回环地址 {@code 127.0.0.1}（本机自测，客户端和服务器
     * 跑在同一台机器上）。跨机器答辩部署时，优先级从高到低：
     * <ol>
     *   <li>JVM 参数 {@code -Dvcampus.server.host=服务器IP}</li>
     *   <li>当前工作目录下的 {@code client.properties} 文件里的
     *       {@code server.host=服务器IP}（打包后的客户端专用，改这个文件
     *       就能换服务器地址，不用重新编译，也不用敲命令行参数）</li>
     *   <li>都没有就退回 {@code 127.0.0.1}</li>
     * </ol>
     * 所有客户端 {@code biz} 层仍然引用 {@link #SERVER_HOST} 这一个字段，
     * 不需要逐个改。
     */
    String SERVER_HOST = resolveServerHost();
    /**
     * 服务器监听端口。解析优先级同 {@link #SERVER_HOST}：JVM 参数
     * {@code -Dvcampus.server.port=}，其次 {@code client.properties} 里的
     * {@code server.port=}，都没有则默认 8888。
     */
    int SERVER_PORT = resolveServerPort();

    /**
     * 加载当前工作目录下的 {@code client.properties}（找不到或读取失败时返回
     * 空 {@link java.util.Properties}，不会抛异常影响启动——这个文件本来就是
     * 可选的部署期配置，不存在时应当静默退回默认值）。
     *
     * @return 解析到的配置，找不到文件时为空
     */
    static java.util.Properties loadClientProperties() {
        java.util.Properties p = new java.util.Properties();
        java.io.File f = new java.io.File("client.properties");
        if (f.isFile()) {
            try (java.io.InputStream in = new java.io.FileInputStream(f)) {
                p.load(in);
            } catch (java.io.IOException ignored) {
                // 读取失败就当没有这个文件，退回默认值，不影响客户端启动
            }
        }
        return p;
    }

    /**
     * 解析服务器地址，见 {@link #SERVER_HOST} 上的优先级说明。
     *
     * @return 实际使用的服务器地址
     */
    static String resolveServerHost() {
        String sys = System.getProperty("vcampus.server.host");
        if (sys != null && !sys.trim().isEmpty()) {
            return sys.trim();
        }
        String fromFile = loadClientProperties().getProperty("server.host");
        return (fromFile == null || fromFile.trim().isEmpty()) ? "127.0.0.1" : fromFile.trim();
    }

    /**
     * 解析服务器端口，见 {@link #SERVER_PORT} 上的优先级说明。数值不合法
     * （非数字）时同样静默退回默认端口 8888，不影响客户端启动。
     *
     * @return 实际使用的服务器端口
     */
    static int resolveServerPort() {
        String sys = System.getProperty("vcampus.server.port");
        String raw = (sys != null && !sys.trim().isEmpty())
                ? sys.trim() : loadClientProperties().getProperty("server.port");
        if (raw != null && !raw.trim().isEmpty()) {
            try {
                return Integer.parseInt(raw.trim());
            } catch (NumberFormatException ignored) {
                // 配置写错了就退回默认端口，不影响客户端启动
            }
        }
        return 8888;
    }

    /** 状态码：操作成功。 */
    String STATUS_SUCCESS = "200";
    /** 状态码：登录失败（密码错误）。 */
    String STATUS_LOGIN_FAIL = "401";
    /** 状态码：用户不存在。 */
    String STATUS_USER_NOT_FOUND = "404";

    /** 状态码：请求参数不符合业务要求。 */
    String STATUS_BAD_REQUEST = "400";

    /**
     * 状态码：当前用户无权执行该操作（如学籍模块的角色权限校验、
     * 非管理员尝试执行管理员专属操作）。跟 {@link #STATUS_ACCOUNT_DISABLED}
     * 数值上都是 403，但用在不同的消息类型（{@code Message.getName()}）
     * 下，客户端按各自的消息类型分别判断，不会混淆。
     */
    String STATUS_FORBIDDEN = "403";

    /** 状态码：注册时用户名已存在。 */
    String STATUS_USER_EXISTS = "409";
    /** 状态码：服务器内部异常。 */
    String STATUS_ERROR = "500";

    /** 状态码：账号已被管理员禁用。 */
    String STATUS_ACCOUNT_DISABLED = "403";

    /** 状态码：账号注册后还在等待管理员审核，暂时不能登录。 */
    String STATUS_ACCOUNT_PENDING = "402";

    // ========= 用户模块消息 =========
    /** 消息名：登录请求，对应 {@link vcampus.common.vo.Message#getName()}。 */
    String MSG_LOGIN = "login";
    /** 消息名：注册请求，对应 {@link vcampus.common.vo.Message#getName()}。 */
    String MSG_REGISTER = "register";
    /** 消息名：登出请求，对应 {@link vcampus.common.vo.Message#getName()}。 */
    String MSG_LOGOUT = "logout";

    //医院模块新增常量
    String MSG_HOSPITAL_GET_OCCUPIED_TIME = "hospital_get_occupied_time";

    /**
     * 消息名：管理员禁用/启用账号请求，对应说明书"管理员可注销/禁用账号"。
     * 请求 data 约定为 {@code Object[]{operatorUId, targetUId, newStatus}}。
     */
    String MSG_USER_SET_STATUS = "userSetStatus";

    /**
     * 消息名：管理员查询"待审核"账号列表请求。请求 data 为操作者登录ID
     * （{@code String}），成功响应 data 为 {@code List<User>}。
     */
    String MSG_USER_LIST_PENDING = "userListPending";

    /** 消息名：管理员将学生密码重置为系统初始密码。 */
    String MSG_USER_RESET_PASSWORD = "userResetPassword";

    /**
     * 消息名：学生角色自助注册请求（跟普通 {@link #MSG_REGISTER} 分开，
     * 因为学生注册除了写 tblUser，还要顺带写一条 tblStudent 学籍记录）。
     * 请求 data 约定为 {@code Object[]{User newUser, Student profile}}。
     */
    String MSG_REGISTER_STUDENT = "registerStudent";

    // ========== 图书馆模块消息类型 ==========
    String MSG_QUERY_BOOKS = "queryBooks";
    String MSG_BORROW_BOOK = "borrowBook";
    String MSG_RETURN_BOOK = "returnBook";
    String MSG_GET_BORROW_RECORDS = "getBorrowRecords";
    String MSG_ADD_BOOK = "addBook";
    String MSG_UPDATE_BOOK = "updateBook";
    String MSG_DELETE_BOOK = "deleteBook";
    String MSG_QUERY_PAPERS = "queryPapers";
    String MSG_GET_PDF_DATA = "getPdfData";
    String MSG_ADD_PAPER = "addPaper";
    String MSG_DELETE_PAPER = "deletePaper";
    String MSG_UPLOAD_PDF = "uploadPdf";

    // ========== 选课模块消息类型 ==========
    String MSG_COURSE_QUERY = "courseQuery";
    String MSG_COURSE_ADD = "courseAdd";
    String MSG_COURSE_UPDATE = "courseUpdate";
    String MSG_COURSE_DELETE = "courseDelete";
    String MSG_COURSE_SELECT = "courseSelect";
    String MSG_COURSE_DROP = "courseDrop";
    String MSG_COURSE_SELECTED_QUERY = "courseSelectedQuery";
    String MSG_COURSE_STUDENT_ID_QUERY = "courseStudentIdQuery";
    String MSG_COURSE_SCHEDULE_QUERY = "courseScheduleQuery";
    String MSG_COURSE_SCHEDULE_ADD = "courseScheduleAdd";
    String MSG_COURSE_SCHEDULE_UPDATE = "courseScheduleUpdate";
    String MSG_COURSE_SCHEDULE_DELETE = "courseScheduleDelete";
    String MSG_STUDENT_TIMETABLE_QUERY = "studentTimetableQuery";
    String MSG_TEACHER_COURSE_ENROLLMENTS_QUERY = "teacherCourseEnrollmentsQuery";
    String MSG_TEACHING_CLASS_QUERY = "teachingClassQuery";
    String MSG_TEACHING_CLASS_ADD = "teachingClassAdd";
    String MSG_TEACHING_CLASS_UPDATE = "teachingClassUpdate";
    String MSG_TEACHING_CLASS_DELETE = "teachingClassDelete";
    String MSG_COURSE_REQUIREMENT_GROUP_QUERY = "courseRequirementGroupQuery";
    String MSG_COURSE_AUTO_SCHEDULE_PREVIEW = "courseAutoSchedulePreview";
    String MSG_COURSE_AUTO_SCHEDULE_APPLY = "courseAutoScheduleApply";
    String MSG_COURSE_AUTO_SCHEDULE_VALIDATE = "courseAutoScheduleValidate";
    String MSG_COURSE_AUTO_SCHEDULE_DEMO_LOAD = "courseAutoScheduleDemoLoad";
    String MSG_COURSE_DASHBOARD_QUERY = "courseDashboardQuery";
    String MSG_COURSE_SCORE_STUDENT_QUERY = "courseScoreStudentQuery";
    String MSG_COURSE_SCORE_TEACHER_QUERY = "courseScoreTeacherQuery";
    String MSG_COURSE_SCORE_SUBMIT = "courseScoreSubmit";
    String MSG_COURSE_SCORE_PENDING_QUERY = "courseScorePendingQuery";
    String MSG_COURSE_SCORE_REVIEW = "courseScoreReview";

    // ---------- 虚拟商店模块（store）消息名与状态码 ----------

    /** 状态码：商品不存在。 */
    String STATUS_GOODS_NOT_FOUND = "404";

    /** 状态码：库存不足。 */
    String STATUS_STOCK_NOT_ENOUGH = "409";

    /** 状态码：业务冲突（如商品编号重复、存在购买记录禁止删除）。 */
    String STATUS_CONFLICT = "409";

    /** 状态码：余额不足。 */
    String STATUS_BALANCE_NOT_ENOUGH = "409";

    /** 消息名：查询商品（按关键字/类别筛选）。 */
    String MSG_SHOP_QUERY_GOODS = "shopQueryGoods";

    /** 消息名：购买商品。 */
    String MSG_SHOP_PURCHASE = "shopPurchase";

    /** 消息名：查询购买记录（按购买人）。 */
    String MSG_SHOP_QUERY_RECORDS = "shopQueryRecords";

    /** 消息名：新增商品（管理员）。 */
    String MSG_SHOP_ADD_GOODS = "shopAddGoods";

    /** 消息名：修改商品（管理员）。 */
    String MSG_SHOP_UPDATE_GOODS = "shopUpdateGoods";

    /** 消息名：删除商品（管理员）。 */
    String MSG_SHOP_DELETE_GOODS = "shopDeleteGoods";

    /** 消息名：查询校园卡余额。 */
    String MSG_SHOP_QUERY_BALANCE = "shopQueryBalance";

    /** 消息名：校园卡充值。 */
    String MSG_SHOP_RECHARGE = "shopRecharge";

    /** 消息名：购物车结算（一次提交多个商品，服务器生成一个含多条明细的订单）。 */
    String MSG_SHOP_CHECKOUT = "shopCheckout";

    /** 消息名：查询订单（含订单明细，普通用户查本人，管理员查全部）。 */
    String MSG_SHOP_QUERY_ORDERS = "shopQueryOrders";

    /** 消息名：查询全部促销活动（管理员配置"每日特价"用）。 */
    String MSG_SHOP_QUERY_PROMOTIONS = "shopQueryPromotions";

    /** 消息名：新增促销活动（管理员）。 */
    String MSG_SHOP_ADD_PROMOTION = "shopAddPromotion";

    /** 消息名：修改促销活动（管理员）。 */
    String MSG_SHOP_UPDATE_PROMOTION = "shopUpdatePromotion";

    /** 消息名：删除促销活动（管理员）。 */
    String MSG_SHOP_DELETE_PROMOTION = "shopDeletePromotion";
    // ========== 医院模块新增常量 ==========
    /** 查询全部医生 */
    String MSG_HOSPITAL_QUERY_ALL_DOCTOR = "hospital_query_all_doctor";
    /** 根据科室查询医生 */
    String MSG_HOSPITAL_QUERY_DOCTOR_BY_DEPT = "hospital_query_doctor_by_dept";
    /** 新增挂号预约 */
    String MSG_HOSPITAL_ADD_APPOINTMENT = "hospital_add_appointment";
    /** 查询个人预约记录 */
    String MSG_HOSPITAL_QUERY_MY_APPOINTMENT = "hospital_query_my_appointment";
    /** 取消预约 */
    String MSG_HOSPITAL_CANCEL_APPOINTMENT = "hospital_cancel_appointment";
    //管理员医院模块消息
    String MSG_HOSPITAL_QUERY_ALL_APPOINTMENT = "HOSPITAL_QUERY_ALL_APPOINTMENT";
    String MSG_HOSPITAL_ADD_DOCTOR = "HOSPITAL_ADD_DOCTOR";
    String MSG_HOSPITAL_UPDATE_DOCTOR = "HOSPITAL_UPDATE_DOCTOR";
    String MSG_HOSPITAL_DELETE_DOCTOR = "HOSPITAL_DELETE_DOCTOR";
    String MSG_HOSPITAL_QUERY_CAN_DELETE_DOCTOR = "HOSPITAL_QUERY_CAN_DELETE_DOCTOR";
    // 新增：删除【已取消】预约记录
    String MSG_HOSPITAL_DELETE_CANCEL_APPOINT = "HOSPITAL_DELETE_CANCEL_APPOINT";
    //==================== 健康教育模块 ====================
    String MSG_HOSPITAL_QUERY_HEALTH_ARTICLE = "hospital_query_health_article";
    //=====就诊叫号（医生查看待就诊、完成就诊）=====
    String MSG_HOSPITAL_DOCTOR_GET_MY_PENDING_APPOINT = "hospital_doctor_get_my_pending_appoint";
    String MSG_HOSPITAL_FINISH_APPOINT = "hospital_finish_appoint";
    //====药房开药处方模块====
String MSG_HOSPITAL_QUERY_ALL_MEDICINE = "hospital_query_all_medicine";
String MSG_HOSPITAL_SAVE_PRESCRIPTION = "hospital_save_prescription";
String MSG_HOSPITAL_QUERY_PRES_BY_APPOINT = "hospital_query_pres_by_appoint";
//====药房取药功能====
String MSG_HOSPITAL_USER_QUERY_MY_PRES = "hospital_user_query_my_pres";
String MSG_HOSPITAL_TAKE_MEDICINE = "hospital_take_medicine";
//药房内存模拟充值支付（内存余额，不修改数据库表）
String MSG_HOSPITAL_GET_MEM_BALANCE="hospital_get_mem_balance";
String MSG_HOSPITAL_MEM_RECHARGE="hospital_mem_recharge";
String MSG_HOSPITAL_MEM_PAY_PRES="hospital_mem_pay_pres";
//管理员药品库存管理
String MSG_HOSPITAL_ADMIN_QUERY_ALL_MED="hospital_admin_query_all_med";
String MSG_HOSPITAL_ADMIN_UPDATE_STOCK="hospital_admin_update_stock";
String MSG_HOSPITAL_ADMIN_ADD_MED="hospital_admin_add_med";
String MSG_HOSPITAL_ADMIN_UPDATE_MED="hospital_admin_update_med";
String MSG_HOSPITAL_ADMIN_DELETE_MED="hospital_admin_delete_med";



    // 医院业务自定义状态码
    /** 预约记录不存在 */
    String STATUS_APPOINT_NOT_FOUND = "601";
    /** 预约取消失败 */
    String STATUS_APPOINT_CANCEL_FAIL = "602";

    // ========== AI 问答模块消息类型 ==========
    /** 消息名：向 AI 提问，请求 data 为问题文本（String），成功响应 data 为回答文本。 */
    String MSG_AI_ASK = "aiAsk";
}
