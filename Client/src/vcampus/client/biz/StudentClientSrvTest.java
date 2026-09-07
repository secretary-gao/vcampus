package vcampus.client.biz;

/** 无界面验证学生客户端能够与独立学生服务器完成一次查询。 */
public class StudentClientSrvTest {

    public static void main(String[] args) {
        try {
            StudentClientSrv clientSrv = new StudentClientSrv();
            System.out.println("学生总数：" + clientSrv.findAll().size());
            System.out.println("不存在学号的查询结果："
                    + clientSrv.findByStudentId("not-exist"));
        } catch (Exception exception) {
            System.err.println("学生客户端测试失败：" + exception.getMessage());
            exception.printStackTrace();
        }
    }
}
