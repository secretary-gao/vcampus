/*
 * DoctorDAOTest
 *
 * Version 1.0
 *
 * 2026-08-31
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.server.dao;

import vcampus.common.vo.Doctor;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * {@link DoctorDAO} 的功能验证程序：测试查询全部医生、按科室查询医生。
 *
 * <p>运行前请确认：已执行医院模块sql脚本；已正确
 * 配置 {@code Server/db.properties}；运行时的工作目录为项目根目录。</p>
 */
public class DoctorDAOTest {
    /**
     * 程序入口：测试医生DAO查询能力
     *
     * @param args 命令行参数（未使用）
     */
    public static void main(String[] args) {
        DoctorDAO dao = new DoctorDAO();
        try {
            // 测试1：查询全部医生
            List<Doctor> allDoctor = dao.selectAll();
            if(allDoctor == null || allDoctor.isEmpty()){
                System.out.println("【DoctorDAO】全部医生列表为空，请检查数据库是否初始化医生数据");
            }else{
                System.out.println("【DoctorDAO】查询全部医生成功，共"+allDoctor.size()+"条记录");
                for (Doctor d : allDoctor) {
                    System.out.println("\t"+d);
                }
            }

            // 测试2：按科室查询，替换成你数据库真实存在的科室名称
            String testDept = "内科";
            List<Doctor> deptDoctorList = dao.selectByDepartment(testDept);
            System.out.println("\n【DoctorDAO】查询科室["+testDept+"]医生，共"+deptDoctorList.size()+"条");
            for (Doctor d : deptDoctorList) {
                System.out.println("\t"+d);
            }

        } catch (SQLException | IOException e) {
            System.err.println("DoctorDAO数据层测试失败：" + e.getMessage());
            System.err.println("请检查：1) MySQL 服务是否启动；2) Server/db.properties 是否已正确填写；"
                    + "3) 是否已执行医院模块建表&初始化sql。");
            e.printStackTrace();
        }
    }
}
