/*
 * DoctorDAOTest
 *
 * Version 1.0
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.server.dao;
import vcampus.common.vo.Doctor;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * DoctorDAO完整测试：查询全部、按科室查询、新增、findById查询、修改、删除医生
 */
public class DoctorDAOTest {
    public static void main(String[] args) {
        DoctorDAO dao = new DoctorDAO();
        String testDocId = "D9999999";
        try {
            System.out.println("========== 测试1：查询全部医生 selectAll ==========");
            List<Doctor> allDoctor = dao.selectAll();
            if(allDoctor == null || allDoctor.isEmpty()){
                System.out.println("⚠️【DoctorDAO】全部医生列表为空，请检查数据库是否初始化医生数据");
            }else{
                System.out.println("✅【DoctorDAO】查询全部医生成功，共"+allDoctor.size()+"条记录");
                for (Doctor d : allDoctor) {
                    System.out.println("\t"+d);
                }
            }

            System.out.println("\n========== 测试2：按科室查询 selectByDepartment ==========");
            String testDept = "内科";
            List<Doctor> deptDoctorList = dao.selectByDepartment(testDept);
            System.out.println("✅【DoctorDAO】查询科室["+testDept+"]医生，共"+deptDoctorList.size()+"条");
            for (Doctor d : deptDoctorList) {
                System.out.println("\t"+d);
            }

            System.out.println("\n========== 测试3：新增医生 insert ==========");
            Doctor insertDoc = new Doctor(testDocId,"测试医生A","外科","副主任医师");
            boolean insertOk = dao.insert(insertDoc);
            System.out.println(insertOk ? "✅新增医生成功" : "❌新增医生失败");

            System.out.println("\n========== 测试4：根据ID查询医生 findById ==========");
            Doctor findDoc = dao.findById(testDocId);
            if(findDoc != null){
                System.out.println("✅查询医生成功："+findDoc);
            }else{
                System.out.println("❌查询医生失败");
            }

            System.out.println("\n========== 测试5：修改医生信息 update ==========");
            findDoc.setName("测试医生A_修改后");
            findDoc.setDepartment("骨科");
            findDoc.setTitle("主任医师");
            boolean updateOk = dao.update(findDoc);
            System.out.println(updateOk ? "✅修改医生成功" : "❌修改医生失败");
            Doctor afterUpdate = dao.findById(testDocId);
            System.out.println("修改后数据："+afterUpdate);

            System.out.println("\n========== 测试6：删除医生 delete ==========");
            boolean delOk = dao.delete(testDocId);
            System.out.println(delOk ? "✅删除医生成功" : "❌删除医生失败");
            Doctor afterDel = dao.findById(testDocId);
            if(afterDel == null){
                System.out.println("✅验证：删除后查不到该医生");
            }else{
                System.out.println("❌验证失败：仍然可以查到已删除医生");
            }

        } catch (SQLException | IOException e) {
            System.err.println("\nDoctorDAO数据层测试失败：" + e.getMessage());
            System.err.println("请检查：1) MySQL 服务是否启动；2) Server/db.properties 是否已正确填写；"
                    + "3) 是否已执行医院模块建表&初始化sql。");
            e.printStackTrace();
        }
    }
}
