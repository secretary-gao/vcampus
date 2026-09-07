package vcampus.client.biz;

import vcampus.common.constant.IConstant;
import vcampus.common.vo.Message;
import vcampus.common.vo.User;

/**
 * 回归测试：通过 Socket 提交超长登录 ID 时，服务器应返回参数错误，而不是数据库异常。
 */
public class UserClientSrvValidationTest {

    public static void main(String[] args) throws Exception {
        User user = new User();
        user.setUId("1234567890");
        user.setUPwd("01234567890123456789012345678901");
        user.setURole("学生");

        Message response = new UserClientSrv().register(user);
        if (!IConstant.STATUS_BAD_REQUEST.equals(response.getStatusCode())) {
            throw new AssertionError("期望400参数错误，实际为 "
                    + response.getStatusCode() + ": " + response.getData());
        }
        System.out.println("SOCKET_VALIDATION_OK: " + response.getData());
    }
}
