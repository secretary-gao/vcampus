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
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * 客户端登录/注册窗口。用户填写登录ID、密码、角色后，点击"登录"或"注册"
 * 按钮，通过 {@link UserClientSrv} 把请求发给服务器，并根据响应弹窗提示
 * 成功或失败。界面参照东南大学"身份认证中心"统一登录页的视觉风格：
 * 居中的单张白色卡片 + 校徽 + 简洁表单。
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
     * 搭建窗口上的各个控件与布局：浅色渐变背景上居中放置一张登录卡片。
     */
    private void buildUi(Stage stage) {
        StackPane root = new StackPane();
        root.setBackground(new Background(new BackgroundFill(
                new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                        new Stop(0, Color.web("#eef3f8")),
                        new Stop(1, Color.web("#dde8f4"))),
                CornerRadii.EMPTY, Insets.EMPTY)));
        root.getChildren().add(buildLoginCard());

        Scene scene = new Scene(root, 1200, 780);
        stage.setTitle("东南大学 Vcampus 身份认证中心");
        stage.setResizable(false);
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.setOnCloseRequest(e -> Platform.exit());
    }

    /**
     * 构建居中的登录卡片：校徽品牌区 + 表单区 + 操作按钮。
     *
     * @return 登录卡片
     */
    private VBox buildLoginCard() {
        VBox card = new VBox(22);
        card.setMaxWidth(440);
        card.setPrefWidth(440);
        card.setPadding(new Insets(42, 44, 40, 44));
        card.setAlignment(Pos.TOP_CENTER);
        card.setStyle("-fx-background-color: white;"
                + "-fx-background-radius: 22;"
                + "-fx-effect: dropshadow(gaussian, rgba(15,40,70,0.18), 34, 0.18, 0, 14);");

        card.getChildren().addAll(buildHeader(), buildForm(), buildOptionRow(), buildButtonBlock());
        return card;
    }

    /**
     * 构建头部：校徽 + 校名 + "身份认证中心" + 副标题，全部居中，
     * 呼应真实统一登录页"校徽居中、标题居中"的样式。
     *
     * @return 头部面板
     */
    private VBox buildHeader() {
        StackPane emblem = SeuEmblem.build(76);

        Label school = new Label("东南大学");
        school.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 23));
        school.setTextFill(Color.web("#13161c"));

        Label english = new Label("SOUTHEAST UNIVERSITY");
        english.setFont(Font.font("System", FontWeight.BOLD, 11));
        english.setTextFill(Color.web("#98a3b1"));

        VBox brandText = new VBox(2, school, english);
        brandText.setAlignment(Pos.CENTER);

        VBox brandBlock = new VBox(10, emblem, brandText);
        brandBlock.setAlignment(Pos.CENTER);

        Label center = new Label("身份认证中心");
        center.setFont(Font.font("System", FontWeight.BOLD, 21));
        center.setTextFill(Color.web("#1d2b39"));

        Label desc = new Label("欢迎登录 Vcampus 系列作品 · 统一管理校园服务入口");
        desc.setTextFill(Color.web("#8b96a4"));
        desc.setFont(Font.font("System", 12.5));

        VBox header = new VBox(6, brandBlock, center, desc);
        header.setAlignment(Pos.CENTER);
        VBox.setMargin(center, new Insets(14, 0, 0, 0));
        return header;
    }

    /**
     * 构建表单区：登录ID、密码、角色三个纵向排列的字段，宽度撑满卡片。
     *
     * @return 表单面板
     */
    private VBox buildForm() {
        _uidField.setPromptText("请输入学号 / 工号");
        _pwdField.setPromptText("请输入密码");
        _uidField.setPrefHeight(44);
        _pwdField.setPrefHeight(44);
        _uidField.setMaxWidth(Double.MAX_VALUE);
        _pwdField.setMaxWidth(Double.MAX_VALUE);
        _uidField.setStyle(fieldStyle());
        _pwdField.setStyle(fieldStyle());

        _roleBox.getItems().setAll("学生", "教师", "管理员");
        _roleBox.getSelectionModel().selectFirst();
        _roleBox.setPrefHeight(44);
        _roleBox.setMaxWidth(Double.MAX_VALUE);
        _roleBox.setStyle(fieldStyle());

        VBox form = new VBox(6,
                fieldLabel("登录ID"), _uidField,
                fieldLabel("密码"), _pwdField,
                fieldLabel("角色"), _roleBox);
        form.setFillWidth(true);
        VBox.setMargin(_uidField, new Insets(0, 0, 8, 0));
        VBox.setMargin(_pwdField, new Insets(0, 0, 8, 0));
        return form;
    }

    /**
     * 表单字段的小标题标签。
     *
     * @param text 标签文字
     * @return 标签控件
     */
    private Label fieldLabel(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("System", FontWeight.BOLD, 12.5));
        label.setTextFill(Color.web("#5a6472"));
        return label;
    }

    /**
     * 构建"记住密码"选项行。
     *
     * @return 选项行
     */
    private HBox buildOptionRow() {
        _rememberBox.setTextFill(Color.web("#607080"));
        _rememberBox.setFont(Font.font("System", 12.5));

        HBox optionBar = new HBox(_rememberBox);
        optionBar.setAlignment(Pos.CENTER_LEFT);
        return optionBar;
    }

    /**
     * 构建底部操作区：整宽的绿色"登录"主按钮 + "还没有账号？立即注册"链接行。
     *
     * @return 按钮区
     */
    private VBox buildButtonBlock() {
        Button loginButton = new Button("登  录");
        loginButton.setOnAction(e -> onLogin());
        loginButton.setMaxWidth(Double.MAX_VALUE);
        loginButton.setPrefHeight(46);
        loginButton.setStyle("-fx-background-color: linear-gradient(to right, #3fa34d, #5bc46d);"
                + " -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;"
                + " -fx-background-radius: 23; -fx-cursor: hand;");

        Label registerTip = new Label("还没有账号？");
        registerTip.setTextFill(Color.web("#8b96a4"));
        registerTip.setFont(Font.font("System", 12.5));

        Button registerLink = new Button("立即注册");
        registerLink.setOnAction(e -> onRegister());
        registerLink.setStyle("-fx-background-color: transparent; -fx-text-fill: #2d6a9f;"
                + " -fx-font-size: 12.5px; -fx-font-weight: bold; -fx-underline: true;"
                + " -fx-cursor: hand; -fx-padding: 0;");

        HBox registerRow = new HBox(4, registerTip, registerLink);
        registerRow.setAlignment(Pos.CENTER);

        VBox block = new VBox(14, loginButton, registerRow);
        block.setAlignment(Pos.CENTER);
        return block;
    }

    /**
     * 输入框统一样式。
     *
     * @return CSS 样式
     */
    private String fieldStyle() {
        return "-fx-background-radius: 12;"
                + "-fx-border-radius: 12;"
                + "-fx-border-color: #e3e9ef;"
                + "-fx-border-width: 1;"
                + "-fx-background-color: #f8fafc;"
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
        if (uid.length() != 8) {
            showAlert(Alert.AlertType.WARNING, "提示", "登录ID必须为8位");
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
