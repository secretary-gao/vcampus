package vcampus.client.biz;

import vcampus.common.constant.IConstant;
import vcampus.common.util.MD5Util;
import vcampus.common.vo.Message;
import vcampus.common.vo.Student;
import vcampus.common.vo.User;

/** 无界面验证登录用户能够按自身角色访问学籍模块。 */
public class StudentClientSrvTest {

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("用法：StudentClientSrvTest <登录ID> <密码>");
            return;
        }

        try {
            User loginUser = new User();
            loginUser.setUId(args[0]);
            loginUser.setUPwd(MD5Util.md5(args[1]));

            Message loginResponse = new UserClientSrv().login(loginUser);
            if (!IConstant.STATUS_SUCCESS.equals(loginResponse.getStatusCode())) {
                System.err.println("登录失败：" + loginResponse.getData());
                return;
            }

            User currentUser = (User) loginResponse.getData();
            StudentClientSrv clientSrv = new StudentClientSrv(currentUser);
            if (currentUser.isStudent()) {
                Student student = clientSrv.getMyStudentInfo();
                System.out.println(student == null ? "当前账号尚未绑定学籍" : student);
            } else {
                System.out.println("当前角色可查看学生数：" + clientSrv.findAll().size());
            }
        } catch (Exception exception) {
            System.err.println("学生客户端测试失败：" + exception.getMessage());
            exception.printStackTrace();
        }
    }
}
