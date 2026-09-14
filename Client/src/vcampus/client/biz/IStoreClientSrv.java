/*
 * IStoreClientSrv
 *
 * Version 1.0
 *
 * 2026-09-01
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.client.biz;

import vcampus.common.vo.Goods;
import vcampus.common.vo.Message;

import java.io.IOException;
import java.math.BigDecimal;

/**
 * 客户端商店业务服务接口，对应共享说明书中"虚拟商店模块"的 IStoreClientSrv。
 * 负责把界面层（{@code view}）的操作封装成网络请求发给服务器，并把服务器
 * 的响应原样返回给界面层解析（成功/失败、具体数据都在响应 {@link Message} 中）。
 */
public interface IStoreClientSrv {

    /**
     * 发起"查询商品"请求。
     *
     * @param keyword  名称/类别关键字，可为 {@code null}（不筛选）
     * @param category 类别，可为 {@code null}（不筛选）
     * @return 服务器返回的响应消息
     * @throws IOException            网络连接异常（如服务器未启动）
     * @throws ClassNotFoundException 反序列化响应对象失败
     */
    Message queryGoods(String keyword, String category) throws IOException, ClassNotFoundException;

    /**
     * 发起"购买商品"请求。
     *
     * @param userId   购买人ID
     * @param goodsId  商品编号
     * @param quantity 购买数量
     * @return 服务器返回的响应消息
     * @throws IOException            网络连接异常（如服务器未启动）
     * @throws ClassNotFoundException 反序列化响应对象失败
     */
    Message purchaseGoods(String userId, String goodsId, int quantity) throws IOException, ClassNotFoundException;

    /**
     * 发起"查询购买记录"请求。
     *
     * @param userId 购买人ID；可为 {@code null} 或空串（管理员查询全部）
     * @return 服务器返回的响应消息
     * @throws IOException            网络连接异常（如服务器未启动）
     * @throws ClassNotFoundException 反序列化响应对象失败
     */
    Message queryPurchaseRecords(String userId) throws IOException, ClassNotFoundException;

    /**
     * 发起"新增商品"请求（管理员）。
     *
     * @param goods 待新增的商品对象
     * @return 服务器返回的响应消息
     * @throws IOException            网络连接异常（如服务器未启动）
     * @throws ClassNotFoundException 反序列化响应对象失败
     */
    Message addGoods(Goods goods) throws IOException, ClassNotFoundException;

    /**
     * 发起"修改商品"请求（管理员）。
     *
     * @param goods 待修改的商品对象
     * @return 服务器返回的响应消息
     * @throws IOException            网络连接异常（如服务器未启动）
     * @throws ClassNotFoundException 反序列化响应对象失败
     */
    Message updateGoods(Goods goods) throws IOException, ClassNotFoundException;

    /**
     * 发起"删除商品"请求（管理员）。
     *
     * @param goodsId 商品编号
     * @return 服务器返回的响应消息
     * @throws IOException            网络连接异常（如服务器未启动）
     * @throws ClassNotFoundException 反序列化响应对象失败
     */
    Message deleteGoods(String goodsId) throws IOException, ClassNotFoundException;

    /**
     * 发起"查询校园卡余额"请求。
     *
     * @param userId 用户ID
     * @return 服务器返回的响应消息（成功时 data 为余额 BigDecimal）
     * @throws IOException            网络连接异常（如服务器未启动）
     * @throws ClassNotFoundException 反序列化响应对象失败
     */
    Message queryBalance(String userId) throws IOException, ClassNotFoundException;

    /**
     * 发起"校园卡充值"请求。
     *
     * @param userId 用户ID
     * @param amount 充值金额（正数）
     * @return 服务器返回的响应消息（成功时 data 为充值后的余额）
     * @throws IOException            网络连接异常（如服务器未启动）
     * @throws ClassNotFoundException 反序列化响应对象失败
     */
    Message recharge(String userId, BigDecimal amount) throws IOException, ClassNotFoundException;
}
