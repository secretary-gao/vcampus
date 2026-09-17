package vcampus.server.srv;

import vcampus.common.vo.StudentStatus;
import vcampus.common.vo.User;

/** 验证学籍终态与学生账号状态的联动规则。 */
public class StudentStatusAccountLinkageTest {

    public static void main(String[] args) {
        require(User.STATUS_DISABLED.equals(StudentServerSrv.accountStatusForTransition(
                StudentStatus.ENROLLED, StudentStatus.GRADUATED)), "毕业应禁用账号");
        require(User.STATUS_DISABLED.equals(StudentServerSrv.accountStatusForTransition(
                StudentStatus.SUSPENDED, StudentStatus.WITHDRAWN)), "退学应禁用账号");
        require(User.STATUS_NORMAL.equals(StudentServerSrv.accountStatusForTransition(
                StudentStatus.GRADUATED, StudentStatus.ENROLLED)), "恢复在读应恢复账号");
        require(StudentServerSrv.accountStatusForTransition(
                StudentStatus.ENROLLED, StudentStatus.SUSPENDED) == null,
                "休学不应覆盖账号状态");
        require(StudentServerSrv.accountStatusForTransition(
                StudentStatus.GRADUATED, StudentStatus.GRADUATED) == null,
                "普通终态编辑不应重复覆盖账号状态");
        System.out.println("STUDENT_STATUS_ACCOUNT_LINKAGE_TEST=PASS");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException("FAIL: " + message);
        }
    }
}
