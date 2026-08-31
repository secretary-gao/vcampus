/*
 * IHospitalClientSrv
 *
 * Version 1.0
 *
 * 2026-08-31
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.client.biz;

import vcampus.common.vo.Appointment;
import vcampus.common.vo.Doctor;
import vcampus.common.vo.Message;

import java.io.IOException;

/**
 * 客户端医院业务服务接口，对应共享说明书医院模块 IHospitalClientSrv。
 * 负责把界面层（view）的操作封装成网络请求发给服务器，并把服务器
 * 的响应原样返回给界面层解析（成功/失败、具体数据都在响应 {@link Message} 中）。
 */
public interface IHospitalClientSrv {

    /**
     * 查询全部医生
     * @return 服务器返回的响应消息
     * @throws IOException 网络连接异常（如服务器未启动）
     * @throws ClassNotFoundException 反序列化响应对象失败
     */
    Message queryAllDoctor() throws IOException, ClassNotFoundException;

    /**
     * 根据科室查询医生列表
     * @param department 科室名称
     * @return 服务器返回的响应消息
     * @throws IOException 网络连接异常（如服务器未启动）
     * @throws ClassNotFoundException 反序列化响应对象失败
     */
    Message queryDoctorByDept(String department) throws IOException, ClassNotFoundException;

    /**
     * 创建挂号预约
     * @param appoint 预约对象
     * @return 服务器返回的响应消息
     * @throws IOException 网络连接异常（如服务器未启动）
     * @throws ClassNotFoundException 反序列化响应对象失败
     */
    Message addAppointment(Appointment appoint) throws IOException, ClassNotFoundException;

    /**
     * 查询当前用户的预约记录
     * @param userId 用户id
     * @return 服务器返回的响应消息
     * @throws IOException 网络连接异常（如服务器未启动）
     * @throws ClassNotFoundException 反序列化响应对象失败
     */
    Message queryMyAppointment(String userId) throws IOException, ClassNotFoundException;

    /**
     * 取消预约
     * @param appointId 预约id
     * @return 服务器返回的响应消息
     * @throws IOException 网络连接异常（如服务器未启动）
     * @throws ClassNotFoundException 反序列化响应对象失败
     */
    Message cancelAppointment(String appointId) throws IOException, ClassNotFoundException;
}
