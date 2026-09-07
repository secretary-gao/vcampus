/*
 * CourseFrame
 *
 * Version 1.0
 *
 * 2026-09-04
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.client.view.course;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import vcampus.common.vo.User;

import java.util.Map;

/** Course JavaFX 独立演示入口，也用于不经过 MainFrame 的界面启动验证。 */
public class CourseFrame extends Application {

    /** {@inheritDoc} */
    @Override
    public void start(Stage stage) {
        Map<String, String> named = getParameters().getNamed();
        String role = named.getOrDefault("role", "管理员");
        String userId = named.getOrDefault("user", "ADMIN001");
        String userName = named.getOrDefault("name", "Course 演示");
        User user = new User(userId, userName, 20, "男", null, role);
        stage.setTitle("VCampus 教务选课中心");
        stage.setScene(new Scene(new CoursePanel(user), 1100, 720));
        stage.setMinWidth(1000);
        stage.setMinHeight(680);
        stage.show();
    }

    /**
     * 独立演示入口。可传 {@code --role=管理员}，或传已关联学籍的
     * {@code --role=学生 --user=登录ID}，教师可追加 {@code --name=教师姓名}。
     *
     * @param args JavaFX 命令行参数
     */
    public static void main(String[] args) {
        launch(args);
    }
}
