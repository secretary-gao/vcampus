/*
 * AIChatPanel
 *
 * Version 2.0
 *
 * 2026-09-17
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
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * 可嵌入的"校园AI"问答面板：仿 Claude 客户端的布局——左侧一栏"新对话 +
 * 历史会话列表"，右侧是当前会话的聊天记录（头像+气泡）和输入框。背景铺一张
 * 东南大学李文正图书馆的照片（{@link #BACKGROUND_IMAGE_PATH}，找不到时静默
 * 回退成纯色背景，不会空白或报错，做法和 {@link vcampus.client.view.CampusBackground}
 * 一致）。
 *
 * <p>每个会话（{@link ChatSession}）独立保存自己的聊天记录节点，切换会话时
 * 只是把 {@link #_scrollPane} 的内容换成对应会话的记录；发送问题仍然是后台
 * 线程调用 {@link IAIClientSrv#ask}，避免卡住 JavaFX 界面线程。</p>
 *
 * <p>所有角色都能用，不区分学生/教师/管理员。</p>
 */
public class AIChatPanel extends StackPane {

    /** AI 强调色：暖橙色，呼应"校园AI"这个场景，比之前生硬的纯蓝更柔和。 */
    private static final String AI_COLOR = "#d97a4a";

    /** 强调色的浅色版本，用于选中态背景等。 */
    private static final String AI_COLOR_SOFT = "#fbeee6";

    /** 背景图相对项目根目录的路径：李文正图书馆照片。 */
    private static final String BACKGROUND_IMAGE_PATH = "/vcampus/client/view/assets/lwz.jpg";

    /** 图标图片相对项目根目录的路径。 */
    private static final String ICON_IMAGE_PATH = "/vcampus/client/view/assets/icon.png";

    /** 面板整体圆角，和 {@code MainFrame} 里卡片的圆角保持一致。 */
    private static final double PANEL_ARC = 18;

    /** 客户端 AI 问答业务服务。 */
    private final IAIClientSrv _aiClientSrv = new AIClientSrv();

    /** 当前登录用户，用于头像和称呼；可以为 {@code null}（未登录场景）。 */
    private final User _currentUser;

    /** 全部会话，最新的在最前面（列表展示顺序）。 */
    private final List<ChatSession> _sessions = new ArrayList<>();

    /** 历史会话列表容器（左侧栏里可滚动的那部分）。 */
    private final VBox _sessionListBox = new VBox(4);

    /** 当前激活的会话。 */
    private ChatSession _activeSession;

    /** 对话记录的滚动容器：内容会随着切换会话而替换成对应会话的记录节点。 */
    private final ScrollPane _scrollPane = new ScrollPane();

    /** 问题输入框。 */
    private final TextField _inputField = new TextField();

    /** 发送按钮。 */
    private final Button _sendButton = new Button("发送");

    /**
     * 一次独立的对话：标题（取自第一条提问，截断显示）+ 自己的聊天记录节点。
     */
    private static final class ChatSession {
        private String title = "新对话";
        private boolean titled = false;
        private final VBox transcript = new VBox(14);
        private Label sidebarLabel;
        private HBox sidebarRow;
    }

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

        setMinHeight(560);
        setClip(buildClip());

        Region background = buildBackground();
        HBox layout = buildLayout();

        getChildren().addAll(background, layout);

        newSession();
    }

    /**
     * 面板整体裁成圆角，配合 {@code MainFrame} 里卡片的视觉风格，背景图也会
     * 跟着裁出圆角，不会在卡片外露出方角。
     *
     * @return 裁剪形状
     */
    private Rectangle buildClip() {
        Rectangle clip = new Rectangle();
        clip.setArcWidth(PANEL_ARC * 2);
        clip.setArcHeight(PANEL_ARC * 2);
        clip.widthProperty().bind(widthProperty());
        clip.heightProperty().bind(heightProperty());
        return clip;
    }

    /**
     * 构建背景：优先加载李文正图书馆照片，按"裁剪填满"铺满整个面板，叠一层
     * 浅色半透明遮罩保证文字可读；找不到图片时返回一个纯色背景，不会空白。
     *
     * @return 背景节点
     */
    private Region buildBackground() {
        StackPane backgroundStack = new StackPane();
        Image image = loadBackgroundImage();
        if (image == null) {
            backgroundStack.setStyle("-fx-background-color: linear-gradient(to bottom right, #fbeee6, #f4f6f8);");
            return backgroundStack;
        }
        ImageView view = new ImageView(image);
        configureCoverFit(view, backgroundStack);
        Region scrim = new Region();
        scrim.setStyle("-fx-background-color: rgba(255,255,255,0.62);");
        backgroundStack.getChildren().addAll(view, scrim);
        return backgroundStack;
    }

    /**
     * 尝试加载背景图片文件。
     *
     * @return 加载成功的图片；文件不存在或加载失败时返回 {@code null}
     */
    private Image loadBackgroundImage() {
        URL url = AIChatPanel.class.getResource(BACKGROUND_IMAGE_PATH);
        if (url == null) {
            return null;
        }
        try {
            Image image = new Image(url.toExternalForm(), 0, 0, true, true, false);
            return image.isError() ? null : image;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 让一个 {@link ImageView} 按"裁剪填满"的方式跟随容器尺寸缩放，写法和
     * {@link vcampus.client.view.CampusBackground} 完全一致：用
     * {@link Bindings#createDoubleBinding} 在绑定的那一刻就用当前真实值算一次，
     * 避免"容器已经完成首次布局、错过尺寸变化事件"导致图片显示不出来。
     *
     * @param view      要设置的图片视图
     * @param container 参照的容器
     */
    private void configureCoverFit(ImageView view, StackPane container) {
        view.setPreserveRatio(true);
        view.setSmooth(true);

        DoubleBinding scale = Bindings.createDoubleBinding(() -> {
            Image image = view.getImage();
            double containerW = container.getWidth();
            double containerH = container.getHeight();
            if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0
                    || containerW <= 0 || containerH <= 0) {
                return 0.0;
            }
            return Math.max(containerW / image.getWidth(), containerH / image.getHeight());
        }, view.imageProperty(), container.widthProperty(), container.heightProperty());

        view.fitWidthProperty().bind(Bindings.createDoubleBinding(
                () -> view.getImage() == null ? 0.0 : view.getImage().getWidth() * scale.get(),
                scale, view.imageProperty()));
        view.fitHeightProperty().bind(Bindings.createDoubleBinding(
                () -> view.getImage() == null ? 0.0 : view.getImage().getHeight() * scale.get(),
                scale, view.imageProperty()));
    }

    /**
     * 构建整体布局：左侧历史会话栏 + 右侧聊天区。
     *
     * @return 布局容器
     */
    private HBox buildLayout() {
        HBox layout = new HBox(_sidebar(), buildChatArea());
        layout.setFillHeight(true);
        return layout;
    }

    /**
     * 构建左侧栏：品牌区、"新对话"按钮、历史会话列表。
     *
     * @return 左侧栏容器
     */
    private VBox _sidebar() {
        VBox sidebar = new VBox(10);
        sidebar.setPrefWidth(220);
        sidebar.setMinWidth(220);
        sidebar.setMaxWidth(220);
        sidebar.setPadding(new Insets(18, 12, 18, 16));
        sidebar.setStyle("-fx-background-color: rgba(255,255,255,0.88);");

        HBox brand = new HBox(8, buildAiIcon(28), brandLabel());
        brand.setAlignment(Pos.CENTER_LEFT);
        brand.setPadding(new Insets(0, 0, 6, 2));

        Button newChatButton = new Button("＋ 新对话");
        newChatButton.setMaxWidth(Double.MAX_VALUE);
        newChatButton.setStyle("-fx-background-color: " + AI_COLOR + "; -fx-text-fill: white;"
                + " -fx-background-radius: 12; -fx-font-weight: bold; -fx-cursor: hand;"
                + " -fx-padding: 9 0 9 0; -fx-effect: dropshadow(gaussian, rgba(217,122,74,0.35), 10, 0.2, 0, 3);");
        newChatButton.setOnAction(event -> newSession());

        Label historyLabel = new Label("历史对话");
        historyLabel.setFont(Font.font("System", FontWeight.BOLD, 11));
        historyLabel.setStyle("-fx-text-fill: #9a8f88;");
        historyLabel.setPadding(new Insets(10, 0, 2, 4));

        _sessionListBox.setPadding(new Insets(0, 2, 0, 0));
        ScrollPane historyScroll = new ScrollPane(_sessionListBox);
        historyScroll.setFitToWidth(true);
        historyScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(historyScroll, Priority.ALWAYS);

        sidebar.getChildren().addAll(brand, newChatButton, historyLabel, historyScroll);
        return sidebar;
    }

    /**
     * 品牌文字："校园AI"。
     *
     * @return 标题标签
     */
    private Label brandLabel() {
        Label label = new Label("校园AI");
        label.setFont(Font.font("System", FontWeight.BOLD, 15));
        label.setStyle("-fx-text-fill: #3a2e28;");
        return label;
    }

    /**
     * 构建右侧聊天区：标题条 + 对话记录 + 输入框。
     *
     * @return 聊天区容器
     */
    private VBox buildChatArea() {
        VBox chatArea = new VBox(14);
        chatArea.setPadding(new Insets(20, 24, 20, 24));
        HBox.setHgrow(chatArea, Priority.ALWAYS);

        Label tip = new Label("嵌入在 Vcampus 系统里的智能助手，有什么问题都可以问问，仅供参考。");
        tip.setStyle("-fx-text-fill: #7a6f68;");
        tip.setFont(Font.font("System", 12));
        HBox tipBar = new HBox(tip);
        tipBar.setPadding(new Insets(9, 14, 9, 14));
        tipBar.setStyle("-fx-background-color: rgba(255,255,255,0.9); -fx-background-radius: 12;");

        _scrollPane.setFitToWidth(true);
        // 不铺白色卡片，让最底层的李文正图书馆背景图直接透出来；文字可读性靠每条
        // 消息气泡自己不透明的背景色兜底（AI 气泡 #f2ece8、用户气泡纯色 AI_COLOR），
        // 不依赖这层外壳。两个属性都要设透明——ScrollPane 真正铺在视口/内容后面的是
        // -fx-background 这个专属属性，只设 -fx-background-color 只会影响控件外层。
        _scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(_scrollPane, Priority.ALWAYS);

        _inputField.setPromptText("跟校园AI说点什么，按回车或点发送");
        _inputField.setStyle("-fx-background-radius: 20; -fx-padding: 10 18 10 18;"
                + "-fx-background-color: rgba(255,255,255,0.96); -fx-border-color: rgba(0,0,0,0.06);"
                + "-fx-border-radius: 20; -fx-text-fill: #2b241f; -fx-prompt-text-fill: #a89c92;");
        _inputField.setOnAction(event -> onSend());
        HBox.setHgrow(_inputField, Priority.ALWAYS);

        _sendButton.setStyle("-fx-background-color: " + AI_COLOR + "; -fx-text-fill: white;"
                + " -fx-background-radius: 20; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 10 24 10 24;"
                + " -fx-effect: dropshadow(gaussian, rgba(217,122,74,0.35), 10, 0.2, 0, 3);");
        _sendButton.setOnAction(event -> onSend());

        HBox inputBar = new HBox(10, _inputField, _sendButton);
        inputBar.setAlignment(Pos.CENTER_LEFT);

        chatArea.getChildren().addAll(tipBar, _scrollPane, inputBar);
        return chatArea;
    }

    /**
     * 头部/侧边栏用的 AI 图标：优先加载 {@link #ICON_IMAGE_PATH} 圆形裁剪显示，
     * 找不到图片或加载失败时回退成"圆形色块 + AI 文字"，做法和
     * {@link vcampus.client.view.SeuEmblem#tryLoadImage} 一致。
     *
     * @param size 直径
     * @return 图标节点
     */
    private StackPane buildAiIcon(double size) {
        StackPane icon = new StackPane();
        icon.setPrefSize(size, size);
        ImageView iconImage = tryLoadIconImage(size);
        if (iconImage != null) {
            icon.getChildren().add(iconImage);
            return icon;
        }
        Circle circle = new Circle(size / 2.0, Color.web(AI_COLOR));
        Label text = new Label("AI");
        text.setStyle("-fx-text-fill: white;");
        text.setFont(Font.font("System", FontWeight.BOLD, size * 0.34));
        icon.getChildren().addAll(circle, text);
        return icon;
    }

    /**
     * 尝试加载图标图片并按直径圆形裁切、缩放。
     *
     * @param size 目标直径
     * @return 加载成功时返回可用的 {@link ImageView}；文件不存在或加载失败时返回 {@code null}
     */
    private ImageView tryLoadIconImage(double size) {
        URL url = AIChatPanel.class.getResource(ICON_IMAGE_PATH);
        if (url == null) {
            return null;
        }
        try {
            Image image = new Image(url.toExternalForm(), size, size, false, true, true);
            if (image.isError()) {
                return null;
            }
            ImageView view = new ImageView(image);
            view.setFitWidth(size);
            view.setFitHeight(size);
            view.setPreserveRatio(false);
            view.setSmooth(true);
            view.setClip(new Circle(size / 2.0, size / 2.0, size / 2.0));
            return view;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 新建一个空会话并切换过去：清空的对话记录里先放一条问候语，和之前单会话
     * 版本的开场白保持一致。
     */
    private void newSession() {
        ChatSession session = new ChatSession();
        session.transcript.setPadding(new Insets(16));
        session.transcript.setAlignment(Pos.TOP_LEFT);
        _sessions.add(0, session);
        appendBubble(session, "校园AI", "你好，我是校园AI，Vcampus 系统里的智能助手，有什么想问的都可以说。", false);
        renderSessionList();
        switchToSession(session);
    }

    /**
     * 切换到指定会话：把滚动容器内容换成该会话的记录节点，并刷新左侧栏高亮。
     *
     * @param session 目标会话
     */
    private void switchToSession(ChatSession session) {
        _activeSession = session;
        _scrollPane.setContent(session.transcript);
        for (ChatSession s : _sessions) {
            boolean active = s == session;
            if (s.sidebarRow != null) {
                s.sidebarRow.setStyle("-fx-background-radius: 10; -fx-cursor: hand; -fx-padding: 8 10 8 10;"
                        + (active ? " -fx-background-color: " + AI_COLOR_SOFT + ";" : ""));
                // 历史对话文字统一用纯黑色，选中/未选中都一样，不再靠深浅区分——
                // 保证在任何背景下都够对比度，选中状态只靠背景色块和加粗区分。
                // 注意：颜色必须写进 setStyle 的内联样式里，不能靠单独调用
                // setTextFill()——JavaFX 有个坑，节点第一次做 CSS 计算时会把
                // 纯 Java 属性设置的 textFill 顶掉变回默认色（这里默认是白色，
                // 之前就是因为这样导致文字"看不见"，点一下触发二次赋值才短暂生效）。
                s.sidebarLabel.setStyle("-fx-text-fill: #1a1a1a;");
                s.sidebarLabel.setFont(Font.font("System", active ? FontWeight.BOLD : FontWeight.NORMAL, 12.5));
            }
        }
        scrollToBottom();
    }

    /**
     * 重建左侧历史会话列表（每次新增会话或标题更新后调用）。
     */
    private void renderSessionList() {
        _sessionListBox.getChildren().clear();
        for (ChatSession session : _sessions) {
            Label label = new Label(session.title);
            label.setFont(Font.font("System", 12.5));
            label.setStyle("-fx-text-fill: #1a1a1a;");
            HBox row = new HBox(label);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setMaxWidth(Double.MAX_VALUE);
            row.setStyle("-fx-background-radius: 10; -fx-cursor: hand; -fx-padding: 8 10 8 10;");
            row.setOnMouseClicked(event -> switchToSession(session));
            session.sidebarLabel = label;
            session.sidebarRow = row;
            _sessionListBox.getChildren().add(row);
        }
    }

    /**
     * 处理一次发送：显示问题气泡、插入一条"正在思考"的占位气泡，后台线程
     * 请求服务器，回来后把占位替换成真正的回答。第一次提问会顺带把会话标题
     * 从"新对话"换成问题内容（截断），并刷新左侧栏显示。
     */
    private void onSend() {
        String question = _inputField.getText() == null ? "" : _inputField.getText().trim();
        if (question.isEmpty() || _activeSession == null) {
            return;
        }
        ChatSession session = _activeSession;
        if (!session.titled) {
            session.title = question.length() > 16 ? question.substring(0, 16) + "…" : question;
            session.titled = true;
            renderSessionList();
            switchToSession(session);
        }

        appendBubble(session, myDisplayName(), question, true);
        _inputField.clear();
        _inputField.setDisable(true);
        _sendButton.setDisable(true);

        Label thinkingBubble = appendBubble(session, "校园AI", "校园AI 正在思考…", false);
        // 追加而不是覆盖 setStyle：appendBubble 已经给它设过背景色/圆角/内边距，
        // 这里在同一个内联样式字符串后面拼上文字颜色（不能单独调 setTextFill，
        // 会被 JavaFX 那个"首次 CSS 计算吃掉纯 Java 设置的 textFill"的坑吞掉）。
        thinkingBubble.setStyle(thinkingBubble.getStyle() + " -fx-text-fill: #9a8f88;");

        new Thread(() -> {
            String answerText;
            try {
                Message response = (_aiClientSrv instanceof AIClientSrv client)
                        ? client.ask(question, _currentUser) : _aiClientSrv.ask(question);
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
                thinkingBubble.setStyle(thinkingBubble.getStyle().replace("-fx-text-fill: #9a8f88;", "")
                        + " -fx-text-fill: #1d2b39;");
                _inputField.setDisable(false);
                _sendButton.setDisable(false);
                _inputField.requestFocus();
                if (_activeSession == session) {
                    scrollToBottom();
                }
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
     * 往指定会话的对话记录里追加一条带头像的气泡。
     *
     * @param session 目标会话
     * @param speaker 说话人（显示在气泡上方的小字）
     * @param text    内容
     * @param isUser  是否是用户自己发的（决定气泡颜色和头像颜色）
     * @return 气泡里内容 {@link Label}，方便调用方后续替换文本（比如"正在思考"占位）
     */
    private Label appendBubble(ChatSession session, String speaker, String text, boolean isUser) {
        Label content = new Label(text);
        content.setWrapText(true);
        content.setMaxWidth(480);
        // 文字颜色和背景一起写进同一个 setStyle 字符串，不要单独调 setTextFill()——
        // 见 switchToSession() 里的注释，单独调会被 JavaFX 首次 CSS 计算吃掉变白。
        content.setStyle((isUser ? "-fx-background-color: " + AI_COLOR + "; -fx-text-fill: white;"
                : "-fx-background-color: #f2ece8; -fx-text-fill: #1d2b39;")
                + " -fx-background-radius: 18; -fx-padding: 11 16 11 16;");

        Label speakerLabel = new Label(speaker);
        speakerLabel.setFont(Font.font("System", FontWeight.BOLD, 11));
        speakerLabel.setStyle("-fx-text-fill: #9a8f88;");

        // 用户消息不再靠右分居两侧——之前那套"右对齐"排版在个别机器上死活
        // 显示不出来，排查了好几轮都没能百分百锁定根因，用户也明确说了不用
        // 再纠结原因，直接改成跟 AI 消息完全一样的"头像+气泡都靠左、从上到下
        // 顺序排列"，只在气泡颜色/头像上区分我方和对方，位置上不再有任何
        // "靠右"的代码路径，从根上避开这个问题。
        VBox bubble = new VBox(4, speakerLabel, content);
        bubble.setAlignment(Pos.TOP_LEFT);

        StackPane avatar;
        if (isUser) {
            avatar = new StackPane();
            avatar.setPrefSize(32, 32);
            Circle avatarCircle = new Circle(16, Color.web("#3fa34d"));
            Label avatarText = new Label(avatarInitial(speaker));
            avatarText.setStyle("-fx-text-fill: white;");
            avatarText.setFont(Font.font("System", FontWeight.BOLD, 12));
            avatar.getChildren().addAll(avatarCircle, avatarText);
        } else {
            avatar = buildAiIcon(32);
        }

        HBox innerRow = new HBox(10, avatar, bubble);
        innerRow.setAlignment(Pos.CENTER);

        // BorderPane 只用来把整行钉在左边（比直接把 innerRow 加进 VBox 更明确、
        // 也和之前排查时验证过确实稳定能显示的写法保持一致），不再有 setRight 分支。
        BorderPane row = new BorderPane();
        row.setMaxWidth(Double.MAX_VALUE);
        row.setLeft(innerRow);

        session.transcript.getChildren().add(row);
        if (session == _activeSession) {
            scrollToBottom();
        }
        return content;
    }

    /**
     * 头像圆圈里显示的一个字：取称呼的首字。
     *
     * @param speaker 说话人称呼
     * @return 头像文字
     */
    private String avatarInitial(String speaker) {
        return speaker == null || speaker.isEmpty() ? "?" : speaker.substring(0, 1);
    }

    /**
     * 把对话记录滚动到底部，让新消息始终可见。
     */
    private void scrollToBottom() {
        Platform.runLater(() -> _scrollPane.setVvalue(1.0));
    }
}
