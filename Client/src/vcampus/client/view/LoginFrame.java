/*
 * LoginFrame
 *
 * Version 1.0
 *
 * 2026-08-30
 *
 * Copyright (c) 2026 Vcampus Team
*/
package vcampus.client.view;

import vcampus.client.biz.IUserClientSrv;
import vcampus.client.biz.UserClientSrv;
import vcampus.common.constant.IConstant;
import vcampus.common.util.MD5Util;
import vcampus.common.vo.Message;
import vcampus.common.vo.User;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * 客户端登录/注册窗口。用户填写登录ID、密码、角色后，点击"登录"或"注册"
 * 按钮，通过 {@link UserClientSrv} 把请求发给服务器，并根据响应弹窗提示
 * 成功或失败。这是用户管理模块本周要跑通的完整链路的界面入口。
 */
public class LoginFrame extends Application {

    /** 当前窗口的舞台。 */
    private Stage _stage;

    /** 登录ID输入框。 */
    private final TextField _uidField = new TextField();

    /** 密码输入框。 */
    private final PasswordField _pwdField = new PasswordField();

    /** 角色选择框。 */
    private final ComboBox<String> _roleBox = new ComboBox<>();

    /** 记住密码复选框。 */
    private final CheckBox _rememberBox = new CheckBox("记住密码");

    /** 客户端用户业务服务，负责实际的 Socket 通信。 */
    private final IUserClientSrv _userClientSrv = new UserClientSrv();

    /**
     * 构造方法：创建窗口并搭建界面。
     */
    public LoginFrame() {
    }

    /**
     * 搭建窗口上的各个控件与布局。
     */
    private void buildUi(Stage stage) {
        BorderPane root = new BorderPane();
        root.setBackground(new Background(new BackgroundFill(
                new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                        new Stop(0, Color.web("#f7f9fc")),
                        new Stop(1, Color.web("#e9f1fb"))),
                CornerRadii.EMPTY, Insets.EMPTY)));

        root.setLeft(buildBrandPanel());
        root.setCenter(buildLoginPanel());

        Scene scene = new Scene(root, 1200, 760);
        stage.setTitle("东南大学 Vcampus 身份认证中心");
        stage.setResizable(false);
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.setOnCloseRequest(e -> Platform.exit());
    }

    /**
     * 构建左侧品牌区。
     *
     * @return 品牌面板
     */
    private VBox buildBrandPanel() {
        VBox panel = new VBox(18);
        panel.setPrefWidth(360);
        panel.setPadding(new Insets(54, 34, 56, 48));
        panel.setAlignment(Pos.TOP_LEFT);
        panel.setStyle("-fx-background-color: rgba(255,255,255,0.96);"
                + "-fx-background-radius: 0 42 42 0;"
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.10), 24, 0.18, 2, 0);");

        StackPane emblem = new StackPane();
        emblem.setPrefSize(118, 118);

        Circle outer = new Circle(52, Color.web("#f3cf57"));
        outer.setStroke(Color.web("#9b7815"));
        outer.setStrokeWidth(3);

        Circle inner = new Circle(41, Color.web("#fff8db"));
        inner.setStroke(Color.web("#d7b243"));
        inner.setStrokeWidth(1.5);

        Rectangle tower = new Rectangle(16, 26, Color.web("#b98516"));
        tower.setArcWidth(2);
        tower.setArcHeight(2);
        tower.setTranslateY(4);

        Rectangle roof = new Rectangle(30, 8, Color.web("#b98516"));
        roof.setArcWidth(2);
        roof.setArcHeight(2);
        roof.setTranslateY(-12);

        Rectangle base = new Rectangle(44, 8, Color.web("#b98516"));
        base.setArcWidth(2);
        base.setArcHeight(2);
        base.setTranslateY(16);

        Line leftWing = new Line(-16, 2, -34, -10);
        leftWing.setStroke(Color.web("#b98516"));
        leftWing.setStrokeWidth(3);
        Line rightWing = new Line(16, 2, 34, -10);
        rightWing.setStroke(Color.web("#b98516"));
        rightWing.setStrokeWidth(3);

        Label sealText = new Label("东南");
        sealText.setTextFill(Color.web("#9b7815"));
        sealText.setFont(Font.font("System", FontWeight.BOLD, 18));
        sealText.setTranslateY(34);

        emblem.getChildren().addAll(outer, inner, roof, tower, base, leftWing, rightWing, sealText);

        VBox titleBlock = new VBox(2);
        Label school = new Label("东南大学");
        school.setFont(Font.font("KaiTi", FontWeight.BOLD, 30));
        school.setTextFill(Color.web("#151515"));

        Label english = new Label("SOUTHEAST UNIVERSITY");
        english.setFont(Font.font("System", FontWeight.BOLD, 12));
        english.setTextFill(Color.web("#7a8696"));

        titleBlock.getChildren().addAll(school, english);

        Label center = new Label("身份认证中心");
        center.setFont(Font.font("System", FontWeight.BOLD, 28));
        center.setTextFill(Color.web("#111111"));

        Label desc = new Label("欢迎登录 Vcampus 系列作品\n统一管理校园服务入口");
        desc.setTextFill(Color.web("#5a6472"));
        desc.setFont(Font.font("System", 15));
        desc.setLineSpacing(6);

        panel.getChildren().addAll(emblem, titleBlock, center, desc);
        return panel;
    }

    /**
     * 构建中间登录区。
     *
     * @return 登录面板
     */
    private StackPane buildLoginPanel() {
        StackPane wrapper = new StackPane();
        wrapper.setPadding(new Insets(36, 18, 36, 18));

        VBox card = new VBox(18);
        card.setMaxWidth(380);
        card.setPrefWidth(380);
        card.setPadding(new Insets(30, 28, 28, 28));
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle("-fx-background-color: rgba(255,255,255,0.98);"
                + "-fx-background-radius: 24;"
                + "-fx-border-radius: 24;"
                + "-fx-border-color: rgba(45,106,159,0.12);"
                + "-fx-effect: dropshadow(gaussian, rgba(28,54,84,0.12), 22, 0.12, 0, 8);");

        Label cardTitle = new Label("账号登录");
        cardTitle.setFont(Font.font("System", FontWeight.BOLD, 26));
        cardTitle.setTextFill(Color.web("#1d2b39"));

        Label cardTip = new Label("请输入学号/工号和密码");
        cardTip.setTextFill(Color.web("#738092"));
        cardTip.setFont(Font.font("System", 13));

        _uidField.setPromptText("请输入登录ID");
        _pwdField.setPromptText("请输入密码");
        _uidField.setPrefHeight(42);
        _pwdField.setPrefHeight(42);
        _uidField.setStyle(fieldStyle());
        _pwdField.setStyle(fieldStyle());

        _roleBox.getItems().setAll("学生", "管理员");
        _roleBox.getSelectionModel().selectFirst();
        _roleBox.setPrefHeight(42);
        _roleBox.setStyle(fieldStyle());

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(12);
        form.setMaxWidth(320);
        form.add(new Label("登录ID"), 0, 0);
        form.add(_uidField, 0, 1);
        form.add(new Label("密码"), 0, 2);
        form.add(_pwdField, 0, 3);
        form.add(new Label("角色"), 0, 4);
        form.add(_roleBox, 0, 5);

        _rememberBox.setTextFill(Color.web("#607080"));
        _rememberBox.setFont(Font.font("System", 13));

        HBox optionBar = new HBox(12, _rememberBox);
        optionBar.setAlignment(Pos.CENTER_LEFT);

        Button loginButton = new Button("登录");
        Button registerButton = new Button("注册");
        loginButton.setOnAction(e -> onLogin());
        registerButton.setOnAction(e -> onRegister());
        loginButton.setPrefWidth(130);
        registerButton.setPrefWidth(130);
        loginButton.setPrefHeight(44);
        registerButton.setPrefHeight(44);
        loginButton.setStyle("-fx-background-color: #73c553; -fx-text-fill: white;"
                + " -fx-font-size: 16px; -fx-font-weight: bold;"
                + " -fx-background-radius: 22; -fx-cursor: hand;");
        registerButton.setStyle("-fx-background-color: white; -fx-text-fill: #2d6a9f;"
                + " -fx-border-color: #2d6a9f; -fx-border-width: 1.2;"
                + " -fx-border-radius: 22; -fx-background-radius: 22;"
                + " -fx-font-size: 16px; -fx-font-weight: bold; -fx-cursor: hand;");

        HBox buttonBar = new HBox(12, loginButton, registerButton);
        buttonBar.setAlignment(Pos.CENTER);
        buttonBar.setMaxWidth(320);

        card.getChildren().addAll(cardTitle, cardTip, form, optionBar, buttonBar);
        wrapper.getChildren().add(card);
        return wrapper;
    }

    /**
     * 输入框统一样式。
     *
     * @return CSS 样式
     */
    private String fieldStyle() {
        return "-fx-background-radius: 18;"
                + "-fx-border-radius: 18;"
                + "-fx-border-color: #edf2f6;"
                + "-fx-border-width: 1;"
                + "-fx-background-color: #ffffff;"
                + "-fx-padding: 0 14 0 14;"
                + "-fx-font-size: 14px;";
    }

    /**
     * JavaFX 启动入口。
     *
     * @param stage 舞台
     */
    @Override
    public void start(Stage stage) {
        this._stage = stage;
        buildUi(stage);
        stage.show();
    }

    /**
     * 从界面收集输入并校验，密码在这里就完成 MD5 摘要（明文密码不会经网络传输）。
     *
     * @return 校验通过时返回封装好的 {@link User}；输入不合法时返回 {@code null}
     */
    private User collectInput() {
        String uid = _uidField.getText().trim();
        String pwd = _pwdField.getText();
        String role = _roleBox.getSelectionModel().getSelectedItem();

        if (uid.isEmpty() || pwd.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "提示", "登录ID和密码不能为空");
            return null;
        }

        User user = new User();
        user.setUId(uid);
        user.setUPwd(MD5Util.md5(pwd));
        user.setURole(role);
        return user;
    }

    /**
     * "登录"按钮的点击处理：发送登录请求并根据响应弹窗提示。
     */
    private void onLogin() {
        User loginUser = collectInput();
        if (loginUser == null) {
            return;
        }
        try {
            Message response = _userClientSrv.login(loginUser);
            if (IConstant.STATUS_SUCCESS.equals(response.getStatusCode())) {
                User found = (User) response.getData();
                showAlert(Alert.AlertType.INFORMATION, "登录成功", "登录成功，欢迎 " + found.getUId() + "！");
                openMainFrame(found);
            } else {
                showAlert(Alert.AlertType.ERROR, "登录失败", String.valueOf(response.getData()));
            }
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "连接失败", "无法连接服务器，请确认服务器已启动：" + e.getMessage());
        } catch (ClassNotFoundException e) {
            showAlert(Alert.AlertType.ERROR, "错误", "服务器返回的数据无法识别：" + e.getMessage());
        }
    }

    /**
     * "注册"按钮的点击处理：发送注册请求并根据响应弹窗提示。
     */
    private void onRegister() {
        User newUser = collectInput();
        if (newUser == null) {
            return;
        }
        try {
            Message response = _userClientSrv.register(newUser);
            if (IConstant.STATUS_SUCCESS.equals(response.getStatusCode())) {
                showAlert(Alert.AlertType.INFORMATION, "注册成功", String.valueOf(response.getData()));
            } else {
                showAlert(Alert.AlertType.ERROR, "注册失败", String.valueOf(response.getData()));
            }
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "连接失败", "无法连接服务器，请确认服务器已启动：" + e.getMessage());
        } catch (ClassNotFoundException e) {
            showAlert(Alert.AlertType.ERROR, "错误", "服务器返回的数据无法识别：" + e.getMessage());
        }
    }

    /**
     * 显示提示对话框。
     *
     * @param alertType 提示类型
     * @param title     标题
     * @param content   内容
     */
    private void showAlert(Alert.AlertType alertType, String title, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * 打开主界面。
     *
     * @param currentUser 当前登录用户
     */
    private void openMainFrame(User currentUser) {
        try {
            Stage mainStage = new Stage();
            new MainFrame(currentUser).start(mainStage);
            if (_stage != null) {
                _stage.close();
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "错误", "打开主界面失败：" + e.getMessage());
        }
    }

    /**
     * 客户端程序入口：在事件分派线程上创建并显示登录窗口。
     *
     * @param args 命令行参数（未使用）
     */
    public static void main(String[] args) {
        launch(args);
    }
}
