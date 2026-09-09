/*
 * AIChatPanel
 *
 * Version 1.1
 *
 * 2026-09-09
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.client.view.ai;

import vcampus.client.biz.AIClientSrv;
import vcampus.client.biz.IAIClientSrv;
import vcampus.common.constant.IConstant;
import vcampus.common.vo.Message;
import vcampus.common.vo.User;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.io.IOException;

/**
 * 可嵌入的"校园AI"问答面板：上面是滚动的对话记录（头像+气泡，问题/回答用
 * 不同背景色区分），下面是输入框+发送按钮。点发送在后台线程调用
 * {@link IAIClientSrv#ask}，避免卡住 JavaFX 界面线程，先插入一条"校园AI
 * 正在思考…"的占位气泡，回来后用 {@link Platform#runLater} 把占位替换成
 * 真正的回答。
 *
 * <p>所有角色都能用，不区分学生/教师/管理员。</p>
 */
public class AIChatPanel extends VBox {

    /** AI 强调色，跟主界面导航栏"校园AI"入口的颜色保持一致。 */
    private static final String AI_COLOR = "#1c5a97";

    /** 客户端 AI 问答业务服务。 */
    private final IAIClientSrv _aiClientSrv = new AIClientSrv();

    /** 当前登录用户，用于头像和称呼；可以为 {@code null}（未登录场景）。 */
    private final User _currentUser;

    /** 对话记录容器。 */
    private final VBox _transcript = new VBox(14);

    /** 对话记录的滚动容器，用于每次新增消息后自动滚到底部。 */
    private final ScrollPane _scrollPane = new ScrollPane(_transcript);

    /** 问题输入框。 */
    private final TextField _inputField = new TextField();

    /** 发送按钮。 */
    private final Button _sendButton = new Button("发送");

    /**
     * 构造方法（未登录场景，头像用"我"兜底）。
     */
    public AIChatPanel() {
        this(null);
    }

    /**
     * 构造方法，搭好整个面板的布局。
     *
     * @param currentUser 当前登录用户，用于头像显示姓名首字；可为 {@code null}
     */
    public AIChatPanel(User currentUser) {
        this._currentUser = currentUser;

        setSpacing(14);
        setPadding(new Insets(20));
        setPrefWidth(760);
        setMaxHeight(Double.MAX_VALUE);

        StackPane aiIcon = new StackPane();
        aiIcon.setPrefSize(40, 40);
        Circle aiIconCircle = new Circle(20, Color.web(AI_COLOR));
        Label aiIconText = new Label("AI");
        aiIconText.setTextFill(Color.WHITE);
        aiIconText.setFont(Font.font("System", FontWeight.BOLD, 13));
        aiIcon.getChildren().addAll(aiIconCircle, aiIconText);

        Label title = new Label("校园AI");
        title.setFont(Font.font("System", FontWeight.BOLD, 20));
        title.setTextFill(Color.web("#1d2b39"));

        Label tip = new Label("嵌入在 Vcampus 系统里的智能助手，有什么问题都可以问问，仅供参考。");
        tip.setTextFill(Color.web("#8b96a4"));
        tip.setFont(Font.font("System", 12));

        VBox titleBlock = new VBox(2, title, tip);
        HBox header = new HBox(12, aiIcon, titleBlock);
        header.setAlignment(Pos.CENTER_LEFT);

        _transcript.setPadding(new Insets(16));
        _transcript.setAlignment(Pos.TOP_LEFT);

        _scrollPane.setFitToWidth(true);
        _scrollPane.setStyle("-fx-background-color: #f8fafc; -fx-background: #f8fafc;"
                + "-fx-background-radius: 14; -fx-border-radius: 14; -fx-border-color: rgba(0,0,0,0.06);");
        VBox.setVgrow(_scrollPane, Priority.ALWAYS);

        _inputField.setPromptText("跟校园AI说点什么，按回车或点发送");
        _inputField.setStyle("-fx-background-radius: 18; -fx-padding: 9 16 9 16;"
                + "-fx-background-color: #f4f6f8; -fx-border-color: transparent;");
        _inputField.setOnAction(event -> onSend());
        HBox.setHgrow(_inputField, Priority.ALWAYS);

        _sendButton.setStyle("-fx-background-color: " + AI_COLOR + "; -fx-text-fill: white;"
                + " -fx-background-radius: 18; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 9 22 9 22;");
        _sendButton.setOnAction(event -> onSend());

        HBox inputBar = new HBox(10, _inputField, _sendButton);
        inputBar.setAlignment(Pos.CENTER_LEFT);

        getChildren().addAll(header, _scrollPane, inputBar);
        appendBubble("校园AI", "你好，我是校园AI，Vcampus 系统里的智能助手，有什么想问的都可以说。", false);
    }

    /**
     * 处理一次发送：显示问题气泡、插入一条"正在思考"的占位气泡，后台线程
     * 请求服务器，回来后把占位替换成真正的回答。
     */
    private void onSend() {
        String question = _inputField.getText() == null ? "" : _inputField.getText().trim();
        if (question.isEmpty()) {
            return;
        }
        appendBubble(myDisplayName(), question, true);
        _inputField.clear();
        _inputField.setDisable(true);
        _sendButton.setDisable(true);

        Label thinkingBubble = appendBubble("校园AI", "校园AI 正在思考…", false);
        thinkingBubble.setTextFill(Color.web("#8b96a4"));

        new Thread(() -> {
            String answerText;
            try {
                Message response = _aiClientSrv.ask(question);
                boolean success = IConstant.STATUS_SUCCESS.equals(response.getStatusCode());
                answerText = String.valueOf(response.getData());
                if (!success) {
                    answerText = "出错了：" + answerText;
                }
            } catch (IOException | ClassNotFoundException e) {
                answerText = "网络异常：" + e.getMessage();
            }
            final String finalAnswer = answerText;
            Platform.runLater(() -> {
                thinkingBubble.setText(finalAnswer);
                thinkingBubble.setTextFill(Color.web("#1d2b39"));
                _inputField.setDisable(false);
                _sendButton.setDisable(false);
                _inputField.requestFocus();
                scrollToBottom();
            });
        }).start();
    }

    /**
     * 当前用户在对话里显示的称呼：有姓名用姓名，没有就用登录ID，都没有
     * 就用"我"兜底。
     *
     * @return 显示用的称呼
     */
    private String myDisplayName() {
        if (_currentUser == null) {
            return "我";
        }
        String name = _currentUser.getUName();
        if (name != null && !name.trim().isEmpty()) {
            return name.trim();
        }
        String uid = _currentUser.getUId();
        return uid == null || uid.trim().isEmpty() ? "我" : uid.trim();
    }

    /**
     * 往对话记录里追加一条带头像的气泡。
     *
     * @param speaker 说话人（显示在气泡上方的小字）
     * @param text    内容
     * @param isUser  是否是用户自己发的（决定气泡颜色、头像颜色和靠左/靠右）
     * @return 气泡里内容 {@link Label}，方便调用方后续替换文本（比如"正在思考"占位）
     */
    private Label appendBubble(String speaker, String text, boolean isUser) {
        Label content = new Label(text);
        content.setWrapText(true);
        content.setMaxWidth(480);
        content.setTextFill(isUser ? Color.WHITE : Color.web("#1d2b39"));
        content.setStyle((isUser ? "-fx-background-color: " + AI_COLOR + ";" : "-fx-background-color: #eef3f8;")
                + " -fx-background-radius: 16; -fx-padding: 10 14 10 14;");

        Label speakerLabel = new Label(speaker);
        speakerLabel.setFont(Font.font("System", FontWeight.BOLD, 11));
        speakerLabel.setTextFill(Color.web("#8b96a4"));

        VBox bubble = new VBox(4, speakerLabel, content);
        bubble.setAlignment(isUser ? Pos.TOP_RIGHT : Pos.TOP_LEFT);

        StackPane avatar = new StackPane();
        avatar.setPrefSize(34, 34);
        Circle avatarCircle = new Circle(17, Color.web(isUser ? "#3fa34d" : AI_COLOR));
        Label avatarText = new Label(avatarInitial(speaker, isUser));
        avatarText.setTextFill(Color.WHITE);
        avatarText.setFont(Font.font("System", FontWeight.BOLD, 12));
        avatar.getChildren().addAll(avatarCircle, avatarText);

        HBox row = new HBox(10, isUser ? bubble : avatar, isUser ? avatar : bubble);
        row.setAlignment(isUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        _transcript.getChildren().add(row);
        scrollToBottom();
        return content;
    }

    /**
     * 头像圆圈里显示的一两个字：AI 固定显示"AI"，用户取称呼的首字。
     *
     * @param speaker 说话人称呼
     * @param isUser  是否是用户
     * @return 头像文字
     */
    private String avatarInitial(String speaker, boolean isUser) {
        if (!isUser) {
            return "AI";
        }
        return speaker == null || speaker.isEmpty() ? "?" : speaker.substring(0, 1);
    }

    /**
     * 把对话记录滚动到底部，让新消息始终可见。
     */
    private void scrollToBottom() {
        Platform.runLater(() -> _scrollPane.setVvalue(1.0));
    }
}
