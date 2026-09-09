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

    /** 状态码：注册时用户名已存在。 */
    String STATUS_USER_EXISTS = "409";
    /** 状态码：服务器内部异常。 */
    String STATUS_ERROR = "500";

    /** 状态码：账号已被管理员禁用。 */
    String STATUS_ACCOUNT_DISABLED = "403";

    /** 状态码：权限不足（如非管理员尝试执行管理员专属操作）。 */
    String STATUS_FORBIDDEN = "410";

    // ========= 用户模块消息 =========
    /** 消息名：登录请求，对应 {@link vcampus.common.vo.Message#getName()}。 */
    String MSG_LOGIN = "login";
    /** 消息名：注册请求，对应 {@link vcampus.common.vo.Message#getName()}。 */
    String MSG_REGISTER = "register";
    /** 消息名：登出请求，对应 {@link vcampus.common.vo.Message#getName()}。 */
    String MSG_LOGOUT = "logout";

    /**
     * 消息名：管理员禁用/启用账号请求，对应说明书"管理员可注销/禁用账号"。
     * 请求 data 约定为 {@code Object[]{operatorUId, targetUId, newStatus}}。
     */
    String MSG_USER_SET_STATUS = "userSetStatus";

    // ========== 图书馆模块消息类型 ==========
    String MSG_QUERY_BOOKS = "queryBooks";
    String MSG_BORROW_BOOK = "borrowBook";
    String MSG_RETURN_BOOK = "returnBook";
    String MSG_GET_BORROW_RECORDS = "getBorrowRecords";
    String MSG_ADD_BOOK = "addBook";
    String MSG_UPDATE_BOOK = "updateBook";
    String MSG_DELETE_BOOK = "deleteBook";

    // ---------- 虚拟商店模块（store）消息名与状态码 ----------

    /** 状态码：商品不存在。 */
    String STATUS_GOODS_NOT_FOUND = "404";

    /** 状态码：库存不足。 */
    String STATUS_STOCK_NOT_ENOUGH = "409";

    /** 状态码：业务冲突（如商品编号重复、存在购买记录禁止删除）。 */
    String STATUS_CONFLICT = "409";

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
}
