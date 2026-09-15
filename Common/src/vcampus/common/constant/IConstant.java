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
    /** 服务器地址（本机测试时用回环地址）。 */
    String SERVER_HOST = "127.0.0.1";
    /** 服务器监听端口。 */
    int SERVER_PORT = 8888;

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
    String MSG_COURSE_DASHBOARD_QUERY = "courseDashboardQuery";

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

    // 医院业务自定义状态码
    /** 预约记录不存在 */
    String STATUS_APPOINT_NOT_FOUND = "601";
    /** 预约取消失败 */
    String STATUS_APPOINT_CANCEL_FAIL = "602";

    // ========== AI 问答模块消息类型 ==========
    /** 消息名：向 AI 提问，请求 data 为问题文本（String），成功响应 data 为回答文本。 */
    String MSG_AI_ASK = "aiAsk";
}
