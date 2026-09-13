/*
 * HospitalFrame
 *
 * Version 2.7 新增：左侧导航分组折叠展开（用户操作 / 管理员操作）
 * 保留之前全部4项修复：空表格占位、导航高亮、修改医生回显、预约选中反馈
 *
 * 2026-09-09
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.client.view;
import vcampus.client.biz.HospitalClientSrv;
import vcampus.common.constant.IConstant;
import vcampus.common.vo.Appointment;
import vcampus.common.vo.Doctor;
import vcampus.common.vo.Message;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javafx.scene.Cursor;

public class HospitalFrame extends VBox {
    private final String _loginUserId;
    private final String _loginUserRole;
    private final HospitalClientSrv _hospitalSrv = new HospitalClientSrv();

    //====【新增】导航按钮样式常量
    private static final String BTN_NORMAL_STYLE = "-fx-background-color:#367ba9;-fx-text-fill:white;-fx-font-size:12px;-fx-background-radius:5;-fx-cursor:hand;";
    private static final String BTN_HOVER_STYLE = "-fx-background-color:#2b648c;-fx-text-fill:white;-fx-font-size:12px;-fx-background-radius:5;-fx-cursor:hand;";
    private static final String BTN_ACTIVE_STYLE = "-fx-background-color:#194b75;-fx-text-fill:white;-fx-font-weight:bold;-fx-font-size:12px;-fx-background-radius:5;-fx-cursor:hand;";

    // 导航按钮
    private Button btnQueryAllDoctor;
    private Button btnQueryByDept;
    private Button btnAddAppoint;
    private Button btnMyAppoint;
    private Button btnCancelAppoint;
    private Button btnQueryAllAppoint;
    private Button btnAddDoctor;
    private Button btnUpdateDoctor;
    private Button btnDeleteDoctor;
    private StackPane _contentPane;
    //====【新增】记录当前激活导航按钮，用于高亮
    private Button _activeNavButton;

    //====【新增：分组折叠】分组容器
    private VBox vboxUserGroup;    // 用户操作分组按钮容器
    private VBox vboxAdminGroup;   // 管理员操作分组按钮容器
    private Label lblUserGroupTitle;
    private Label lblAdminGroupTitle;
    private boolean userGroupExpanded = true;
    private boolean adminGroupExpanded = true;

    // ---------------------- 面板控件（TableView） ----------------------
    private VBox panelQueryAllDoctor;
    private TableView<Doctor> tableAllDoctor;
    private VBox panelQueryDept;
    private TextField tfDept;
    private TableView<Doctor> tableDeptDoctor;
    private VBox panelAddAppoint;
    //预约挂号页面新增控件
    private TableView<Doctor> tvAppointDoctor;
    //====【新增】预约挂号选中医生提示Label
    private Label lblSelectedDoctorTip;
    private DatePicker dpAppointDate;
    private ComboBox<String> cbAppointTime;
    private Button btnSubmitAppoint;
    private VBox panelMyAppoint;
    private TableView<Appointment> tableAllMyAppoint; //我的预约：全部记录
    private VBox panelCancelAppoint;
    private TableView<Appointment> tableMyAppoint;     //取消预约：仅待就诊
    private Button btnCancelSelectedAppoint;
    private VBox panelQueryAllAppoint;
    private TableView<Appointment> tableAllAppoint;
    private VBox panelAddDoctor;
    private TextField tfAddDocId;
    private TextField tfAddDocName;
    private TextField tfAddDocDept;
    private TextField tfAddDocTitle;
    private VBox panelUpdateDoctor;
    private TextField tfUpdDocId;
    private TextField tfUpdDocName;
    private TextField tfUpdDocDept;
    private TextField tfUpdDocTitle;
    private VBox panelDeleteDoctor;
    private TableView<Doctor> tvDelDoctor;
    private Button btnDeleteSelectedDoctor;

    public HospitalFrame(String loginUserId, String loginUserRole) {
        this._loginUserId = loginUserId;
        this._loginUserRole = loginUserRole;
        setSpacing(0);
        buildUi();
    }

    private void buildUi() {
        BorderPane root = new BorderPane();
        root.setBackground(new Background(new BackgroundFill(
                new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                        new Stop(0, Color.web("#f4f8fb")),
                        new Stop(1, Color.web("#e8f0f7"))),
                CornerRadii.EMPTY, Insets.EMPTY)));
        HBox topBanner = buildTopBanner();
        BorderPane bodyPane = new BorderPane();
        VBox sideBar = buildSideBar();
        bodyPane.setLeft(sideBar);
        _contentPane = new StackPane();
        _contentPane.setPadding(new Insets(12));
        bodyPane.setCenter(_contentPane);
        VBox mainContainer = new VBox(0, topBanner, bodyPane);
        VBox.setVgrow(bodyPane, Priority.ALWAYS);
        root.setCenter(mainContainer);
        initAllPanels();
        switchPanel(panelQueryAllDoctor, btnQueryAllDoctor);
        this.getChildren().add(root);
        VBox.setVgrow(root, Priority.ALWAYS);
    }

    private HBox buildTopBanner() {
        HBox banner = new HBox();
        banner.setAlignment(Pos.CENTER_LEFT);
        banner.setPadding(new Insets(8, 20, 8, 20));
        banner.setStyle("-fx-background-color:#ffffff;-fx-border-color:#cce0ed;-fx-border-width:0 0 1 0;");
        ImageView logoView = new ImageView();
        java.net.URL logoResource = HospitalFrame.class.getResource("/vcampus/client/view/seu_logo.jpeg");
        if (logoResource != null) {
            Image logoImg = new Image(logoResource.toExternalForm(), 100, 100, true, true, false);
            if (!logoImg.isError()) {
                logoView.setImage(logoImg);
            }
        }
        Label systemTitle = new Label("🏥 医院挂号管理系统");
        systemTitle.setFont(Font.font("System", FontWeight.BOLD, 20));
        systemTitle.setTextFill(Color.web("#194b75"));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label userTopTip = new Label("使用者：" + _loginUserId + " | " + _loginUserRole);
        userTopTip.setFont(Font.font("System", 13));
        userTopTip.setTextFill(Color.web("#446078"));
        banner.getChildren().addAll(logoView, new Region(), systemTitle, spacer, userTopTip);
        HBox.setMargin(systemTitle, new Insets(0, 0, 0, 30));
        return banner;
    }

    private VBox buildSideBar() {
        VBox side = new VBox(10);
        side.setPrefWidth(190);
        side.setPadding(new Insets(18, 10, 10, 10));
        side.setStyle("-fx-background-color: #ffffff;"
                + "-fx-border-color:#cce0ed;"
                + "-fx-border-width:0 1 0 0;"
                + "-fx-effect: dropshadow(gaussian, rgba(80,130,170,0.08),8,0.1,1,0);");
        Label title = new Label("功能导航");
        title.setFont(Font.font("System", FontWeight.BOLD, 16));
        title.setTextFill(Color.web("#194b75"));
        Separator sep1 = new Separator();
        sep1.setPadding(new Insets(8, 0, 8, 0));

        //====【新增：分组折叠】用户操作分组标题（带箭头）
        lblUserGroupTitle = new Label("▼ 用户操作");
        lblUserGroupTitle.setFont(Font.font("System", FontWeight.BOLD, 13));
        lblUserGroupTitle.setTextFill(Color.web("#235782"));
        lblUserGroupTitle.setCursor(Cursor.HAND);
        //用户操作按钮容器
        vboxUserGroup = new VBox(6);
        btnQueryAllDoctor = new Button("查询全部医生");
        btnQueryByDept = new Button("按科室查询医生");
        btnAddAppoint = new Button("预约挂号");
        btnMyAppoint = new Button("我的预约记录");
        btnCancelAppoint = new Button("取消预约");
        vboxUserGroup.getChildren().addAll(btnQueryAllDoctor, btnQueryByDept, btnAddAppoint, btnMyAppoint, btnCancelAppoint);

        //====【新增：分组折叠】管理员操作分组标题（带箭头）
        Separator sep2 = new Separator();
        sep2.setPadding(new Insets(8, 0, 8, 0));
        lblAdminGroupTitle = new Label("▼ 管理员操作");
        lblAdminGroupTitle.setFont(Font.font("System", FontWeight.BOLD, 13));
        lblAdminGroupTitle.setTextFill(Color.web("#235782"));
        lblAdminGroupTitle.setCursor(Cursor.HAND);
        //管理员按钮容器
        vboxAdminGroup = new VBox(6);
        btnQueryAllAppoint = new Button("查询全部预约");
        btnAddDoctor = new Button("新增医生");
        btnUpdateDoctor = new Button("修改医生");
        btnDeleteDoctor = new Button("删除医生");
        vboxAdminGroup.getChildren().addAll(btnQueryAllAppoint, btnAddDoctor, btnUpdateDoctor, btnDeleteDoctor);

        //====【新增：折叠点击事件】点击分组标题切换展开收起
        lblUserGroupTitle.setOnMouseClicked(e->{
            userGroupExpanded = !userGroupExpanded;
            vboxUserGroup.setVisible(userGroupExpanded);
            lblUserGroupTitle.setText(userGroupExpanded ? "▼ 用户操作" : "▶ 用户操作");
        });
        lblAdminGroupTitle.setOnMouseClicked(e->{
            adminGroupExpanded = !adminGroupExpanded;
            vboxAdminGroup.setVisible(adminGroupExpanded);
            lblAdminGroupTitle.setText(adminGroupExpanded ? "▼ 管理员操作" : "▶ 管理员操作");
        });

        Button[] btns = {
                btnQueryAllDoctor, btnQueryByDept, btnAddAppoint, btnMyAppoint, btnCancelAppoint,
                btnQueryAllAppoint, btnAddDoctor, btnUpdateDoctor, btnDeleteDoctor
        };
        for (Button b : btns) {
            b.setMaxWidth(Double.MAX_VALUE);
            b.setPrefHeight(34);
            b.setStyle(BTN_NORMAL_STYLE);
            b.setOnMouseEntered(e -> {
                if(b != _activeNavButton){
                    b.setStyle(BTN_HOVER_STYLE);
                }
            });
            b.setOnMouseExited(e -> {
                if(b != _activeNavButton){
                    b.setStyle(BTN_NORMAL_STYLE);
                }
            });
        }

        // 普通用户隐藏管理员整个分组
        if (!"管理员".equals(_loginUserRole)) {
            lblAdminGroupTitle.setVisible(false);
            vboxAdminGroup.setVisible(false);
            sep2.setVisible(false);
        }

        // 绑定导航切换事件
        btnQueryAllDoctor.setOnAction(e -> switchPanel(panelQueryAllDoctor, btnQueryAllDoctor));
        btnQueryByDept.setOnAction(e -> switchPanel(panelQueryDept, btnQueryByDept));
        btnAddAppoint.setOnAction(e -> switchPanel(panelAddAppoint, btnAddAppoint));
        btnMyAppoint.setOnAction(e -> switchPanel(panelMyAppoint, btnMyAppoint));
        btnCancelAppoint.setOnAction(e -> switchPanel(panelCancelAppoint, btnCancelAppoint));
        btnQueryAllAppoint.setOnAction(e -> switchPanel(panelQueryAllAppoint, btnQueryAllAppoint));
        btnAddDoctor.setOnAction(e -> switchPanel(panelAddDoctor, btnAddDoctor));
        btnUpdateDoctor.setOnAction(e -> switchPanel(panelUpdateDoctor, btnUpdateDoctor));
        btnDeleteDoctor.setOnAction(e -> switchPanel(panelDeleteDoctor, btnDeleteDoctor));

        // 组装侧边栏
        side.getChildren().addAll(
                title, sep1,
                lblUserGroupTitle, vboxUserGroup,
                sep2, lblAdminGroupTitle, vboxAdminGroup
        );
        return side;
    }

    /**
     * 切换面板，切换页面自动执行数据加载 +【新增】导航按钮高亮
     */
    private void switchPanel(VBox panel, Button navButton) {
        //====【新增】清除上一个激活按钮样式
        if(_activeNavButton != null){
            _activeNavButton.setStyle(BTN_NORMAL_STYLE);
        }
        // 设置当前激活按钮
        _activeNavButton = navButton;
        _activeNavButton.setStyle(BTN_ACTIVE_STYLE);

        _contentPane.getChildren().clear();
        _contentPane.getChildren().add(wrapCard(panel));
        // 切换页面自动刷新数据
        if (panel == panelQueryAllDoctor) {
            actionQueryAllDoctor();
        } else if (panel == panelMyAppoint) {
            loadAllMyAppointment();
        } else if (panel == panelQueryAllAppoint) {
            actionQueryAllAppointment();
        }else if(panel == panelAddAppoint){
            loadAppointDoctorTable();
        }else if(panel == panelCancelAppoint){
            loadCancelAbleAppointment();
        }else if(panel == panelDeleteDoctor){
            loadCanDeleteDoctorTable();
        }
    }

    private VBox wrapCard(VBox inner) {
        VBox card = new VBox();
        card.setMaxWidth(Double.MAX_VALUE);
        card.setPadding(new Insets(14, 14, 14, 14));
        card.setStyle("-fx-background-color: #ffffff;"
                + "-fx-background-radius:10;"
                + "-fx-border-radius:10;"
                + "-fx-border-color:#d0e1ec;"
                + "-fx-border-width:1;"
                + "-fx-effect: dropshadow(gaussian, rgba(60,110,150,0.09),10,0.1,0,3);");
        card.getChildren().add(inner);
        return card;
    }

    private void initAllPanels() {
        // ========== 1. 查询全部医生【表格设置CONSTRAINED_RESIZE_POLICY填满宽度，消除右侧空白】 ==========
        panelQueryAllDoctor = new VBox(10);
        panelQueryAllDoctor.setAlignment(Pos.TOP_LEFT);
        panelQueryAllDoctor.setMaxWidth(Double.MAX_VALUE);
        Label lab1 = new Label("医生信息管理 — 查询全部医生");
        lab1.setFont(Font.font("System", FontWeight.BOLD, 17));
        lab1.setTextFill(Color.web("#194b75"));
        tableAllDoctor = new TableView<>();
        tableAllDoctor.setMaxWidth(Double.MAX_VALUE);
        tableAllDoctor.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(tableAllDoctor, Priority.ALWAYS);
        //====【新增】空状态提示
        tableAllDoctor.setPlaceholder(new Label("暂无医生数据"));
        TableColumn<Doctor, String> colDocId = new TableColumn<>("医生编号");
        colDocId.setCellValueFactory(new PropertyValueFactory<>("doctorId"));
        colDocId.setPrefWidth(120);
        TableColumn<Doctor, String> colName = new TableColumn<>("姓名");
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colName.setPrefWidth(100);
        TableColumn<Doctor, String> colDept = new TableColumn<>("科室");
        colDept.setCellValueFactory(new PropertyValueFactory<>("department"));
        colDept.setPrefWidth(100);
        TableColumn<Doctor, String> colTitle = new TableColumn<>("职称");
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colTitle.setPrefWidth(100);
        tableAllDoctor.getColumns().addAll(colDocId, colName, colDept, colTitle);

        //====【新增】查询全部医生选中行，回填修改医生表单
        tableAllDoctor.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if(newVal != null){
                tfUpdDocId.setText(newVal.getDoctorId());
                tfUpdDocName.setText(newVal.getName());
                tfUpdDocDept.setText(newVal.getDepartment());
                tfUpdDocTitle.setText(newVal.getTitle());
            }
        });

        panelQueryAllDoctor.getChildren().addAll(lab1, tableAllDoctor);
        VBox.setVgrow(panelQueryAllDoctor, Priority.ALWAYS);
        // ==========2.按科室查询医生 ==========
        panelQueryDept = new VBox(10);
        panelQueryDept.setAlignment(Pos.TOP_LEFT);
        panelQueryDept.setMaxWidth(Double.MAX_VALUE);
        Label lab2 = new Label("医生信息管理 — 按科室查询");
        lab2.setFont(Font.font("System", FontWeight.BOLD, 17));
        lab2.setTextFill(Color.web("#194b75"));
        tfDept = new TextField();
        tfDept.setPromptText("输入科室名称，例如：内科");
        tfDept.setStyle(fieldStyle());
        tfDept.setMaxWidth(Double.MAX_VALUE);
        Button btnQDept = new Button("查询");
        btnQDept.setOnAction(e -> actionQueryDoctorByDept());
        tableDeptDoctor = new TableView<>();
        tableDeptDoctor.setMaxWidth(Double.MAX_VALUE);
        tableDeptDoctor.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(tableDeptDoctor, Priority.ALWAYS);
        //====【新增】空状态提示
        tableDeptDoctor.setPlaceholder(new Label("未找到该科室医生"));
        TableColumn<Doctor, String> c1 = new TableColumn<>("医生编号");
        c1.setCellValueFactory(new PropertyValueFactory<>("doctorId"));
        c1.setPrefWidth(120);
        TableColumn<Doctor, String> c2 = new TableColumn<>("姓名");
        c2.setCellValueFactory(new PropertyValueFactory<>("name"));
        c2.setPrefWidth(100);
        TableColumn<Doctor, String> c3 = new TableColumn<>("科室");
        c3.setCellValueFactory(new PropertyValueFactory<>("department"));
        c3.setPrefWidth(100);
        TableColumn<Doctor, String> c4 = new TableColumn<>("职称");
        c4.setCellValueFactory(new PropertyValueFactory<>("title"));
        c4.setPrefWidth(100);
        tableDeptDoctor.getColumns().addAll(c1, c2, c3, c4);
        panelQueryDept.getChildren().addAll(lab2, tfDept, btnQDept, tableDeptDoctor);
        VBox.setVgrow(panelQueryDept, Priority.ALWAYS);
        // ==========3.预约挂号【去掉弹窗 +【新增】选中医生提示】 ==========
        panelAddAppoint = new VBox(10);
        panelAddAppoint.setAlignment(Pos.TOP_LEFT);
        panelAddAppoint.setMaxWidth(Double.MAX_VALUE);
        Label lab3 = new Label("预约挂号 — 新建就诊预约");
        lab3.setFont(Font.font("System", FontWeight.BOLD, 17));
        lab3.setTextFill(Color.web("#194b75"));
        Label tipAppoint = new Label("在下方表格选中一位医生，再选择日期与就诊时段，点击确认预约");
        tipAppoint.setFont(Font.font("System", 13));
        tipAppoint.setTextFill(Color.web("#555555"));

        //====【新增】选中医生提示
        lblSelectedDoctorTip = new Label("✅尚未选择医生");
        lblSelectedDoctorTip.setFont(Font.font("System",13));
        lblSelectedDoctorTip.setTextFill(Color.web("#c0392b"));

        tvAppointDoctor = new TableView<>();
        tvAppointDoctor.setMaxWidth(Double.MAX_VALUE);
        tvAppointDoctor.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(tvAppointDoctor, Priority.ALWAYS);
        //====【新增】空状态提示
        tvAppointDoctor.setPlaceholder(new Label("暂无可预约医生"));
        TableColumn<Doctor, String> colADocId = new TableColumn<>("医生编号");
        colADocId.setCellValueFactory(new PropertyValueFactory<>("doctorId"));
        colADocId.setPrefWidth(120);
        TableColumn<Doctor, String> colAName = new TableColumn<>("姓名");
        colAName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colAName.setPrefWidth(100);
        TableColumn<Doctor, String> colADept = new TableColumn<>("科室");
        colADept.setCellValueFactory(new PropertyValueFactory<>("department"));
        colADept.setPrefWidth(100);
        TableColumn<Doctor, String> colATitle = new TableColumn<>("职称");
        colATitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colATitle.setPrefWidth(100);
        tvAppointDoctor.getColumns().addAll(colADocId, colAName, colADept, colATitle);

        //====【新增】监听表格选中行，更新提示文字
        tvAppointDoctor.getSelectionModel().selectedItemProperty().addListener((obs, oldDoc, newDoc) -> {
            if(newDoc != null){
                lblSelectedDoctorTip.setText("✅已选择医生："+newDoc.getDoctorId()+" | "+newDoc.getName()+" | "+newDoc.getDepartment());
                lblSelectedDoctorTip.setTextFill(Color.web("#27ae60"));
            }else{
                lblSelectedDoctorTip.setText("✅尚未选择医生");
                lblSelectedDoctorTip.setTextFill(Color.web("#c0392b"));
            }
        });

        dpAppointDate = new DatePicker(LocalDate.now());
        dpAppointDate.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if(!empty && date.isBefore(LocalDate.now())){
                    setDisable(true);
                    setStyle("-fx-background-color:#dddddd;");
                }
            }
        });
        cbAppointTime = new ComboBox<>();
        cbAppointTime.getItems().addAll("08:30", "09:00", "09:30", "10:00", "10:30", "14:00", "14:30", "15:00", "15:30");
        cbAppointTime.setPromptText("选择就诊时刻");
        HBox hbRow = new HBox(15, new Label("就诊日期："), dpAppointDate, new Label("就诊时段："), cbAppointTime);
        hbRow.setAlignment(Pos.CENTER_LEFT);
        btnSubmitAppoint = new Button("确认预约");
        btnSubmitAppoint.setOnAction(e -> doSubmitAppoint());
        panelAddAppoint.getChildren().addAll(lab3, tipAppoint, lblSelectedDoctorTip, tvAppointDoctor, hbRow, btnSubmitAppoint);
        VBox.setVgrow(panelAddAppoint, Priority.ALWAYS);
        // ==========4.我的预约记录【展示全部状态】 ==========
        panelMyAppoint = new VBox(10);
        panelMyAppoint.setAlignment(Pos.TOP_LEFT);
        panelMyAppoint.setMaxWidth(Double.MAX_VALUE);
        Label lab4 = new Label("预约记录 — 我的就诊预约");
        lab4.setFont(Font.font("System", FontWeight.BOLD, 17));
        lab4.setTextFill(Color.web("#194b75"));
        tableAllMyAppoint = new TableView<>();
        tableAllMyAppoint.setMaxWidth(Double.MAX_VALUE);
        tableAllMyAppoint.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(tableAllMyAppoint, Priority.ALWAYS);
        //====【新增】空状态提示
        tableAllMyAppoint.setPlaceholder(new Label("暂无我的预约记录"));
        TableColumn<Appointment, String> aid = new TableColumn<>("预约编号");
        aid.setCellValueFactory(new PropertyValueFactory<>("appointmentId"));
        aid.setPrefWidth(130);
        TableColumn<Appointment, String> uid = new TableColumn<>("用户ID");
        uid.setCellValueFactory(new PropertyValueFactory<>("userId"));
        uid.setPrefWidth(100);
        TableColumn<Appointment, String> did = new TableColumn<>("医生ID");
        did.setCellValueFactory(new PropertyValueFactory<>("doctorId"));
        did.setPrefWidth(100);
        TableColumn<Appointment, Date> atime = new TableColumn<>("预约时间");
        atime.setCellValueFactory(new PropertyValueFactory<>("appointmentTime"));
        atime.setPrefWidth(170);
        TableColumn<Appointment, String> stat = new TableColumn<>("状态");
        stat.setCellValueFactory(new PropertyValueFactory<>("status"));
        stat.setPrefWidth(110);
        stat.setCellFactory(col -> new TableCell<Appointment, String>() {
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                if (empty) {
                    setText(null);
                } else if (value == null || value.isBlank()) {
                    setText("待就诊");
                } else {
                    setText(value);
                }
            }
        });
        tableAllMyAppoint.getColumns().addAll(aid, uid, did, atime, stat);
        panelMyAppoint.getChildren().addAll(lab4, tableAllMyAppoint);
        VBox.setVgrow(panelMyAppoint, Priority.ALWAYS);
        // ==========5.取消预约【只加载待就诊】 ==========
        panelCancelAppoint = new VBox(10);
        panelCancelAppoint.setAlignment(Pos.TOP_LEFT);
        panelCancelAppoint.setMaxWidth(Double.MAX_VALUE);
        Label lab5 = new Label("预约操作 — 取消就诊预约");
        lab5.setFont(Font.font("System", FontWeight.BOLD, 17));
        lab5.setTextFill(Color.web("#194b75"));
        Label tipCancel = new Label("在表格选中本人【待就诊】预约记录，点击下方按钮执行取消");
        tipCancel.setFont(Font.font("System", 13));
        tipCancel.setTextFill(Color.web("#555555"));
        tableMyAppoint = new TableView<>();
        tableMyAppoint.setMaxWidth(Double.MAX_VALUE);
        tableMyAppoint.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(tableMyAppoint, Priority.ALWAYS);
        //====【新增】空状态提示
        tableMyAppoint.setPlaceholder(new Label("没有可取消的待就诊预约记录"));
        TableColumn<Appointment, String> cAid = new TableColumn<>("预约编号");
        cAid.setCellValueFactory(new PropertyValueFactory<>("appointmentId"));
        cAid.setPrefWidth(130);
        TableColumn<Appointment, String> cUid = new TableColumn<>("用户ID");
        cUid.setCellValueFactory(new PropertyValueFactory<>("userId"));
        cUid.setPrefWidth(100);
        TableColumn<Appointment, String> cDid = new TableColumn<>("医生ID");
        cDid.setCellValueFactory(new PropertyValueFactory<>("doctorId"));
        cDid.setPrefWidth(100);
        TableColumn<Appointment, Date> cTime = new TableColumn<>("预约时间");
        cTime.setCellValueFactory(new PropertyValueFactory<>("appointmentTime"));
        cTime.setPrefWidth(170);
        TableColumn<Appointment, String> cStat = new TableColumn<>("状态");
        cStat.setCellValueFactory(new PropertyValueFactory<>("status"));
        cStat.setPrefWidth(110);
        cStat.setCellFactory(col -> new TableCell<Appointment, String>() {
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                if (empty) setText(null);
                else if(value==null||value.isBlank()) setText("待就诊");
                else setText(value);
            }
        });
        tableMyAppoint.getColumns().addAll(cAid,cUid,cDid,cTime,cStat);
        btnCancelSelectedAppoint = new Button("取消选中的预约");
        btnCancelSelectedAppoint.setOnAction(e -> doCancelSelected());
        panelCancelAppoint.getChildren().addAll(lab5, tipCancel, tableMyAppoint, btnCancelSelectedAppoint);
        VBox.setVgrow(panelCancelAppoint, Priority.ALWAYS);
        // ==========管理员：全部预约表格 ==========
        panelQueryAllAppoint = new VBox(10);
        panelQueryAllAppoint.setAlignment(Pos.TOP_LEFT);
        panelQueryAllAppoint.setMaxWidth(Double.MAX_VALUE);
        Label labA1 = new Label("【管理员】全部就诊预约记录");
        labA1.setFont(Font.font("System", FontWeight.BOLD, 17));
        labA1.setTextFill(Color.web("#194b75"));
        tableAllAppoint = new TableView<>();
        tableAllAppoint.setMaxWidth(Double.MAX_VALUE);
        tableAllAppoint.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(tableAllAppoint, Priority.ALWAYS);
        //====【新增】空状态提示
        tableAllAppoint.setPlaceholder(new Label("暂无预约数据"));
        TableColumn<Appointment, String> aa = new TableColumn<>("预约编号");
        aa.setCellValueFactory(new PropertyValueFactory<>("appointmentId"));
        aa.setPrefWidth(130);
        TableColumn<Appointment, String> uu = new TableColumn<>("用户ID");
        uu.setCellValueFactory(new PropertyValueFactory<>("userId"));
        uu.setPrefWidth(100);
        TableColumn<Appointment, String> dd = new TableColumn<>("医生ID");
        dd.setCellValueFactory(new PropertyValueFactory<>("doctorId"));
        dd.setPrefWidth(100);
        TableColumn<Appointment, Date> tt = new TableColumn<>("预约时间");
        tt.setCellValueFactory(new PropertyValueFactory<>("appointmentTime"));
        tt.setPrefWidth(170);
        TableColumn<Appointment, String> ss = new TableColumn<>("状态");
        ss.setCellValueFactory(new PropertyValueFactory<>("status"));
        ss.setPrefWidth(110);
        ss.setCellFactory(col -> new TableCell<Appointment, String>() {
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                if (empty) {
                    setText(null);
                } else if (value == null || value.isBlank()) {
                    setText("待就诊");
                } else {
                    setText(value);
                }
            }
        });
        tableAllAppoint.getColumns().addAll(aa, uu, dd, tt, ss);
        TableColumn<Appointment, Void> colOpt = new TableColumn<>("操作");
        colOpt.setPrefWidth(100);
        colOpt.setCellFactory(param -> new TableCell<Appointment, Void>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Appointment appoint = getTableView().getItems().get(getIndex());
                    String status = appoint.getStatus();
                    if ("已就诊".equals(status) || "已取消".equals(status)) {
                        Button btnDel = new Button("删除");
                        btnDel.setStyle("-fx-background-color:#dc3545;-fx-text-fill:white;-fx-font-size:11px;-fx-background-radius:4px;");
                        btnDel.setOnAction(e -> {
                            Appointment rowData = getTableView().getItems().get(getIndex());
                            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                            confirm.setTitle("确认删除");
                            confirm.setContentText("确定要永久删除这条记录吗？该操作不可恢复！");
                            Optional<ButtonType> res = confirm.showAndWait();
                            if (res.isPresent() && res.get() == ButtonType.OK) {
                                String aid = rowData.getAppointmentId();
                                new Thread(() -> {
                                    try {
                                        Message resp = _hospitalSrv.deleteCancelAppointment(aid);
                                        Platform.runLater(() -> {
                                            if (IConstant.STATUS_SUCCESS.equals(resp.getStatusCode())) {
                                                showAlert(Alert.AlertType.INFORMATION, "成功", String.valueOf(resp.getData()));
                                                actionQueryAllAppointment();
                                            } else {
                                                showAlert(Alert.AlertType.ERROR, "失败", String.valueOf(resp.getData()));
                                            }
                                        });
                                    } catch (Exception ex) {
                                        Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "异常", ex.getMessage()));
                                    }
                                }).start();
                            }
                        });
                        setGraphic(btnDel);
                    } else {
                        setGraphic(null);
                    }
                }
            }
        });
        tableAllAppoint.getColumns().add(colOpt);
        panelQueryAllAppoint.getChildren().addAll(labA1, tableAllAppoint);
        VBox.setVgrow(panelQueryAllAppoint, Priority.ALWAYS);
        // ==========管理员新增医生 ==========
        panelAddDoctor = new VBox(10);
        panelAddDoctor.setAlignment(Pos.TOP_LEFT);
        panelAddDoctor.setMaxWidth(Double.MAX_VALUE);
        Label labA2 = new Label("【管理员】医生信息 — 新增医生");
        labA2.setFont(Font.font("System", FontWeight.BOLD, 17));
        labA2.setTextFill(Color.web("#194b75"));
        tfAddDocId = new TextField();
        tfAddDocId.setPromptText("医生ID");
        tfAddDocId.setStyle(fieldStyle());
        tfAddDocId.setMaxWidth(Double.MAX_VALUE);
        tfAddDocName = new TextField();
        tfAddDocName.setPromptText("姓名");
        tfAddDocName.setStyle(fieldStyle());
        tfAddDocName.setMaxWidth(Double.MAX_VALUE);
        tfAddDocDept = new TextField();
        tfAddDocDept.setPromptText("科室");
        tfAddDocDept.setStyle(fieldStyle());
        tfAddDocDept.setMaxWidth(Double.MAX_VALUE);
        tfAddDocTitle = new TextField();
        tfAddDocTitle.setPromptText("职称");
        tfAddDocTitle.setStyle(fieldStyle());
        tfAddDocTitle.setMaxWidth(Double.MAX_VALUE);
        Button btnAddDoc = new Button("提交新增");
        btnAddDoc.setOnAction(e -> actionAddDoctor());
        panelAddDoctor.getChildren().addAll(labA2, tfAddDocId, tfAddDocName, tfAddDocDept, tfAddDocTitle, btnAddDoc);
        VBox.setVgrow(panelAddDoctor, Priority.ALWAYS);
        // ==========管理员修改医生 ==========
        panelUpdateDoctor = new VBox(10);
        panelUpdateDoctor.setAlignment(Pos.TOP_LEFT);
        panelUpdateDoctor.setMaxWidth(Double.MAX_VALUE);
        Label labA3 = new Label("【管理员】医生信息 — 修改医生");
        labA3.setFont(Font.font("System", FontWeight.BOLD, 17));
        labA3.setTextFill(Color.web("#194b75"));
        //====【新增】提示文字，告诉用户：去查询全部医生页面选中行自动回填
        Label tipUpdate = new Label("💡提示：切换到【查询全部医生】页面选中表格行，本表单会自动回填医生信息");
        tipUpdate.setFont(Font.font("System",12));
        tipUpdate.setTextFill(Color.web("#34495e"));

        tfUpdDocId = new TextField();
        tfUpdDocId.setPromptText("要修改的医生ID");
        tfUpdDocId.setStyle(fieldStyle());
        tfUpdDocId.setMaxWidth(Double.MAX_VALUE);
        tfUpdDocName = new TextField();
        tfUpdDocName.setPromptText("新姓名");
        tfUpdDocName.setStyle(fieldStyle());
        tfUpdDocName.setMaxWidth(Double.MAX_VALUE);
        tfUpdDocDept = new TextField();
        tfUpdDocDept.setPromptText("新科室");
        tfUpdDocDept.setStyle(fieldStyle());
        tfUpdDocDept.setMaxWidth(Double.MAX_VALUE);
        tfUpdDocTitle = new TextField();
        tfUpdDocTitle.setPromptText("新职称");
        tfUpdDocTitle.setStyle(fieldStyle());
        tfUpdDocTitle.setMaxWidth(Double.MAX_VALUE);
        Button btnUpdDoc = new Button("提交修改");
        btnUpdDoc.setOnAction(e -> actionUpdateDoctor());
        panelUpdateDoctor.getChildren().addAll(labA3, tipUpdate, tfUpdDocId, tfUpdDocName, tfUpdDocDept, tfUpdDocTitle, btnUpdDoc);
        VBox.setVgrow(panelUpdateDoctor, Priority.ALWAYS);
        // ==========管理员删除医生【去掉弹窗】 ==========
        panelDeleteDoctor = new VBox(10);
        panelDeleteDoctor.setAlignment(Pos.TOP_LEFT);
        panelDeleteDoctor.setMaxWidth(Double.MAX_VALUE);
        Label labA4 = new Label("【管理员】医生信息 — 删除医生");
        labA4.setFont(Font.font("System", FontWeight.BOLD, 17));
        labA4.setTextFill(Color.web("#194b75"));
        Label tipDel = new Label("表格仅展示完全没有任何预约记录的医生；选中一行，点击按钮删除");
        tipDel.setFont(Font.font("System", 13));
        tipDel.setTextFill(Color.web("#555555"));
        tvDelDoctor = new TableView<>();
        tvDelDoctor.setMaxWidth(Double.MAX_VALUE);
        tvDelDoctor.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(tvDelDoctor, Priority.ALWAYS);
        //====【新增】空状态提示
        tvDelDoctor.setPlaceholder(new Label("暂无可删除的医生（该医生存在关联预约记录则不可删除）"));
        TableColumn<Doctor, String> colDDocId = new TableColumn<>("医生编号");
        colDDocId.setCellValueFactory(new PropertyValueFactory<>("doctorId"));
        colDDocId.setPrefWidth(120);
        TableColumn<Doctor, String> colDName = new TableColumn<>("姓名");
        colDName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colDName.setPrefWidth(100);
        TableColumn<Doctor, String> colDDept = new TableColumn<>("科室");
        colDDept.setCellValueFactory(new PropertyValueFactory<>("department"));
        colDDept.setPrefWidth(100);
        TableColumn<Doctor, String> colDTitle = new TableColumn<>("职称");
        colDTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colDTitle.setPrefWidth(100);
        tvDelDoctor.getColumns().addAll(colDDocId, colDName, colDDept, colDTitle);
        btnDeleteSelectedDoctor = new Button("删除选中医生");
        btnDeleteSelectedDoctor.setOnAction(e -> doDeleteSelectedDoctor());
        panelDeleteDoctor.getChildren().addAll(labA4, tipDel, tvDelDoctor, btnDeleteSelectedDoctor);
        VBox.setVgrow(panelDeleteDoctor, Priority.ALWAYS);
    }

    private String fieldStyle() {
        return "-fx-background-radius:6;"
                + "-fx-border-radius:6;"
                + "-fx-border-color:#b8cddb;"
                + "-fx-border-width:1;"
                + "-fx-background-color: #ffffff;"
                + "-fx-padding: 0 12 0 12;"
                + "-fx-font-size:14px;";
    }

    //==================== 新增：预约挂号提交（无弹窗）====================
    private void doSubmitAppoint(){
        Doctor selectedDoc = tvAppointDoctor.getSelectionModel().getSelectedItem();
        LocalDate selectDate = dpAppointDate.getValue();
        String timeStr = cbAppointTime.getValue();
        if(selectedDoc == null){
            showAlert(Alert.AlertType.WARNING,"提示","请先在表格选中一位医生！");
            return;
        }
        if(selectDate == null){
            showAlert(Alert.AlertType.WARNING,"提示","请选择就诊日期！");
            return;
        }
        if(selectDate.isBefore(LocalDate.now())){
            showAlert(Alert.AlertType.WARNING,"预约失败","无法选择过去的时间");
            return;
        }
        if(timeStr == null || timeStr.isBlank()){
            showAlert(Alert.AlertType.WARNING,"提示","请选择就诊时段！");
            return;
        }
        LocalDateTime localDateTime = LocalDateTime.of(selectDate, LocalTime.parse(timeStr));
        Date appointDate = Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
        Appointment appoint = new Appointment();
        appoint.setDoctorId(selectedDoc.getDoctorId());
        appoint.setUserId(_loginUserId);
        appoint.setAppointmentTime(appointDate);
        new Thread(() -> {
            try {
                Message resp = _hospitalSrv.addAppointment(appoint);
                Platform.runLater(() -> {
                    if (IConstant.STATUS_SUCCESS.equals(resp.getStatusCode())) {
                        showAlert(Alert.AlertType.INFORMATION, "预约成功", String.valueOf(resp.getData()));
                        loadAppointDoctorTable();
                    } else {
                        showAlert(Alert.AlertType.ERROR, "预约失败", String.valueOf(resp.getData()));
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "异常", e.getMessage()));
            }
        }).start();
    }

    private void loadAppointDoctorTable(){
        new Thread(()->{
            try {
                Message resp = _hospitalSrv.queryAllDoctor();
                Platform.runLater(()->{
                    if(IConstant.STATUS_SUCCESS.equals(resp.getStatusCode())){
                        List<Doctor> list = (List<Doctor>)resp.getData();
                        tvAppointDoctor.getItems().clear();
                        tvAppointDoctor.getItems().addAll(list);
                        //重置选中提示
                        tvAppointDoctor.getSelectionModel().clearSelection();
                    }
                });
            }catch (Exception ex){
                Platform.runLater(()->showAlert(Alert.AlertType.ERROR,"加载医生失败",ex.getMessage()));
            }
        }).start();
    }

    //【我的预约记录】加载全部状态
    private void loadAllMyAppointment(){
        new Thread(() -> {
            try {
                Message resp = _hospitalSrv.queryMyAppointment(_loginUserId);
                Platform.runLater(() -> {
                    if(IConstant.STATUS_SUCCESS.equals(resp.getStatusCode())){
                        List<Appointment> list = (List<Appointment>) resp.getData();
                        tableAllMyAppoint.getItems().setAll(list);
                    }
                });
            }catch (Exception ex){
                Platform.runLater(()->showAlert(Alert.AlertType.ERROR,"加载我的预约失败",ex.getMessage()));
            }
        }).start();
    }

    // 【取消预约页面】只加载待就诊
    private void loadCancelAbleAppointment(){
        new Thread(() -> {
            try {
                Message resp = _hospitalSrv.queryMyAppointment(_loginUserId);
                Platform.runLater(() -> {
                    if(IConstant.STATUS_SUCCESS.equals(resp.getStatusCode())){
                        List<Appointment> all = (List<Appointment>) resp.getData();
                        List<Appointment> canCancel = all.stream()
                                .filter(a->"待就诊".equals(a.getStatus()))
                                .collect(Collectors.toList());
                        tableMyAppoint.getItems().setAll(canCancel);
                    }
                });
            }catch (Exception ex){
                Platform.runLater(()->showAlert(Alert.AlertType.ERROR,"加载可取消预约失败",ex.getMessage()));
            }
        }).start();
    }

    //==================== 新增：取消选中预约 ====================
    private void doCancelSelected(){
        Appointment apt = tableMyAppoint.getSelectionModel().getSelectedItem();
        if(apt == null){
            showAlert(Alert.AlertType.WARNING,"提示","请先选中一条预约记录！");
            return;
        }
        new Thread(() -> {
            try {
                Message resp = _hospitalSrv.cancelAppointment(_loginUserId, apt.getAppointmentId());
                Platform.runLater(() -> {
                    if (IConstant.STATUS_SUCCESS.equals(resp.getStatusCode())) {
                        showAlert(Alert.AlertType.INFORMATION, "取消成功", String.valueOf(resp.getData()));
                        loadCancelAbleAppointment();
                    } else {
                        showAlert(Alert.AlertType.ERROR, "取消失败", String.valueOf(resp.getData()));
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "异常", e.getMessage()));
            }
        }).start();
    }

    //==================== 新增：删除选中医生 ====================
    private void loadCanDeleteDoctorTable(){
        new Thread(()->{
            try {
                Message msg = _hospitalSrv.queryCanDeleteDoctor();
                Platform.runLater(()->{
                    if(IConstant.STATUS_SUCCESS.equals(msg.getStatusCode())){
                        List<Doctor> docList = (List<Doctor>) msg.getData();
                        tvDelDoctor.getItems().clear();
                        tvDelDoctor.getItems().addAll(docList);
                    }else{
                        showAlert(Alert.AlertType.ERROR, "获取医生列表失败", String.valueOf(msg.getData()));
                    }
                });
            }catch (Exception e){
                Platform.runLater(()->showAlert(Alert.AlertType.ERROR,"加载可删除医生失败",e.getMessage()));
            }
        }).start();
    }

    private void doDeleteSelectedDoctor(){
        Doctor doc = tvDelDoctor.getSelectionModel().getSelectedItem();
        if(doc == null){
            showAlert(Alert.AlertType.WARNING,"提示","请先选中要删除的医生！");
            return;
        }
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("删除确认");
        confirmAlert.setContentText("确定要删除医生【" + doc.getDoctorId() + " " + doc.getName() + "】吗？该操作不可恢复！");
        Optional<ButtonType> res = confirmAlert.showAndWait();
        if(res.isEmpty() || res.get() != ButtonType.OK){
            return;
        }
        new Thread(() -> {
            try {
                Message resp = _hospitalSrv.deleteDoctor(_loginUserId, doc.getDoctorId());
                Platform.runLater(() -> {
                    if (IConstant.STATUS_SUCCESS.equals(resp.getStatusCode())) {
                        showAlert(Alert.AlertType.INFORMATION, "成功", String.valueOf(resp.getData()));
                        loadCanDeleteDoctorTable();
                    } else {
                        showAlert(Alert.AlertType.ERROR, "业务失败", String.valueOf(resp.getData()));
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "异常", e.getMessage()));
            }
        }).start();
    }

    // ============ 原有弹窗方法保留，不再调用 ============
    private Optional<Appointment> showAppointSelectDialog() { return Optional.empty(); }
    private Optional<String> showSelectMyAppointDialog() { return Optional.empty(); }
    private Optional<String> showSelectDeleteDoctorDialog() { return Optional.empty(); }

    // ============原有业务方法保留============
    private void actionQueryAllDoctor() {
        new Thread(() -> {
            try {
                Message resp = _hospitalSrv.queryAllDoctor();
                if (resp == null) {
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "错误", "服务器返回空消息"));
                    return;
                }
                Platform.runLater(() -> {
                    if (IConstant.STATUS_SUCCESS.equals(resp.getStatusCode())) {
                        Object rawData = resp.getData();
                        if (rawData instanceof List<?>) {
                            List<Doctor> list = (List<Doctor>) rawData;
                            tableAllDoctor.getItems().clear();
                            tableAllDoctor.getItems().addAll(list);
                        }
                    } else {
                        showAlert(Alert.AlertType.ERROR, "业务失败", String.valueOf(resp.getData()));
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "异常", e.getMessage()));
            }
        }).start();
    }

    private void actionQueryDoctorByDept() {
        String dept = tfDept.getText().trim();
        if (dept.isBlank()) {
            showAlert(Alert.AlertType.WARNING, "提示", "请填写科室名称");
            return;
        }
        new Thread(() -> {
            try {
                Message resp = _hospitalSrv.queryDoctorByDept(dept);
                Platform.runLater(() -> {
                    if (IConstant.STATUS_SUCCESS.equals(resp.getStatusCode())) {
                        Object rawData = resp.getData();
                        if (rawData instanceof List<?>) {
                            List<Doctor> list = (List<Doctor>) rawData;
                            tableDeptDoctor.getItems().clear();
                            tableDeptDoctor.getItems().addAll(list);
                        }
                    } else {
                        showAlert(Alert.AlertType.ERROR, "业务失败", String.valueOf(resp.getData()));
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "异常", e.getMessage()));
            }
        }).start();
    }

    private void actionAddAppointment() {}
    private void actionQueryMyAppointment(){}
    private void actionCancelAppointment() {}

    private void actionQueryAllAppointment() {
        new Thread(() -> {
            try {
                Message resp = _hospitalSrv.queryAllAppointment(_loginUserId);
                Platform.runLater(() -> {
                    if (IConstant.STATUS_SUCCESS.equals(resp.getStatusCode())) {
                        Object rawData = resp.getData();
                        if (rawData instanceof List<?>) {
                            List<Appointment> list = (List<Appointment>) rawData;
                            tableAllAppoint.getItems().clear();
                            tableAllAppoint.getItems().addAll(list);
                        }
                    } else {
                        showAlert(Alert.AlertType.ERROR, "业务失败", String.valueOf(resp.getData()));
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "异常", e.getMessage()));
            }
        }).start();
    }

    private void actionAddDoctor() {
        String id = tfAddDocId.getText().trim();
        String name = tfAddDocName.getText().trim();
        String dept = tfAddDocDept.getText().trim();
        String title = tfAddDocTitle.getText().trim();
        if (id.isBlank() || name.isBlank() || dept.isBlank() || title.isBlank()) {
            showAlert(Alert.AlertType.WARNING, "提示", "全部字段不能为空");
            return;
        }
        Doctor doc = new Doctor();
        doc.setDoctorId(id);
        doc.setName(name);
        doc.setDepartment(dept);
        doc.setTitle(title);
        new Thread(() -> {
            try {
                Message resp = _hospitalSrv.addDoctor(_loginUserId, doc);
                Platform.runLater(() -> {
                    if (IConstant.STATUS_SUCCESS.equals(resp.getStatusCode())) {
                        showAlert(Alert.AlertType.INFORMATION, "成功", String.valueOf(resp.getData()));
                    } else {
                        showAlert(Alert.AlertType.ERROR, "业务失败", String.valueOf(resp.getData()));
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "异常", e.getMessage()));
            }
        }).start();
    }

    private void actionUpdateDoctor() {
        String id = tfUpdDocId.getText().trim();
        String name = tfUpdDocName.getText().trim();
        String dept = tfUpdDocDept.getText().trim();
        String title = tfUpdDocTitle.getText().trim();
        if (id.isBlank()) {
            showAlert(Alert.AlertType.WARNING, "提示", "医生ID不能为空");
            return;
        }
        Doctor doc = new Doctor();
        doc.setDoctorId(id);
        doc.setName(name);
        doc.setDepartment(dept);
        doc.setTitle(title);
        new Thread(() -> {
            try {
                Message resp = _hospitalSrv.updateDoctor(_loginUserId, doc);
                Platform.runLater(() -> {
                    if (IConstant.STATUS_SUCCESS.equals(resp.getStatusCode())) {
                        showAlert(Alert.AlertType.INFORMATION, "成功", String.valueOf(resp.getData()));
                    } else {
                        showAlert(Alert.AlertType.ERROR, "业务失败", String.valueOf(resp.getData()));
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "异常", e.getMessage()));
            }
        }).start();
    }

    private void actionDeleteDoctor() {}

    private void showAlert(Alert.AlertType alertType, String title, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
