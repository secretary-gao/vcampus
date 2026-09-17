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
import javafx.util.Duration;

import java.io.IOException;

/**
 * 客户端登录/注册窗口。用户填写登录ID、密码后点击"登录"；教师和管理员
 * 可以通过注册入口创建账号，学生账号由管理员新增学籍时统一创建。
 * 按钮，通过 {@link UserClientSrv} 把请求发给服务器，并根据响应弹窗提示
 * 成功或失败。界面参照东南大学"身份认证中心"统一登录页的视觉风格：
 * 居中的单张白色卡片 + 校徽 + 简洁表单。
 */
public class LoginFrame extends Application {

    /** 当前窗口的舞台。 */
    private Stage _stage;

    /** 登录ID输入框。 */
    private final TextField _uidField = new TextField();

    /** 密码输入框（正常状态下显示的遮罩输入框）。 */
    private final PasswordField _pwdField = new PasswordField();

    /** 密码明文输入框，跟 {@link #_pwdField} 文本双向绑定，点"显示密码"
     *  按钮时临时切换显示这一个，几秒后自动切回遮罩状态。 */
    private final TextField _pwdPlainField = new TextField();

    /** 记住密码复选框。 */
    private final CheckBox _rememberBox = new CheckBox("记住密码");

    /** 本地记住的登录信息存放路径，放在用户主目录下，不会被提交进仓库
     *  （每台电脑各记各的，纯粹是本地便利功能，不是安全存储）。 */
    private static final java.io.File REMEMBER_FILE =
            new java.io.File(System.getProperty("user.home"), ".vcampus_login.properties");

    /** 客户端用户业务服务，负责实际的 Socket 通信。 */
    private final IUserClientSrv _userClientSrv = new UserClientSrv();

    /**
     * 构造方法：创建窗口并搭建界面。
     */
    public LoginFrame() {
    }

    /**
     * 搭建窗口上的各个控件与布局：铺满整个窗口的东南大学校园背景轮播图
     * （见 {@link CampusBackground}，找不到图片时自动退回浅色渐变），
     * 上面居中放置一张登录卡片。窗口默认最大化占满屏幕，参照真实统一
     * 身份认证页"全屏大图背景 + 居中登录框"的视觉效果。
     */
    private void buildUi(Stage stage) {
        StackPane root = new StackPane();
        root.setBackground(new Background(new BackgroundFill(
                new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                        new Stop(0, Color.web("#eef3f8")),
                        new Stop(1, Color.web("#dde8f4"))),
                CornerRadii.EMPTY, Insets.EMPTY)));
        root.getChildren().addAll(CampusBackground.build(), buildLoginCard());

        Scene scene = new Scene(root, 1200, 780);
        stage.setTitle("东南大学 Vcampus 身份认证中心");
        stage.setResizable(true);
        stage.setScene(scene);
        stage.setOnCloseRequest(e -> Platform.exit());
        // centerOnScreen() 会把窗口拉回非最大化的默认尺寸，所以最大化要放在
        // 最后设置，且不能再调用 centerOnScreen()，否则窗口会被重新变小。
        stage.setMaximized(true);
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
     * 构建表单区：登录ID、密码两个纵向排列的字段，宽度撑满卡片（角色选择
     * 已经挪到注册弹窗里——登录不需要选角色，服务器会按登录ID查库返回
     * 真实角色，客户端选什么都不影响登录结果）。
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

        VBox form = new VBox(6,
                fieldLabel("登录ID"), _uidField,
                fieldLabel("密码"), buildPasswordRow());
        form.setFillWidth(true);
        VBox.setMargin(_uidField, new Insets(0, 0, 8, 0));

        loadRememberedCredential();
        return form;
    }

    /**
     * 构建密码输入行：遮罩输入框叠加一个明文输入框（两者文本双向绑定），
     * 右侧一个"显示密码"按钮，点一下临时切到明文输入框，2秒后自动切
     * 回遮罩状态，对应"点击右侧短暂查看"的需求。
     *
     * @return 密码输入行
     */
    private HBox buildPasswordRow() {
        _pwdPlainField.textProperty().bindBidirectional(_pwdField.textProperty());
        _pwdPlainField.setPromptText("请输入密码");
        _pwdPlainField.setPrefHeight(44);
        _pwdPlainField.setMaxWidth(Double.MAX_VALUE);
        _pwdPlainField.setStyle(fieldStyle());
        _pwdPlainField.setManaged(false);
        _pwdPlainField.setVisible(false);

        StackPane pwdStack = new StackPane(_pwdField, _pwdPlainField);
        HBox.setHgrow(pwdStack, javafx.scene.layout.Priority.ALWAYS);

        Button toggleButton = new Button("显示");
        toggleButton.setPrefHeight(44);
        toggleButton.setStyle("-fx-background-color: #f8fafc; -fx-text-fill: #5a6472;"
                + " -fx-border-color: #e3e9ef; -fx-border-radius: 12; -fx-background-radius: 12;"
                + " -fx-font-size: 12.5px; -fx-cursor: hand;");

        javafx.animation.PauseTransition autoHide = new javafx.animation.PauseTransition(Duration.seconds(2));
        autoHide.setOnFinished(e -> {
            _pwdField.setManaged(true);
            _pwdField.setVisible(true);
            _pwdPlainField.setManaged(false);
            _pwdPlainField.setVisible(false);
            toggleButton.setText("显示");
        });

        toggleButton.setOnAction(e -> {
            _pwdField.setManaged(false);
            _pwdField.setVisible(false);
            _pwdPlainField.setManaged(true);
            _pwdPlainField.setVisible(true);
            toggleButton.setText("隐藏");
            autoHide.playFromStart();
        });

        HBox row = new HBox(8, pwdStack, toggleButton);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
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
     * 构建底部操作区：整宽的绿色"登录"主按钮 + 教师/管理员注册链接行。
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

        Label registerTip = new Label("教师/管理员账号？");
        registerTip.setTextFill(Color.web("#8b96a4"));
        registerTip.setFont(Font.font("System", 12.5));

        Button registerLink = new Button("注册");
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
        AppIcons.installGlobalIcon();
        this._stage = stage;
        buildUi(stage);
        stage.show();
    }

    /**
     * 从登录表单收集输入并校验，密码在这里就完成 MD5 摘要（明文密码不会经
     * 网络传输）。登录不需要选角色，服务器按登录ID查库返回真实角色。
     *
     * @return 校验通过时返回封装好的 {@link User}；输入不合法时返回 {@code null}
     */
    private User collectLoginInput() {
        String uid = _uidField.getText().trim();
        String pwd = _pwdField.getText();

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
        return user;
    }

    /**
     * "登录"按钮的点击处理：发送登录请求并根据响应弹窗提示。
     */
    private void onLogin() {
        String rawPwd = _pwdField.getText();
        User loginUser = collectLoginInput();
        if (loginUser == null) {
            return;
        }
        try {
            Message response = _userClientSrv.login(loginUser);
            if (IConstant.STATUS_SUCCESS.equals(response.getStatusCode())) {
                User found = (User) response.getData();
                if (_rememberBox.isSelected()) {
                    saveRememberedCredential(loginUser.getUId(), rawPwd);
                } else {
                    clearRememberedCredential();
                }
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
     * 注册入口只用于教师和管理员，学生账号由管理员新增学籍时创建。
     */
    private void onRegister() {
        openRegisterDialog();
    }

    /**
     * 弹出教师/管理员注册窗口。注册成功后账号是"待审核"状态，不能立即
     * 登录，要等管理员在"账号管理"页面手动确认。
     */
    private void openRegisterDialog() {
        Stage dialogStage = new Stage();
        dialogStage.setTitle("注册新账号");
        dialogStage.initOwner(_stage);
        dialogStage.initModality(javafx.stage.Modality.WINDOW_MODAL);

        TextField uidField = new TextField();
        uidField.setPromptText("请输入8位学号/工号");
        PasswordField pwdField = new PasswordField();
        pwdField.setPromptText("请输入密码");
        PasswordField confirmField = new PasswordField();
        confirmField.setPromptText("请再输入一次密码");
        TextField nameField = new TextField();
        nameField.setPromptText("请输入真实姓名");
        TextField ageField = new TextField();
        ageField.setPromptText("请输入年龄");

        ComboBox<String> sexBox = new ComboBox<>();
        sexBox.getItems().setAll("男", "女");
        sexBox.getSelectionModel().selectFirst();
        sexBox.setMaxWidth(Double.MAX_VALUE);

        ComboBox<String> roleBox = new ComboBox<>();
        roleBox.getItems().setAll("教师", "管理员");
        roleBox.getSelectionModel().selectFirst();
        roleBox.setMaxWidth(Double.MAX_VALUE);

        for (TextField field : new TextField[] {uidField, pwdField, confirmField, nameField,
                ageField}) {
            field.setPrefHeight(38);
            field.setMaxWidth(Double.MAX_VALUE);
            field.setStyle(fieldStyle());
        }
        sexBox.setPrefHeight(38);
        sexBox.setStyle(fieldStyle());
        roleBox.setPrefHeight(38);
        roleBox.setStyle(fieldStyle());

        Label resultLabel = new Label();
        resultLabel.setWrapText(true);
        resultLabel.setTextFill(Color.web("#e6604f"));

        Button submitButton = new Button("提交注册");
        submitButton.setMaxWidth(Double.MAX_VALUE);
        submitButton.setPrefHeight(42);
        submitButton.setStyle("-fx-background-color: linear-gradient(to right, #3fa34d, #5bc46d);"
                + " -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;"
                + " -fx-background-radius: 21; -fx-cursor: hand;");

        submitButton.setOnAction(e -> {
            String uid = uidField.getText() == null ? "" : uidField.getText().trim();
            String pwd = pwdField.getText() == null ? "" : pwdField.getText();
            String confirm = confirmField.getText() == null ? "" : confirmField.getText();
            String name = nameField.getText() == null ? "" : nameField.getText().trim();
            String ageText = ageField.getText() == null ? "" : ageField.getText().trim();
            String sex = sexBox.getSelectionModel().getSelectedItem();
            String role = roleBox.getSelectionModel().getSelectedItem();
            if (uid.isEmpty() || pwd.isEmpty() || name.isEmpty()) {
                resultLabel.setText("登录ID、密码、姓名都不能为空");
                return;
            }
            if (uid.length() != 8) {
                resultLabel.setText("登录ID必须为8位");
                return;
            }
            if (!pwd.equals(confirm)) {
                resultLabel.setText("两次输入的密码不一致");
                return;
            }
            Integer age = null;
            if (!ageText.isEmpty()) {
                try {
                    age = Integer.valueOf(ageText);
                } catch (NumberFormatException ex) {
                    resultLabel.setText("年龄必须是数字");
                    return;
                }
            }
            User newUser = new User();
            newUser.setUId(uid);
            newUser.setUPwd(MD5Util.md5(pwd));
            newUser.setURole(role);
            newUser.setUName(name);
            newUser.setUAge(age);
            newUser.setUSex(sex);

            submitButton.setDisable(true);
            resultLabel.setTextFill(Color.web("#697687"));
            resultLabel.setText("正在提交…");

            new Thread(() -> {
                String message;
                boolean success;
                try {
                    Message response;
                    response = _userClientSrv.register(newUser);
                    success = IConstant.STATUS_SUCCESS.equals(response.getStatusCode());
                    message = success
                            ? "提交成功！账号需要管理员审核通过后才能登录，请耐心等待。"
                            : String.valueOf(response.getData());
                } catch (IOException ex) {
                    success = false;
                    message = "无法连接服务器，请确认服务器已启动：" + ex.getMessage();
                } catch (ClassNotFoundException ex) {
                    success = false;
                    message = "服务器返回的数据无法识别：" + ex.getMessage();
                }
                final String finalMessage = message;
                final boolean finalSuccess = success;
                Platform.runLater(() -> {
                    submitButton.setDisable(false);
                    if (finalSuccess) {
                        showAlert(Alert.AlertType.INFORMATION, "注册成功", finalMessage);
                        dialogStage.close();
                    } else {
                        resultLabel.setTextFill(Color.web("#e6604f"));
                        resultLabel.setText(finalMessage);
                    }
                });
            }).start();
        });

        VBox form = new VBox(6,
                fieldLabel("登录ID"), uidField,
                fieldLabel("密码"), pwdField,
                fieldLabel("确认密码"), confirmField,
                fieldLabel("角色"), roleBox,
                fieldLabel("姓名"), nameField,
                fieldLabel("年龄"), ageField,
                fieldLabel("性别"), sexBox,
                fieldLabel("说明"), new Label("学生账号由管理员新增学籍时创建，初始密码为 123456。"));
        form.setFillWidth(true);

        Label title = new Label("注册新账号");
        title.setFont(Font.font("System", FontWeight.BOLD, 20));
        title.setTextFill(Color.web("#1d2b39"));

        Label tip = new Label("教师和管理员账号提交后需要管理员审核通过；学生账号请联系管理员创建。");
        tip.setWrapText(true);
        tip.setTextFill(Color.web("#8b96a4"));
        tip.setFont(Font.font("System", 12));

        VBox card = new VBox(12, title, tip, form, submitButton, resultLabel);
        card.setPadding(new Insets(26));
        card.setPrefWidth(380);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 18;");

        StackPane root = new StackPane(card);
        root.setStyle("-fx-background-color: #eef3f8;");
        javafx.scene.control.ScrollPane scrollPane = new javafx.scene.control.ScrollPane(root);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
        Scene scene = new Scene(scrollPane, 440, 680);
        dialogStage.setScene(scene);
        dialogStage.setResizable(false);
        dialogStage.showAndWait();
    }

    /**
     * 把这次登录成功的账号密码记到本地文件里（供下次自动填充），文件放在
     * 用户主目录，不会被提交进仓库。只做简单编码，不是安全存储，纯粹是
     * 本地使用便利——如果要更安全，得走操作系统级别的凭据管理，这个不在
     * 课程项目范围内。
     *
     * @param uid    登录ID
     * @param rawPwd 明文密码
     */
    private void saveRememberedCredential(String uid, String rawPwd) {
        try {
            java.util.Properties props = new java.util.Properties();
            props.setProperty("uid", uid);
            props.setProperty("pwd", java.util.Base64.getEncoder()
                    .encodeToString(rawPwd.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            try (java.io.OutputStream out = new java.io.FileOutputStream(REMEMBER_FILE)) {
                props.store(out, "Vcampus 本地记住的登录信息，删掉这个文件就能清空");
            }
        } catch (IOException e) {
            // 记住密码只是便利功能，写文件失败不影响正常登录，忽略即可。
        }
    }

    /**
     * 清空本地记住的登录信息（取消勾选"记住密码"后登录时调用）。
     */
    private void clearRememberedCredential() {
        if (REMEMBER_FILE.isFile()) {
            REMEMBER_FILE.delete();
        }
    }

    /**
     * 启动时如果本地有记住的登录信息，回填登录ID和密码，并勾上"记住密码"。
     * 文件不存在或读取失败时静默跳过，不影响正常使用。
     */
    private void loadRememberedCredential() {
        if (!REMEMBER_FILE.isFile()) {
            return;
        }
        try (java.io.InputStream in = new java.io.FileInputStream(REMEMBER_FILE)) {
            java.util.Properties props = new java.util.Properties();
            props.load(in);
            String uid = props.getProperty("uid");
            String encodedPwd = props.getProperty("pwd");
            if (uid != null && encodedPwd != null) {
                String rawPwd = new String(java.util.Base64.getDecoder().decode(encodedPwd),
                        java.nio.charset.StandardCharsets.UTF_8);
                _uidField.setText(uid);
                _pwdField.setText(rawPwd);
                _rememberBox.setSelected(true);
            }
        } catch (Exception e) {
            // 本地记住的文件损坏或者格式不对，直接当作没记住处理。
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
