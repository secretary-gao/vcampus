/*
 * AIChatPanel
 *
 * Version 1.0
 *
 * 2026-09-07
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.client.view.ai;

import vcampus.client.biz.AIClientSrv;
import vcampus.client.biz.IAIClientSrv;
import vcampus.common.constant.IConstant;
import vcampus.common.vo.Message;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.io.IOException;

/**
 * 可嵌入的 AI 问答面板：上面是滚动的对话记录（问题/回答用不同背景色的
 * 气泡样式区分），下面是输入框+发送按钮。点发送在后台线程调用
 * {@link IAIClientSrv#ask}，避免卡住 JavaFX 界面线程，回来后用
 * {@link Platform#runLater} 把回答写回界面。
 *
 * <p>所有角色都能用，不区分学生/教师/管理员。</p>
 */
public class AIChatPanel extends VBox {

    /** 客户端 AI 问答业务服务。 */
    private final IAIClientSrv _aiClientSrv = new AIClientSrv();

    /** 对话记录容器。 */
    private final VBox _transcript = new VBox(10);

    /** 问题输入框。 */
    private final TextField _inputField = new TextField();

    /** 发送按钮。 */
    private final Button _sendButton = new Button("发送");

    /**
     * 构造方法，搭好整个面板的布局。
     */
    public AIChatPanel() {
        setSpacing(12);
        setPadding(new Insets(20));
        setPrefWidth(760);
        setMaxHeight(Double.MAX_VALUE);

        Label title = new Label("智能问答");
        title.setFont(Font.font("System", FontWeight.BOLD, 20));
        title.setTextFill(Color.web("#1d2b39"));

        Label tip = new Label("接入通义千问，有什么问题都可以问，仅供参考。");
        tip.setTextFill(Color.web("#8b96a4"));
        tip.setFont(Font.font("System", 12));

        _transcript.setPadding(new Insets(12));
        _transcript.setAlignment(Pos.TOP_LEFT);

        ScrollPane scrollPane = new ScrollPane(_transcript);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: #f8fafc; -fx-background: #f8fafc;"
                + "-fx-background-radius: 14; -fx-border-radius: 14; -fx-border-color: rgba(0,0,0,0.06);");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        _inputField.setPromptText("输入问题，按回车或点发送");
        _inputField.setOnAction(event -> onSend());
        HBox.setHgrow(_inputField, Priority.ALWAYS);

        _sendButton.setStyle("-fx-background-color: #1c5a97; -fx-text-fill: white;"
                + " -fx-background-radius: 18; -fx-font-weight: bold; -fx-cursor: hand;");
        _sendButton.setOnAction(event -> onSend());

        HBox inputBar = new HBox(10, _inputField, _sendButton);
        inputBar.setAlignment(Pos.CENTER_LEFT);

        getChildren().addAll(title, tip, scrollPane, inputBar);
        appendBubble("AI", "你好，我是接入通义千问的智能助手，有什么想问的吗？", false);
    }

    /**
     * 处理一次发送：读取输入框内容、显示到对话记录里、后台线程请求服务器。
     */
    private void onSend() {
        String question = _inputField.getText() == null ? "" : _inputField.getText().trim();
        if (question.isEmpty()) {
            return;
        }
        appendBubble("我", question, true);
        _inputField.clear();
        _inputField.setDisable(true);
        _sendButton.setDisable(true);

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
                appendBubble("AI", finalAnswer, false);
                _inputField.setDisable(false);
                _sendButton.setDisable(false);
                _inputField.requestFocus();
            });
        }).start();
    }

    /**
     * 往对话记录里追加一条气泡。
     *
     * @param speaker 说话人（"我"/"AI"）
     * @param text    内容
     * @param isUser  是否是用户自己发的（决定气泡颜色和靠左/靠右）
     */
    private void appendBubble(String speaker, String text, boolean isUser) {
        Label content = new Label(text);
        content.setWrapText(true);
        content.setMaxWidth(520);
        content.setTextFill(isUser ? Color.WHITE : Color.web("#1d2b39"));
        content.setStyle((isUser ? "-fx-background-color: #1c5a97;" : "-fx-background-color: #eef3f8;")
                + " -fx-background-radius: 14; -fx-padding: 10 14 10 14;");

        Label speakerLabel = new Label(speaker);
        speakerLabel.setFont(Font.font("System", FontWeight.BOLD, 11));
        speakerLabel.setTextFill(Color.web("#8b96a4"));

        VBox bubble = new VBox(4, speakerLabel, content);
        bubble.setAlignment(isUser ? Pos.TOP_RIGHT : Pos.TOP_LEFT);

        HBox row = new HBox(bubble);
        row.setAlignment(isUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        _transcript.getChildren().add(row);
    }
}
