package vcampus.server.srv;

import vcampus.common.vo.User;

/**
 * 回归测试：超长登录 ID 应在进入 DAO 前被拒绝。
 */
public class UserServerSrvValidationTest {

    public static void main(String[] args) throws Exception {
        User user = new User();
        user.setUId("1234567890");
        user.setUPwd("01234567890123456789012345678901");
        user.setURole("学生");

        try {
            new UserServerSrv().register(user);
            throw new AssertionError("超长登录 ID 未被拒绝");
        } catch (IllegalArgumentException expected) {
            System.out.println("VALIDATION_OK: " + expected.getMessage());
        }
    }
}
