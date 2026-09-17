package vcampus.server.srv;

import vcampus.common.vo.StudentStatus;

/** 验证跨模块统一采用“仅在读学生可新增校园业务”的规则。 */
public class StudentCrossModuleStatusTest {

    public static void main(String[] args) {
        require(StudentStatusGuard.canUseStudentServices(StudentStatus.ENROLLED),
                "在读学生应可使用校园业务");
        require(!StudentStatusGuard.canUseStudentServices(StudentStatus.SUSPENDED),
                "休学学生不应新增校园业务");
        require(!StudentStatusGuard.canUseStudentServices(StudentStatus.GRADUATED),
                "毕业学生不应新增校园业务");
        require(!StudentStatusGuard.canUseStudentServices(StudentStatus.WITHDRAWN),
                "退学学生不应新增校园业务");
        System.out.println("STUDENT_CROSS_MODULE_STATUS_TEST=PASS");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException("FAIL: " + message);
    }
}
