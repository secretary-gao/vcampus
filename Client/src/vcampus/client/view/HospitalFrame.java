/*
 * HospitalFrame
 *
 * Version 3.1 新增【健康教育】数据库文章浏览功能；表格UI美化；角色动态主题配色；前端时段禁用
 *
 * 2026-09-14
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.client.view;
import vcampus.client.biz.HospitalClientSrv;
import vcampus.common.constant.IConstant;
import vcampus.common.vo.Appointment;
import vcampus.common.vo.Doctor;
import vcampus.common.vo.HealthArticle;
import vcampus.common.vo.Message;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import javafx.scene.image.ImageView;
import javafx.scene.Cursor;

public class HospitalFrame extends VBox {
    private final String _loginUserId;
    private final String _loginUserRole;
    private final HospitalClientSrv _hospitalSrv = new HospitalClientSrv();
    //====【角色动态主题颜色】====
    private final String themePrimaryColor;
    private final String themeHoverColor;
    private final String themeActiveColor;
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
    //=====新增 健康教育按钮=====
    private Button btnHealthArticle;

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
    private Label lblSelectedDoctorTip;
    private DatePicker dpAppointDate;
    private ComboBox<String> cbAppointTime;
    private Button btnSubmitAppoint;
    //========【前端预约时段禁用新增成员变量】========
    private Map<LocalDate, Set<String>> dateDisableTimeMap;
    private Set<String> currentDisableSet;
    private final ObservableList<String> allTimeOptions;

    private VBox panelMyAppoint;
    private TableView<Appointment> tableAllMyAppoint;
    private VBox panelCancelAppoint;
    private TableView<Appointment> tableMyAppoint;
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

    //====================健康教育页面控件====================
    private VBox panelHealthArticle;
    private ListView<HealthArticle> lvArticleList;
    private TextArea taArticleContent;
    private ObservableList<HealthArticle> obsArticleList;

    public HospitalFrame(String loginUserId, String loginUserRole) {
        this._loginUserId = loginUserId;
        this._loginUserRole = loginUserRole;
        //========初始化预约时段禁用相关集合========
        dateDisableTimeMap = new HashMap<>();
        currentDisableSet = new HashSet<>();
        allTimeOptions = FXCollections.observableArrayList(
                "08:30", "09:00", "09:30", "10:00", "10:30",
                "14:00", "14:30", "15:00", "15:30"
        );
        obsArticleList = FXCollections.observableArrayList();

        // 根据角色分配主题色
        switch (_loginUserRole) {
            case "管理员":
                themePrimaryColor = "#94b55a";   // 主色：参考图浅绿黄
                themeHoverColor = "#7f9f4d";     // 悬浮：深一点
                themeActiveColor = "#6b8a40";
                break;
            case "教师":
                themePrimaryColor = "#94b55a";   // 主色：参考图浅绿黄
                themeHoverColor = "#7f9f4d";     // 悬浮：深一点
                themeActiveColor = "#6b8a40";
                break;
            case "学生":
            default:
                themePrimaryColor = "#77b55a";   // 主色：参考图浅绿
                themeHoverColor = "#7f9f4d";     // 悬浮：深一点
                themeActiveColor = "#6b8a40";
                break;
        }
        setSpacing(0);
        buildUi();
    }

    private void buildUi() {
        BorderPane root = new BorderPane();
        root.setStyle("""
                .table-view-clean .table-row-cell:hover {
                    -fx-background-color:#e8f2fc;
                }
                .table-view-clean .table-row-cell:selected{
                    -fx-background-color:#c5e0fa;
                }
                """);
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
        systemTitle.setTextFill(Color.web(themePrimaryColor));
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
        title.setTextFill(Color.web(themePrimaryColor));
        Separator sep1 = new Separator();
        sep1.setPadding(new Insets(8, 0, 8, 0));

        lblUserGroupTitle = new Label("▼ 用户操作");
        lblUserGroupTitle.setFont(Font.font("System", FontWeight.BOLD, 13));
        lblUserGroupTitle.setTextFill(Color.web(themePrimaryColor));
        lblUserGroupTitle.setCursor(Cursor.HAND);
        vboxUserGroup = new VBox(6);
        btnQueryAllDoctor = new Button("查询全部医生");
        btnQueryByDept = new Button("按科室查询医生");
        btnAddAppoint = new Button("预约挂号");
        btnMyAppoint = new Button("我的预约记录");
        btnCancelAppoint = new Button("取消预约");
        btnHealthArticle = new Button("健康教育");
        vboxUserGroup.getChildren().addAll(btnQueryAllDoctor, btnQueryByDept, btnAddAppoint, btnMyAppoint, btnCancelAppoint, btnHealthArticle);

        Separator sep2 = new Separator();
        sep2.setPadding(new Insets(8, 0, 8, 0));
        lblAdminGroupTitle = new Label("▼ 管理员操作");
        lblAdminGroupTitle.setFont(Font.font("System", FontWeight.BOLD, 13));
        lblAdminGroupTitle.setTextFill(Color.web(themePrimaryColor));
        lblAdminGroupTitle.setCursor(Cursor.HAND);
        vboxAdminGroup = new VBox(6);
        btnQueryAllAppoint = new Button("查询全部预约");
        btnAddDoctor = new Button("新增医生");
        btnUpdateDoctor = new Button("修改医生");
        btnDeleteDoctor = new Button("删除医生");
        vboxAdminGroup.getChildren().addAll(btnQueryAllAppoint, btnAddDoctor, btnUpdateDoctor, btnDeleteDoctor);

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
                btnQueryAllDoctor, btnQueryByDept, btnAddAppoint, btnMyAppoint, btnCancelAppoint, btnHealthArticle,
                btnQueryAllAppoint, btnAddDoctor, btnUpdateDoctor, btnDeleteDoctor
        };
        for (Button b : btns) {
            b.setMaxWidth(Double.MAX_VALUE);
            b.setPrefHeight(34);
            String btnNormal = "-fx-background-color:"+themePrimaryColor+";-fx-text-fill:white;-fx-font-size:12px;-fx-background-radius:5;-fx-cursor:hand;";
            String btnHover = "-fx-background-color:"+themeHoverColor+";-fx-text-fill:white;-fx-font-size:12px;-fx-background-radius:5;-fx-cursor:hand;";
            b.setStyle(btnNormal);
            b.setOnMouseEntered(e -> {
                if(b != _activeNavButton){
                    b.setStyle(btnHover);
                }
            });
            b.setOnMouseExited(e -> {
                if(b != _activeNavButton){
                    b.setStyle(btnNormal);
                }
            });
        }

        if (!"管理员".equals(_loginUserRole)) {
            lblAdminGroupTitle.setVisible(false);
            vboxAdminGroup.setVisible(false);
            sep2.setVisible(false);
        }

        btnQueryAllDoctor.setOnAction(e -> switchPanel(panelQueryAllDoctor, btnQueryAllDoctor));
        btnQueryByDept.setOnAction(e -> switchPanel(panelQueryDept, btnQueryByDept));
        btnAddAppoint.setOnAction(e -> switchPanel(panelAddAppoint, btnAddAppoint));
        btnMyAppoint.setOnAction(e -> switchPanel(panelMyAppoint, btnMyAppoint));
        btnCancelAppoint.setOnAction(e -> switchPanel(panelCancelAppoint, btnCancelAppoint));
        btnHealthArticle.setOnAction(e -> switchPanel(panelHealthArticle, btnHealthArticle));
        btnQueryAllAppoint.setOnAction(e -> switchPanel(panelQueryAllAppoint, btnQueryAllAppoint));
        btnAddDoctor.setOnAction(e -> switchPanel(panelAddDoctor, btnAddDoctor));
        btnUpdateDoctor.setOnAction(e -> switchPanel(panelUpdateDoctor, btnUpdateDoctor));
        btnDeleteDoctor.setOnAction(e -> switchPanel(panelDeleteDoctor, btnDeleteDoctor));

        side.getChildren().addAll(
                title, sep1,
                lblUserGroupTitle, vboxUserGroup,
                sep2, lblAdminGroupTitle, vboxAdminGroup
        );
        return side;
    }

    private void switchPanel(VBox panel, Button navButton) {
        if(_activeNavButton != null){
            String btnNormal = "-fx-background-color:"+themePrimaryColor+";-fx-text-fill:white;-fx-font-size:12px;-fx-background-radius:5;-fx-cursor:hand;";
            _activeNavButton.setStyle(btnNormal);
        }
        _activeNavButton = navButton;
        String btnActive = "-fx-background-color:"+themeActiveColor+";-fx-text-fill:white;-fx-font-weight:bold;-fx-font-size:12px;-fx-background-radius:5;-fx-cursor:hand;";
        _activeNavButton.setStyle(btnActive);

        _contentPane.getChildren().clear();
        _contentPane.getChildren().add(wrapCard(panel));

        if (panel == panelQueryAllDoctor) {
            actionQueryAllDoctor();
        } else if (panel == panelMyAppoint) {
            loadAllMyAppointment();
        } else if (panel == panelQueryAllAppoint) {
            actionQueryAllAppointment();
        }else if(panel == panelAddAppoint){
            loadAppointDoctorTable();
            dateDisableTimeMap.clear();
            currentDisableSet.clear();
            cbAppointTime.getSelectionModel().clearSelection();
            cbAppointTime.setItems(FXCollections.observableArrayList(allTimeOptions));
        }else if(panel == panelCancelAppoint){
            loadCancelAbleAppointment();
        }else if(panel == panelDeleteDoctor){
            loadCanDeleteDoctorTable();
        }else if(panel == panelHealthArticle){
            loadHealthArticleList();
        }
    }

    private VBox wrapCard(VBox inner) {
        VBox card = new VBox();
        card.setMaxWidth(Double.MAX_VALUE);
        card.setPadding(new Insets(14, 14, 14, 14));
        card.setStyle("-fx-background-color: #ffffff;"
                + "-fx-background-radius:10;"
                + "-fx-border-radius:10;"
                + "-fx-border-color:"+themePrimaryColor+"33;"
                + "-fx-border-width:1;"
                + "-fx-effect: dropshadow(gaussian, "+themePrimaryColor+"18,10,0.1,0,3);");
        card.getChildren().add(inner);
        return card;
    }

    // =====================表格美化工具方法【模仿学籍界面】=====================
    private void styleTable(TableView<?> tableView) {
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tableView.setPlaceholder(buildTablePlaceholder());
        tableView.getStyleClass().addAll("table-view-clean");
    }

    private Label buildTablePlaceholder() {
        Label placeholder = new Label("暂无数据");
        placeholder.setStyle("-fx-font-size:14px;-fx-text-fill:#888888;");
        return placeholder;
    }

    private TableCell<Appointment,String> createAppointStatusCell() {
        return new TableCell<Appointment, String>(){
            private final Label badge = new Label();
            {
                badge.setPadding(new Insets(3,8,3,8));
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if(empty || item == null){
                    setGraphic(null);
                    return;
                }
                String status = item.isBlank() ? "待就诊" : item;
                badge.setText(status);
                switch (status){
                    case "待就诊":
                        badge.setStyle("-fx-background-color:#2196F3;-fx-text-fill:white;-fx-padding:3 8 3 8;-fx-background-radius:4;");
                        break;
                    case "已取消":
                        badge.setStyle("-fx-background-color:#9E9E9E;-fx-text-fill:white;-fx-padding:3 8 3 8;-fx-background-radius:4;");
                        break;
                    case "已就诊":
                        badge.setStyle("-fx-background-color:#4CAF50;-fx-text-fill:white;-fx-padding:3 8 3 8;-fx-background-radius:4;");
                        break;
                    default:
                        badge.setStyle("-fx-background-color:#757575;-fx-text-fill:white;-fx-padding:3 8 3 8;-fx-background-radius:4;");
                }
                setGraphic(badge);
                setAlignment(Pos.CENTER);
            }
        };
    }
    // ======================================================================

    private void initAllPanels() {
        // ========== 1. 查询全部医生 ==========
        panelQueryAllDoctor = new VBox(10);
        panelQueryAllDoctor.setAlignment(Pos.TOP_LEFT);
        panelQueryAllDoctor.setMaxWidth(Double.MAX_VALUE);
        Label lab1 = new Label("医生信息管理 — 查询全部医生");
        lab1.setFont(Font.font("System", FontWeight.BOLD, 17));
        lab1.setTextFill(Color.web(themePrimaryColor));
        tableAllDoctor = new TableView<>();
        VBox.setVgrow(tableAllDoctor, Priority.ALWAYS);
        TableColumn<Doctor, String> colDocId = new TableColumn<>("医生编号");
        colDocId.setCellValueFactory(new PropertyValueFactory<>("doctorId"));
        colDocId.setMinWidth(100);
        TableColumn<Doctor, String> colName = new TableColumn<>("姓名");
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colName.setMinWidth(80);
        TableColumn<Doctor, String> colDept = new TableColumn<>("科室");
        colDept.setCellValueFactory(new PropertyValueFactory<>("department"));
        colDept.setMinWidth(80);
        TableColumn<Doctor, String> colTitle = new TableColumn<>("职称");
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colTitle.setMinWidth(80);
        tableAllDoctor.getColumns().addAll(colDocId, colName, colDept, colTitle);
        styleTable(tableAllDoctor);

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
        lab2.setTextFill(Color.web(themePrimaryColor));
        tfDept = new TextField();
        tfDept.setPromptText("输入科室名称，例如：内科");
        tfDept.setStyle(fieldStyle());
        tfDept.setMaxWidth(Double.MAX_VALUE);
        Button btnQDept = new Button("查询");
        btnQDept.setStyle("-fx-background-color:"+themePrimaryColor+";-fx-text-fill:white;-fx-background-radius:5;");
        btnQDept.setOnAction(e -> actionQueryDoctorByDept());
        tableDeptDoctor = new TableView<>();
        VBox.setVgrow(tableDeptDoctor, Priority.ALWAYS);
        TableColumn<Doctor, String> c1 = new TableColumn<>("医生编号");
        c1.setCellValueFactory(new PropertyValueFactory<>("doctorId"));
        c1.setMinWidth(100);
        TableColumn<Doctor, String> c2 = new TableColumn<>("姓名");
        c2.setCellValueFactory(new PropertyValueFactory<>("name"));
        c2.setMinWidth(80);
        TableColumn<Doctor, String> c3 = new TableColumn<>("科室");
        c3.setCellValueFactory(new PropertyValueFactory<>("department"));
        c3.setMinWidth(80);
        TableColumn<Doctor, String> c4 = new TableColumn<>("职称");
        c4.setCellValueFactory(new PropertyValueFactory<>("title"));
        c4.setMinWidth(80);
        tableDeptDoctor.getColumns().addAll(c1, c2, c3, c4);
        styleTable(tableDeptDoctor);

        panelQueryDept.getChildren().addAll(lab2, tfDept, btnQDept, tableDeptDoctor);
        VBox.setVgrow(panelQueryDept, Priority.ALWAYS);

        // ==========3.预约挂号 ==========
        panelAddAppoint = new VBox(10);
        panelAddAppoint.setAlignment(Pos.TOP_LEFT);
        panelAddAppoint.setMaxWidth(Double.MAX_VALUE);
        Label lab3 = new Label("预约挂号 — 新建就诊预约");
        lab3.setFont(Font.font("System", FontWeight.BOLD, 17));
        lab3.setTextFill(Color.web(themePrimaryColor));
        Label tipAppoint = new Label("在下方表格选中一位医生，再选择日期与就诊时段，灰色时段代表该时段已被占用不可选");
        tipAppoint.setFont(Font.font("System", 13));
        tipAppoint.setTextFill(Color.web("#555555"));
        lblSelectedDoctorTip = new Label("✅尚未选择医生");
        lblSelectedDoctorTip.setFont(Font.font("System",13));
        lblSelectedDoctorTip.setTextFill(Color.web("#c0392b"));
        tvAppointDoctor = new TableView<>();
        VBox.setVgrow(tvAppointDoctor, Priority.ALWAYS);
        TableColumn<Doctor, String> colADocId = new TableColumn<>("医生编号");
        colADocId.setCellValueFactory(new PropertyValueFactory<>("doctorId"));
        colADocId.setMinWidth(100);
        TableColumn<Doctor, String> colAName = new TableColumn<>("姓名");
        colAName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colAName.setMinWidth(80);
        TableColumn<Doctor, String> colADept = new TableColumn<>("科室");
        colADept.setCellValueFactory(new PropertyValueFactory<>("department"));
        colADept.setMinWidth(80);
        TableColumn<Doctor, String> colATitle = new TableColumn<>("职称");
        colATitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colATitle.setMinWidth(80);
        tvAppointDoctor.getColumns().addAll(colADocId, colAName, colADept, colATitle);
        styleTable(tvAppointDoctor);

        tvAppointDoctor.getSelectionModel().selectedItemProperty().addListener((obs, oldDoc, newDoc) -> {
            if(newDoc != null){
                lblSelectedDoctorTip.setText("✅已选择医生："+newDoc.getDoctorId()+" | "+newDoc.getName()+" | "+newDoc.getDepartment());
                lblSelectedDoctorTip.setTextFill(Color.web("#27ae60"));
                String docId = newDoc.getDoctorId();
                new Thread(()->{
                    try {
                        Message resp = _hospitalSrv.getDoctorOccupiedTime(docId);
                        if(IConstant.STATUS_SUCCESS.equals(resp.getStatusCode())){
                            List<Timestamp> tsList = (List<Timestamp>) resp.getData();
                            Map<LocalDate,Set<String>> tempMap = new HashMap<>();
                            for(Timestamp ts : tsList){
                                LocalDateTime ldt = ts.toLocalDateTime();
                                LocalDate ld = ldt.toLocalDate();
                                String timeStr = String.format("%02d:%02d",ldt.getHour(),ldt.getMinute());
                                tempMap.computeIfAbsent(ld,k->new HashSet<>()).add(timeStr);
                            }
                            Platform.runLater(()->{
                                dateDisableTimeMap.clear();
                                dateDisableTimeMap.putAll(tempMap);
                                LocalDate selDate = dpAppointDate.getValue();
                                currentDisableSet.clear();
                                if(selDate!=null && dateDisableTimeMap.containsKey(selDate)){
                                    currentDisableSet.addAll(dateDisableTimeMap.get(selDate));
                                }
                                cbAppointTime.setItems(FXCollections.observableArrayList(allTimeOptions));
                            });
                        }else{
                            Platform.runLater(()->showAlert(Alert.AlertType.ERROR,"获取占用时段失败",resp.getData().toString()));
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                        Platform.runLater(()->showAlert(Alert.AlertType.ERROR,"网络异常","查询医生占用时段失败"));
                    }
                }).start();
            }else{
                lblSelectedDoctorTip.setText("✅尚未选择医生");
                lblSelectedDoctorTip.setTextFill(Color.web("#c0392b"));
                dateDisableTimeMap.clear();
                currentDisableSet.clear();
                cbAppointTime.getSelectionModel().clearSelection();
                cbAppointTime.setItems(FXCollections.observableArrayList(allTimeOptions));
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
        cbAppointTime.setItems(allTimeOptions);
        cbAppointTime.setPromptText("选择就诊时刻");
        cbAppointTime.setCellFactory(param -> new ListCell<String>(){
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if(empty || item == null){
                    setText(null);
                    setDisable(false);
                }else{
                    setText(item);
                    if(currentDisableSet.contains(item)){
                        setDisable(true);
                        setStyle("-fx-background-color:#dddddd; -fx-text-fill:#777777;");
                    }else{
                        setDisable(false);
                        setStyle("");
                    }
                }
            }
        });
        cbAppointTime.setButtonCell(new ListCell<String>(){
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(item==null?"":item);
            }
        });

        dpAppointDate.valueProperty().addListener((obs,oldD,newD)->{
            currentDisableSet.clear();
            if(newD!=null && dateDisableTimeMap.containsKey(newD)){
                currentDisableSet.addAll(dateDisableTimeMap.get(newD));
            }
            cbAppointTime.getSelectionModel().clearSelection();
            cbAppointTime.setItems(FXCollections.observableArrayList(allTimeOptions));
        });

        HBox hbRow = new HBox(15, new Label("就诊日期："), dpAppointDate, new Label("就诊时段："), cbAppointTime);
        hbRow.setAlignment(Pos.CENTER_LEFT);
        btnSubmitAppoint = new Button("确认预约");
        btnSubmitAppoint.setStyle("-fx-background-color:"+themePrimaryColor+"; -fx-text-fill:white; -fx-background-radius:5;");
        btnSubmitAppoint.setOnAction(e -> doSubmitAppoint());
        panelAddAppoint.getChildren().addAll(lab3, tipAppoint, lblSelectedDoctorTip, tvAppointDoctor, hbRow, btnSubmitAppoint);
        VBox.setVgrow(panelAddAppoint, Priority.ALWAYS);

        // ==========4.我的预约记录 ==========
        panelMyAppoint = new VBox(10);
        panelMyAppoint.setAlignment(Pos.TOP_LEFT);
        panelMyAppoint.setMaxWidth(Double.MAX_VALUE);
        Label lab4 = new Label("预约记录 — 我的就诊预约");
        lab4.setFont(Font.font("System", FontWeight.BOLD, 17));
        lab4.setTextFill(Color.web(themePrimaryColor));
        tableAllMyAppoint = new TableView<>();
        VBox.setVgrow(tableAllMyAppoint, Priority.ALWAYS);
        TableColumn<Appointment, String> aid = new TableColumn<>("预约编号");
        aid.setCellValueFactory(new PropertyValueFactory<>("appointmentId"));
        aid.setMinWidth(110);
        TableColumn<Appointment, String> uid = new TableColumn<>("用户ID");
        uid.setCellValueFactory(new PropertyValueFactory<>("userId"));
        uid.setMinWidth(90);
        TableColumn<Appointment, String> did = new TableColumn<>("医生ID");
        did.setCellValueFactory(new PropertyValueFactory<>("doctorId"));
        did.setMinWidth(90);
        TableColumn<Appointment, Date> atime = new TableColumn<>("预约时间");
        atime.setCellValueFactory(new PropertyValueFactory<>("appointmentTime"));
        atime.setMinWidth(150);
        TableColumn<Appointment, String> stat = new TableColumn<>("状态");
        stat.setCellValueFactory(new PropertyValueFactory<>("status"));
        stat.setMinWidth(100);
        stat.setCellFactory(col -> createAppointStatusCell());
        tableAllMyAppoint.getColumns().addAll(aid, uid, did, atime, stat);
        styleTable(tableAllMyAppoint);
        panelMyAppoint.getChildren().addAll(lab4, tableAllMyAppoint);
        VBox.setVgrow(panelMyAppoint, Priority.ALWAYS);

        // ==========5.取消预约 ==========
        panelCancelAppoint = new VBox(10);
        panelCancelAppoint.setAlignment(Pos.TOP_LEFT);
        panelCancelAppoint.setMaxWidth(Double.MAX_VALUE);
        Label lab5 = new Label("预约操作 — 取消就诊预约");
        lab5.setFont(Font.font("System", FontWeight.BOLD, 17));
        lab5.setTextFill(Color.web(themePrimaryColor));
        Label tipCancel = new Label("在表格选中本人【待就诊】预约记录，点击下方按钮执行取消");
        tipCancel.setFont(Font.font("System", 13));
        tipCancel.setTextFill(Color.web("#555555"));
        tableMyAppoint = new TableView<>();
        VBox.setVgrow(tableMyAppoint, Priority.ALWAYS);
        TableColumn<Appointment, String> cAid = new TableColumn<>("预约编号");
        cAid.setCellValueFactory(new PropertyValueFactory<>("appointmentId"));
        cAid.setMinWidth(110);
        TableColumn<Appointment, String> cUid = new TableColumn<>("用户ID");
        cUid.setCellValueFactory(new PropertyValueFactory<>("userId"));
        cUid.setMinWidth(90);
        TableColumn<Appointment, String> cDid = new TableColumn<>("医生ID");
        cDid.setCellValueFactory(new PropertyValueFactory<>("doctorId"));
        cDid.setMinWidth(90);
        TableColumn<Appointment, Date> cTime = new TableColumn<>("预约时间");
        cTime.setCellValueFactory(new PropertyValueFactory<>("appointmentTime"));
        cTime.setMinWidth(150);
        TableColumn<Appointment, String> cStat = new TableColumn<>("状态");
        cStat.setCellValueFactory(new PropertyValueFactory<>("status"));
        cStat.setMinWidth(100);
        cStat.setCellFactory(col -> createAppointStatusCell());
        tableMyAppoint.getColumns().addAll(cAid,cUid,cDid,cTime,cStat);
        styleTable(tableMyAppoint);

        btnCancelSelectedAppoint = new Button("取消选中的预约");
        btnCancelSelectedAppoint.setStyle("-fx-background-color:"+themePrimaryColor+"; -fx-text-fill:white; -fx-background-radius:5;");
        btnCancelSelectedAppoint.setOnAction(e -> doCancelSelected());
        panelCancelAppoint.getChildren().addAll(lab5, tipCancel, tableMyAppoint, btnCancelSelectedAppoint);
        VBox.setVgrow(panelCancelAppoint, Priority.ALWAYS);

        // ==========管理员：全部预约表格 ==========
        panelQueryAllAppoint = new VBox(10);
        panelQueryAllAppoint.setAlignment(Pos.TOP_LEFT);
        panelQueryAllAppoint.setMaxWidth(Double.MAX_VALUE);
        Label labA1 = new Label("【管理员】全部就诊预约记录");
        labA1.setFont(Font.font("System", FontWeight.BOLD, 17));
        labA1.setTextFill(Color.web(themePrimaryColor));
        tableAllAppoint = new TableView<>();
        VBox.setVgrow(tableAllAppoint, Priority.ALWAYS);
        TableColumn<Appointment, String> aa = new TableColumn<>("预约编号");
        aa.setCellValueFactory(new PropertyValueFactory<>("appointmentId"));
        aa.setMinWidth(110);
        TableColumn<Appointment, String> uu = new TableColumn<>("用户ID");
        uu.setCellValueFactory(new PropertyValueFactory<>("userId"));
        uu.setMinWidth(90);
        TableColumn<Appointment, String> dd = new TableColumn<>("医生ID");
        dd.setCellValueFactory(new PropertyValueFactory<>("doctorId"));
        dd.setMinWidth(90);
        TableColumn<Appointment, Date> tt = new TableColumn<>("预约时间");
        tt.setCellValueFactory(new PropertyValueFactory<>("appointmentTime"));
        tt.setMinWidth(150);
        TableColumn<Appointment, String> ss = new TableColumn<>("状态");
        ss.setCellValueFactory(new PropertyValueFactory<>("status"));
        ss.setMinWidth(100);
        ss.setCellFactory(col -> createAppointStatusCell());
        tableAllAppoint.getColumns().addAll(aa, uu, dd, tt, ss);

        TableColumn<Appointment, Void> colOpt = new TableColumn<>("操作");
        colOpt.setMinWidth(90);
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
        styleTable(tableAllAppoint);
        panelQueryAllAppoint.getChildren().addAll(labA1, tableAllAppoint);
        VBox.setVgrow(panelQueryAllAppoint, Priority.ALWAYS);

        // ==========管理员新增医生 ==========
        panelAddDoctor = new VBox(10);
        panelAddDoctor.setAlignment(Pos.TOP_LEFT);
        panelAddDoctor.setMaxWidth(Double.MAX_VALUE);
        Label labA2 = new Label("【管理员】医生信息 — 新增医生");
        labA2.setFont(Font.font("System", FontWeight.BOLD, 17));
        labA2.setTextFill(Color.web(themePrimaryColor));
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
        btnAddDoc.setStyle("-fx-background-color:"+themePrimaryColor+"; -fx-text-fill:white; -fx-background-radius:5;");
        btnAddDoc.setOnAction(e -> actionAddDoctor());
        panelAddDoctor.getChildren().addAll(labA2, tfAddDocId, tfAddDocName, tfAddDocDept, tfAddDocTitle, btnAddDoc);
        VBox.setVgrow(panelAddDoctor, Priority.ALWAYS);

        // ==========管理员修改医生 ==========
        panelUpdateDoctor = new VBox(10);
        panelUpdateDoctor.setAlignment(Pos.TOP_LEFT);
        panelUpdateDoctor.setMaxWidth(Double.MAX_VALUE);
        Label labA3 = new Label("【管理员】医生信息 — 修改医生");
        labA3.setFont(Font.font("System", FontWeight.BOLD, 17));
        labA3.setTextFill(Color.web(themePrimaryColor));
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
        btnUpdDoc.setStyle("-fx-background-color:"+themePrimaryColor+"; -fx-text-fill:white; -fx-background-radius:5;");
        btnUpdDoc.setOnAction(e -> actionUpdateDoctor());
        panelUpdateDoctor.getChildren().addAll(labA3, tipUpdate, tfUpdDocId, tfUpdDocName, tfUpdDocDept, tfUpdDocTitle, btnUpdDoc);
        VBox.setVgrow(panelUpdateDoctor, Priority.ALWAYS);

        // ==========管理员删除医生 ==========
        panelDeleteDoctor = new VBox(10);
        panelDeleteDoctor.setAlignment(Pos.TOP_LEFT);
        panelDeleteDoctor.setMaxWidth(Double.MAX_VALUE);
        Label labA4 = new Label("【管理员】医生信息 — 删除医生");
        labA4.setFont(Font.font("System", FontWeight.BOLD, 17));
        labA4.setTextFill(Color.web(themePrimaryColor));
        Label tipDel = new Label("表格仅展示完全没有任何预约记录的医生；选中一行，点击按钮删除");
        tipDel.setFont(Font.font("System", 13));
        tipDel.setTextFill(Color.web("#555555"));
        tvDelDoctor = new TableView<>();
        VBox.setVgrow(tvDelDoctor, Priority.ALWAYS);
        TableColumn<Doctor, String> colDDocId = new TableColumn<>("医生编号");
        colDDocId.setCellValueFactory(new PropertyValueFactory<>("doctorId"));
        colDDocId.setMinWidth(100);
        TableColumn<Doctor, String> colDName = new TableColumn<>("姓名");
        colDName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colDName.setMinWidth(80);
        TableColumn<Doctor, String> colDDept = new TableColumn<>("科室");
        colDDept.setCellValueFactory(new PropertyValueFactory<>("department"));
        colDDept.setMinWidth(80);
        TableColumn<Doctor, String> colDTitle = new TableColumn<>("职称");
        colDTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colDTitle.setMinWidth(80);
        tvDelDoctor.getColumns().addAll(colDDocId, colDName, colDDept, colDTitle);
        styleTable(tvDelDoctor);

        btnDeleteSelectedDoctor = new Button("删除选中医生");
        btnDeleteSelectedDoctor.setStyle("-fx-background-color:#dc3545; -fx-text-fill:white; -fx-background-radius:5;");
        btnDeleteSelectedDoctor.setOnAction(e -> doDeleteSelectedDoctor());
        panelDeleteDoctor.getChildren().addAll(labA4, tipDel, tvDelDoctor, btnDeleteSelectedDoctor);
        VBox.setVgrow(panelDeleteDoctor, Priority.ALWAYS);

        //====================初始化健康教育面板====================
        panelHealthArticle = new VBox(10);
        panelHealthArticle.setAlignment(Pos.TOP_LEFT);
        panelHealthArticle.setMaxWidth(Double.MAX_VALUE);
        Label labHealth = new Label("📖 健康教育科普阅读");
        labHealth.setFont(Font.font("System", FontWeight.BOLD,17));
        labHealth.setTextFill(Color.web(themePrimaryColor));

        lvArticleList = new ListView<>();
        lvArticleList.setItems(obsArticleList);
        lvArticleList.setCellFactory(param -> new ListCell<HealthArticle>(){
            @Override
            protected void updateItem(HealthArticle item, boolean empty) {
                super.updateItem(item, empty);
                if(empty || item == null){
                    setText(null);
                }else{
                    setText(item.getTitle());
                }
            }
        });
        taArticleContent = new TextArea();
        taArticleContent.setEditable(false);
        taArticleContent.setWrapText(true);
        taArticleContent.setStyle("-fx-font-size:14px;");
        taArticleContent.setPromptText("请在左侧选择一篇文章阅读");

        SplitPane splitPane = new SplitPane();
        splitPane.getItems().addAll(lvArticleList, taArticleContent);
        splitPane.setDividerPositions(0.27);
        VBox.setVgrow(splitPane, Priority.ALWAYS);

        //选中列表项渲染正文
        lvArticleList.getSelectionModel().selectedItemProperty().addListener((obs,old,newVal)->{
            if(newVal != null){
                taArticleContent.setText(newVal.getContent());
            }
        });

        panelHealthArticle.getChildren().addAll(labHealth, splitPane);
        VBox.setVgrow(panelHealthArticle, Priority.ALWAYS);
    }

    //网络加载健康教育文章
    private void loadHealthArticleList(){
        new Thread(()->{
            try {
                Message resp = _hospitalSrv.queryAllHealthArticle();
                Platform.runLater(()->{
                    if(IConstant.STATUS_SUCCESS.equals(resp.getStatusCode())){
                        List<HealthArticle> list = (List<HealthArticle>) resp.getData();
                        obsArticleList.clear();
                        obsArticleList.addAll(list);
                        taArticleContent.clear();
                        lvArticleList.getSelectionModel().clearSelection();
                    }else{
                        showAlert(Alert.AlertType.ERROR,"加载文章失败",String.valueOf(resp.getData()));
                    }
                });
            }catch (Exception e){
                Platform.runLater(()->showAlert(Alert.AlertType.ERROR,"网络异常","获取健康教育文章失败"));
            }
        }).start();
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

    //==================== 预约挂号提交 ====================
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
                        dateDisableTimeMap.clear();
                        currentDisableSet.clear();
                        cbAppointTime.getSelectionModel().clearSelection();
                        cbAppointTime.setItems(FXCollections.observableArrayList(allTimeOptions));
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
                        tvAppointDoctor.getSelectionModel().clearSelection();
                    }
                });
            }catch (Exception ex){
                Platform.runLater(()->showAlert(Alert.AlertType.ERROR,"加载医生失败",ex.getMessage()));
            }
        }).start();
    }

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

    private Optional<Appointment> showAppointSelectDialog() { return Optional.empty(); }
    private Optional<String> showSelectMyAppointDialog() { return Optional.empty(); }
    private Optional<String> showSelectDeleteDoctorDialog() { return Optional.empty(); }

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
