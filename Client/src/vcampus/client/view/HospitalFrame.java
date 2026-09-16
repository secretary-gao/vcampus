/*
 * HospitalFrame
 *
 * Version 3.3 新增：擅长领域、健康教育、就诊叫号；表格UI美化；角色动态主题配色；前端时段禁用
 *
 * 2026-09-16
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.client.view;
import vcampus.client.biz.HospitalClientSrv;
import vcampus.common.constant.IConstant;
import vcampus.common.vo.Appointment;
import vcampus.common.vo.Doctor;
import vcampus.common.vo.HealthArticle;
import vcampus.common.vo.Medicine;
import vcampus.common.vo.Message;
import vcampus.common.vo.Prescription;
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
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import javafx.scene.image.ImageView;
import javafx.scene.Cursor;
import javafx.scene.shape.Rectangle;


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
    private Button btnHealthArticle;
    //====新增：就诊叫号按钮====
    private Button btnDoctorCallNo;
    //====药房取药新增控件====
private Button btnPharmacyTake;
private VBox panelPharmacyTake;


    private StackPane _contentPane;
    private Button _activeNavButton;
    private VBox vboxUserGroup;
    private VBox vboxAdminGroup;
    private Label lblUserGroupTitle;
    private Label lblAdminGroupTitle;
    private boolean userGroupExpanded = true;
    private boolean adminGroupExpanded = true;

    //面板控件
    private VBox panelQueryAllDoctor;
    private TableView<Doctor> tableAllDoctor;
    private VBox panelQueryDept;
    private TextField tfDept;
    private TableView<Doctor> tableDeptDoctor;
    private VBox panelAddAppoint;
    private TableView<Doctor> tvAppointDoctor;
    private Label lblSelectedDoctorTip;
    private DatePicker dpAppointDate;
    private ComboBox<String> cbAppointTime;
    private Button btnSubmitAppoint;

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
    private TextField tfAddDocSkill;

    private VBox panelUpdateDoctor;
    private TextField tfUpdDocId;
    private TextField tfUpdDocName;
    private TextField tfUpdDocDept;
    private TextField tfUpdDocTitle;
    private TextField tfUpdDocSkill;

    private VBox panelDeleteDoctor;
    private TableView<Doctor> tvDelDoctor;
    private Button btnDeleteSelectedDoctor;

    //健康教育
    private VBox panelHealthArticle;
    private ListView<HealthArticle> lvArticleList;
    private TextArea taArticleContent;
    private ObservableList<HealthArticle> obsArticleList;

    //====就诊叫号页面====
    private VBox panelDoctorCallNo;
    private TableView<Appointment> tvDoctorPendingAppoint;
    private Label _labUserBalance;
    private Button btnRecharge;
    private Label labTotalAmount;
    private FlowPane _prescriptionCards;

    //====管理员：药品库存管理====
private Button btnAdminMedicineStock;
private VBox panelAdminMedicineStock;
private TableView<Medicine> tvAdminMedicine;


    public HospitalFrame(String loginUserId, String loginUserRole) {
        this._loginUserId = loginUserId;
        this._loginUserRole = loginUserRole;
        dateDisableTimeMap = new HashMap<>();
        currentDisableSet = new HashSet<>();
        allTimeOptions = FXCollections.observableArrayList(
                "08:30", "09:00", "09:30", "10:00", "10:30",
                "14:00", "14:30", "15:00", "15:30"
        );
        obsArticleList = FXCollections.observableArrayList();

        switch (_loginUserRole) {
            case "管理员":
                themePrimaryColor = "#94b55a";
                themeHoverColor = "#7f9f4d";
                themeActiveColor = "#6b8a40";
                break;
            case "教师":
                themePrimaryColor = "#94b55a";
                themeHoverColor = "#7f9f4d";
                themeActiveColor = "#6b8a40";
                break;
            case "学生":
            default:
                themePrimaryColor = "#77b55a";
                themeHoverColor = "#7f9f4d";
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
        btnDoctorCallNo = new Button("就诊叫号");
        btnPharmacyTake = new Button("药房取药");
        vboxUserGroup.getChildren().addAll(btnQueryAllDoctor, btnQueryByDept, btnAddAppoint, btnMyAppoint, btnCancelAppoint, btnHealthArticle,btnDoctorCallNo,btnPharmacyTake);

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
        btnAdminMedicineStock = new Button("药品库存管理");
        vboxAdminGroup.getChildren().addAll(btnQueryAllAppoint, btnAddDoctor, btnUpdateDoctor, btnDeleteDoctor, btnAdminMedicineStock);


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
        btnQueryAllDoctor, btnQueryByDept, btnAddAppoint, btnMyAppoint, btnCancelAppoint, btnHealthArticle,btnDoctorCallNo,btnPharmacyTake,
        btnQueryAllAppoint, btnAddDoctor, btnUpdateDoctor, btnDeleteDoctor, btnAdminMedicineStock
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
        btnDoctorCallNo.setOnAction(e -> switchPanel(panelDoctorCallNo, btnDoctorCallNo));
        btnQueryAllAppoint.setOnAction(e -> switchPanel(panelQueryAllAppoint, btnQueryAllAppoint));
        btnAddDoctor.setOnAction(e -> switchPanel(panelAddDoctor, btnAddDoctor));
        btnUpdateDoctor.setOnAction(e -> switchPanel(panelUpdateDoctor, btnUpdateDoctor));
        btnDeleteDoctor.setOnAction(e -> switchPanel(panelDeleteDoctor, btnDeleteDoctor));
        btnPharmacyTake.setOnAction(e -> switchPanel(panelPharmacyTake, btnPharmacyTake));
        btnAdminMedicineStock.setOnAction(e -> switchPanel(panelAdminMedicineStock, btnAdminMedicineStock));

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
        }else if(panel == panelDoctorCallNo){
            loadDoctorPendingAppointList();
        }
        else if(panel == panelPharmacyTake){
            loadMyNoTakePrescription();
        }
        else if(panel == panelAdminMedicineStock){
            loadAdminMedicineStockList();
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
        TableColumn<Doctor,String> colSkillAll = new TableColumn<>("擅长领域");
        colSkillAll.setCellValueFactory(new PropertyValueFactory<>("skill"));
        colSkillAll.setPrefWidth(220);

        tableAllDoctor.getColumns().addAll(colDocId, colName, colDept, colTitle, colSkillAll);
        styleTable(tableAllDoctor);

        tableAllDoctor.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if(newVal != null){
                tfUpdDocId.setText(newVal.getDoctorId());
                tfUpdDocName.setText(newVal.getName());
                tfUpdDocDept.setText(newVal.getDepartment());
                tfUpdDocTitle.setText(newVal.getTitle());
                tfUpdDocSkill.setText(newVal.getSkill() == null ? "" : newVal.getSkill());
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
        TableColumn<Doctor,String> colSkillDept = new TableColumn<>("擅长领域");
        colSkillDept.setCellValueFactory(new PropertyValueFactory<>("skill"));
        colSkillDept.setPrefWidth(210);

        tableDeptDoctor.getColumns().addAll(c1, c2, c3, c4, colSkillDept);
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
        TableColumn<Doctor,String> colAppSkill = new TableColumn<>("擅长领域");
        colAppSkill.setCellValueFactory(new PropertyValueFactory<>("skill"));
        colAppSkill.setPrefWidth(200);

        tvAppointDoctor.getColumns().addAll(colADocId, colAName, colADept, colATitle, colAppSkill);
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
        tfAddDocSkill = new TextField();
        tfAddDocSkill.setPromptText("擅长领域");
        tfAddDocSkill.setStyle(fieldStyle());
        tfAddDocSkill.setMaxWidth(Double.MAX_VALUE);

        Button btnAddDoc = new Button("提交新增");
        btnAddDoc.setStyle("-fx-background-color:"+themePrimaryColor+"; -fx-text-fill:white; -fx-background-radius:5;");
        btnAddDoc.setOnAction(e -> actionAddDoctor());
        panelAddDoctor.getChildren().addAll(labA2, tfAddDocId, tfAddDocName, tfAddDocDept, tfAddDocTitle, tfAddDocSkill, btnAddDoc);
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
        tfUpdDocSkill = new TextField();
        tfUpdDocSkill.setPromptText("擅长领域");
        tfUpdDocSkill.setStyle(fieldStyle());
        tfUpdDocSkill.setMaxWidth(Double.MAX_VALUE);

        Button btnUpdDoc = new Button("提交修改");
        btnUpdDoc.setStyle("-fx-background-color:"+themePrimaryColor+"; -fx-text-fill:white; -fx-background-radius:5;");
        btnUpdDoc.setOnAction(e -> actionUpdateDoctor());
        panelUpdateDoctor.getChildren().addAll(labA3, tipUpdate, tfUpdDocId, tfUpdDocName, tfUpdDocDept, tfUpdDocTitle, tfUpdDocSkill, btnUpdDoc);
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
        TableColumn<Doctor,String> colSkillDel = new TableColumn<>("擅长领域");
        colSkillDel.setCellValueFactory(new PropertyValueFactory<>("skill"));
        colSkillDel.setPrefWidth(210);

        tvDelDoctor.getColumns().addAll(colDDocId, colDName, colDDept, colDTitle, colSkillDel);
        styleTable(tvDelDoctor);

        btnDeleteSelectedDoctor = new Button("删除选中医生");
        btnDeleteSelectedDoctor.setStyle("-fx-background-color:#dc3545; -fx-text-fill:white; -fx-background-radius:5;");
        btnDeleteSelectedDoctor.setOnAction(e -> doDeleteSelectedDoctor());
        panelDeleteDoctor.getChildren().addAll(labA4, tipDel, tvDelDoctor, btnDeleteSelectedDoctor);
        VBox.setVgrow(panelDeleteDoctor, Priority.ALWAYS);

        //====================健康教育面板====================
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

        lvArticleList.getSelectionModel().selectedItemProperty().addListener((obs,old,newVal)->{
            if(newVal != null){
                taArticleContent.setText(newVal.getContent());
            }
        });

        panelHealthArticle.getChildren().addAll(labHealth, splitPane);
        VBox.setVgrow(panelHealthArticle, Priority.ALWAYS);

        //=====================【新增】就诊叫号：医生查看自己待就诊预约=====================
        panelDoctorCallNo = new VBox(10);
        panelDoctorCallNo.setAlignment(Pos.TOP_LEFT);
        panelDoctorCallNo.setMaxWidth(Double.MAX_VALUE);
        Label labCall = new Label("🏥就诊叫号 — 我的待就诊患者");
        labCall.setFont(Font.font("System", FontWeight.BOLD,17));
        labCall.setTextFill(Color.web(themePrimaryColor));
        Label tipCall = new Label("说明：只有登录账号ID等于医生编号，才可以看到自己待就诊记录，点击【完成就诊】将记录更新为已就诊");
        tipCall.setFont(Font.font("System",12));
        tipCall.setTextFill(Color.web("#555555"));

        tvDoctorPendingAppoint = new TableView<>();
        VBox.setVgrow(tvDoctorPendingAppoint, Priority.ALWAYS);

        TableColumn<Appointment,String> colAppId = new TableColumn<>("预约编号");
        colAppId.setCellValueFactory(new PropertyValueFactory<>("appointmentId"));
        colAppId.setMinWidth(110);
        TableColumn<Appointment,String> colUserId = new TableColumn<>("就诊用户ID");
        colUserId.setCellValueFactory(new PropertyValueFactory<>("userId"));
        colUserId.setMinWidth(100);
        TableColumn<Appointment,Date> colAppTime = new TableColumn<>("预约时间");
        colAppTime.setCellValueFactory(new PropertyValueFactory<>("appointmentTime"));
        colAppTime.setMinWidth(160);
        TableColumn<Appointment,String> colStatusCall = new TableColumn<>("状态");
        colStatusCall.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStatusCall.setMinWidth(90);
        colStatusCall.setCellFactory(col -> createAppointStatusCell());

        TableColumn<Appointment,Void> colOptFinish = new TableColumn<>("操作");
colOptFinish.setMinWidth(200);
colOptFinish.setCellFactory(param -> new TableCell<Appointment, Void>(){
    @Override
    protected void updateItem(Void item, boolean empty) {
        super.updateItem(item, empty);
        if(empty){
            setGraphic(null);
        }else{
            Appointment apt = getTableView().getItems().get(getIndex());
            if("待就诊".equals(apt.getStatus())){
                HBox box = new HBox(8);
                Button btnPres = new Button("开药");
                btnPres.setStyle("-fx-background-color:#2980b9;-fx-text-fill:white;-fx-background-radius:4;");
                btnPres.setOnAction(e->{
                    openPrescriptionDialog(apt.getAppointmentId(), apt.getUserId(), _loginUserId);
                });

                Button btnFinish = new Button("完成就诊");
                btnFinish.setStyle("-fx-background-color:#27ae60;-fx-text-fill:white;-fx-background-radius:4;");
                btnFinish.setOnAction(e->{
                    String appointId = apt.getAppointmentId();
                    doFinishAppoint(appointId,_loginUserId);
                });
                box.getChildren().addAll(btnPres, btnFinish);
                setGraphic(box);
            }else{
                setGraphic(null);
            }
        }
    }
});



        tvDoctorPendingAppoint.getColumns().addAll(colAppId,colUserId,colAppTime,colStatusCall,colOptFinish);
        styleTable(tvDoctorPendingAppoint);

        panelDoctorCallNo.getChildren().addAll(labCall,tipCall,tvDoctorPendingAppoint);
        VBox.setVgrow(panelDoctorCallNo, Priority.ALWAYS);

        //====================药房取药 用户页面【卡片版+充值+支付】====================
        panelPharmacyTake = new VBox(10);
        panelPharmacyTake.setAlignment(Pos.TOP_LEFT);
        panelPharmacyTake.setMaxWidth(Double.MAX_VALUE);

        //顶部栏：余额 + 充值按钮 + 合计
        HBox topBar = new HBox(15);
        topBar.setAlignment(Pos.CENTER_LEFT);
        _labUserBalance = new Label("校园卡余额：--");
        _labUserBalance.setFont(Font.font("System", FontWeight.BOLD, 14));
        btnRecharge = new Button("💳充值");
        btnRecharge.setStyle("-fx-background-color:#2980b9;-fx-text-fill:white;-fx-background-radius:6;");
        btnRecharge.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog("0.00");
            dialog.setTitle("校园卡充值");
            dialog.setHeaderText("请输入充值金额");
            Optional<String> res = dialog.showAndWait();
            if (res.isPresent()) {
                try {
                    double money = Double.parseDouble(res.get().trim());
                    if (money <= 0) {
                        showAlert(Alert.AlertType.WARNING, "提示", "充值金额必须大于0");
                        return;
                    }
                    new Thread(() -> {
                        try {
                            Message msg = _hospitalSrv.memRecharge(_loginUserId, money);
                            Platform.runLater(() -> {
                                if (IConstant.STATUS_SUCCESS.equals(msg.getStatusCode())) {
                                    showAlert(Alert.AlertType.INFORMATION, "成功", "充值成功");
                                    loadMyNoTakePrescription();
                                } else {
                                    showAlert(Alert.AlertType.ERROR, "失败", String.valueOf(msg.getData()));
                                }
                            });
                        } catch (Exception ex) {
                            Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "异常", "充值网络异常"));
                        }
                    }).start();
                } catch (NumberFormatException ex) {
                    showAlert(Alert.AlertType.WARNING, "输入错误", "请输入合法数字金额");
                }
            }
        });
        labTotalAmount = new Label("处方合计：¥ 0.00");
        labTotalAmount.setFont(Font.font("System", FontWeight.BOLD, 14));
        topBar.getChildren().addAll(_labUserBalance, btnRecharge, labTotalAmount);

        Label labPharm = new Label("💊药房取药 — 待缴费处方");
        labPharm.setFont(Font.font("System", FontWeight.BOLD, 17));
        labPharm.setTextFill(Color.web(themePrimaryColor));
        Label tipPharm = new Label("医生开具的处方，先充值，支付药费后完成取药（校园卡为内存模拟，服务重启余额重置）");
        tipPharm.setFont(Font.font("System", 12));
        tipPharm.setTextFill(Color.web("#555555"));

        //卡片FlowPane，模仿商店商品卡片
        _prescriptionCards = new FlowPane(16, 16);
        _prescriptionCards.setPadding(new Insets(12));
        ScrollPane scrollCards = new ScrollPane(_prescriptionCards);
        scrollCards.setFitToWidth(true);
        scrollCards.setStyle("-fx-background-color:transparent;");

        panelPharmacyTake.getChildren().addAll(labPharm, tipPharm, topBar, scrollCards);
        VBox.setVgrow(scrollCards, Priority.ALWAYS);

       //====================管理员：药品库存管理面板====================
panelAdminMedicineStock = new VBox(12);
panelAdminMedicineStock.setAlignment(Pos.TOP_LEFT);
panelAdminMedicineStock.setMaxWidth(Double.MAX_VALUE);
Label labAdminMed = new Label("录入药品 (管理员)");
labAdminMed.setFont(Font.font("System", FontWeight.BOLD,17));
labAdminMed.setTextFill(Color.web(themePrimaryColor));

// ========= 表单区域 =========
GridPane formPane = new GridPane();
formPane.setHgap(10);
formPane.setVgap(8);
formPane.setPadding(new Insets(10,10,10,0));

TextField tfMedId = new TextField();
tfMedId.setPromptText("药品编号");
TextField tfMedName = new TextField();
tfMedName.setPromptText("药品名称");
TextField tfPrice = new TextField();
tfPrice.setPromptText("单价");
TextField tfStock = new TextField();
tfStock.setPromptText("库存");

formPane.add(new Label("编号"),0,0);
formPane.add(tfMedId,1,0);
formPane.add(new Label("名称"),0,1);
formPane.add(tfMedName,1,1);
formPane.add(new Label("单价"),0,2);
formPane.add(tfPrice,1,2);
formPane.add(new Label("库存"),0,3);
formPane.add(tfStock,1,3);


// 按钮行
HBox btnFormBox = new HBox(10);
Button btnAddMed = new Button("新增");
Button btnUpdateMed = new Button("修改");
Button btnDeleteMed = new Button("删除");
Button btnClearForm = new Button("清空表单");
btnFormBox.getChildren().addAll(btnAddMed,btnUpdateMed,btnDeleteMed,btnClearForm);

// ========= 表格区域 =========
tvAdminMedicine = new TableView<>();
VBox.setVgrow(tvAdminMedicine, Priority.ALWAYS);

TableColumn<Medicine,String> colMedId = new TableColumn<>("药品编号");
colMedId.setCellValueFactory(new PropertyValueFactory<>("medicineId"));
colMedId.setMinWidth(110);
TableColumn<Medicine,String> colMedName = new TableColumn<>("药品名称");
colMedName.setCellValueFactory(new PropertyValueFactory<>("medicineName"));
colMedName.setMinWidth(160);
TableColumn<Medicine,Number> colPrice = new TableColumn<>("单价");
colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
colPrice.setMinWidth(110);
TableColumn<Medicine,Number> colStock = new TableColumn<>("当前库存");
colStock.setCellValueFactory(new PropertyValueFactory<>("stock"));
colStock.setMinWidth(100);

tvAdminMedicine.getColumns().addAll(colMedId,colMedName,colPrice,colStock);
styleTable(tvAdminMedicine);

// 点击表格行，回填表单
tvAdminMedicine.getSelectionModel().selectedItemProperty().addListener((obs,oldVal,newVal)->{
    if(newVal != null){
        tfMedId.setText(newVal.getMedicineId());
        tfMedName.setText(newVal.getMedicineName());
        tfDept.setText(newVal.getDepartment());
        tfPrice.setText(String.valueOf(newVal.getPrice()));
        tfStock.setText(String.valueOf(newVal.getStock()));
    }
});

// ========= 表单按钮事件 =========
// 清空表单
btnClearForm.setOnAction(e->{
    tfMedId.clear();
    tfMedName.clear();
    tfDept.clear();
    tfPrice.clear();
    tfStock.clear();
    tvAdminMedicine.getSelectionModel().clearSelection();
});

//新增药品
btnAddMed.setOnAction(e->{
    String medId = tfMedId.getText().trim();
    String medName = tfMedName.getText().trim();
    String dept = tfDept.getText().trim();
    double price;
    int stock;
    try{
        price = Double.parseDouble(tfPrice.getText().trim());
        stock = Integer.parseInt(tfStock.getText().trim());
        if(medId.isBlank() || medName.isBlank() || dept.isBlank() || price<0 || stock<0){
            showAlert(Alert.AlertType.WARNING,"输入校验","编号/名称/科室不能为空，单价库存不能负数");
            return;
        }
    }catch (Exception ex){
        showAlert(Alert.AlertType.WARNING,"输入错误","单价必须是小数，库存必须是整数");
        return;
    }
    Medicine newMed = new Medicine();
    new Thread(()->{
        try {
            Message msg=_hospitalSrv.adminAddMedicine(newMed);
            Platform.runLater(()->{
                if(IConstant.STATUS_SUCCESS.equals(msg.getStatusCode())){
                    showAlert(Alert.AlertType.INFORMATION,"成功","新增药品完成");
                    loadAdminMedicineStockList();
                    btnClearForm.fire();
                }else{
                    showAlert(Alert.AlertType.WARNING,"失败",String.valueOf(msg.getData()));
                }
            });
        }catch (Exception ex){
            Platform.runLater(()->showAlert(Alert.AlertType.ERROR,"网络异常","新增药品失败"));
        }
    }).start();
});

//修改药品
btnUpdateMed.setOnAction(e->{
    String medId = tfMedId.getText().trim();
    String medName = tfMedName.getText().trim();
    String dept = tfDept.getText().trim();
    double price;
    int stock;
    try{
        price = Double.parseDouble(tfPrice.getText().trim());
        stock = Integer.parseInt(tfStock.getText().trim());
        if(medId.isBlank() || medName.isBlank() || dept.isBlank() || price<0 || stock<0){
            showAlert(Alert.AlertType.WARNING,"输入校验","编号/名称/科室不能为空，单价库存不能负数");
            return;
        }
    }catch (Exception ex){
        showAlert(Alert.AlertType.WARNING,"输入错误","单价必须是小数，库存必须是整数");
        return;
    }
    Medicine updateMed = new Medicine();
    new Thread(()->{
        try {
            Message msg=_hospitalSrv.adminUpdateMedicine(updateMed);
            Platform.runLater(()->{
                if(IConstant.STATUS_SUCCESS.equals(msg.getStatusCode())){
                    showAlert(Alert.AlertType.INFORMATION,"成功","修改药品完成");
                    loadAdminMedicineStockList();
                    btnClearForm.fire();
                }else{
                    showAlert(Alert.AlertType.WARNING,"失败",String.valueOf(msg.getData()));
                }
            });
        }catch (Exception ex){
            Platform.runLater(()->showAlert(Alert.AlertType.ERROR,"网络异常","修改药品失败"));
        }
    }).start();
});

//删除药品
btnDeleteMed.setOnAction(e->{
    String medId = tfMedId.getText().trim();
    if(medId.isBlank()){
        showAlert(Alert.AlertType.WARNING,"提示","请先选中表格药品或者填写药品编号");
        return;
    }
    Optional<ButtonType> res = new Alert(Alert.AlertType.CONFIRMATION).showAndWait();
    if(res.isPresent() && res.get() == ButtonType.OK){
        new Thread(()->{
            try {
                Message msg=_hospitalSrv.adminDeleteMedicine(medId);
                Platform.runLater(()->{
                    if(IConstant.STATUS_SUCCESS.equals(msg.getStatusCode())){
                        showAlert(Alert.AlertType.INFORMATION,"成功","删除药品完成");
                        loadAdminMedicineStockList();
                        btnClearForm.fire();
                    }else{
                        showAlert(Alert.AlertType.WARNING,"失败",String.valueOf(msg.getData()));
                    }
                });
            }catch (Exception ex){
                Platform.runLater(()->showAlert(Alert.AlertType.ERROR,"网络异常","删除药品失败"));
            }
        }).start();
    }
});

panelAdminMedicineStock.getChildren().addAll(labAdminMed, formPane, btnFormBox, tvAdminMedicine);
VBox.setVgrow(panelAdminMedicineStock, Priority.ALWAYS);

    }
        /** 加载当前登录用户未取药处方，渲染卡片，同时刷新余额 */
    private void loadMyNoTakePrescription(){
        new Thread(()->{
            try {
                //1 获取内存模拟余额
                Message balMsg = _hospitalSrv.getMemBalance(_loginUserId);
                double balance = 0.0;
                if(IConstant.STATUS_SUCCESS.equals(balMsg.getStatusCode())){
                    balance = (Double) balMsg.getData();
                }
                //2 查询未缴费处方
                Message resp = _hospitalSrv.queryUserNoTakePres(_loginUserId);
                List<Prescription> list = null;
                if(IConstant.STATUS_SUCCESS.equals(resp.getStatusCode())){
                    list = (List<Prescription>) resp.getData();
                }
                double totalAll = 0.0;
                if(list != null){
                    for(Prescription p : list){
                        if(p.getSubTotal() != null){
                            totalAll += p.getSubTotal();
                        }
                    }
                }
                double finalBal = balance;
                double finalTotal = totalAll;
                List<Prescription> finalList = list;
                Platform.runLater(()->{
                    _labUserBalance.setText("校园卡余额：¥ " + String.format("%.2f", finalBal));
                    labTotalAmount.setText("处方合计：¥ " + String.format("%.2f", finalTotal));
                    _prescriptionCards.getChildren().clear();
                    if(finalList == null || finalList.isEmpty()){
                        showAlert(Alert.AlertType.INFORMATION, "提示", "暂无待缴费处方");
                        return;
                    }
                    //循环渲染卡片，模仿商店
for(Prescription pres : finalList){
    VBox card = new VBox(8);
    card.setPrefWidth(220);
    card.setPadding(new Insets(12));
    card.setStyle("-fx-background-color:#ffffff;-fx-background-radius:10;"
            + "-fx-border-color:#dce4ec;-fx-border-width:1px;"
            + "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.04),6,0,0,2);");

    // ==========这里重点：替换原来的Rectangle色块，调用图片加载方法 ==========
    StackPane medicineImage = buildMedicineImage(pres.getMedicineId());

    Label labMedName = new Label(pres.getMedicineName());
    labMedName.setFont(Font.font("System", FontWeight.BOLD, 14));
    labMedName.setWrapText(true);
    Label labNum = new Label("开药数量：" + pres.getMedicineNum());
    double sub = pres.getSubTotal() != null ? pres.getSubTotal() : 0.0;
    Label labSub = new Label("小计：¥ " + String.format("%.2f", sub));
    labSub.setFont(Font.font("System", FontWeight.BOLD, 13));

    HBox btnBox = new HBox();
    btnBox.setAlignment(Pos.CENTER);
    Button btnPayTake = new Button("💰支付并取药");
    btnPayTake.setStyle("-fx-background-color:#27ae60;-fx-text-fill:white;-fx-background-radius:5;");
    btnPayTake.setOnAction(e -> doPayAndTakePrescription(pres.getPresId(), sub));
    btnBox.getChildren().add(btnPayTake);

    // 加入卡片，注意：第一个是medicineImage
    card.getChildren().addAll(medicineImage, labMedName, labNum, labSub, btnPayTake);
    _prescriptionCards.getChildren().add(card);
}

                });
            } catch (Exception e){
                Platform.runLater(()->showAlert(Alert.AlertType.ERROR, "网络异常", "加载处方出错：" + e.getMessage()));
            }
        }).start();
    }

    /** 支付并取药（内存模拟余额，支付成功才标记已取药） */
    private void doPayAndTakePrescription(String presId, double totalMoney){
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("确认支付药费");
        confirm.setContentText("确认支付药费 ¥" + String.format("%.2f", totalMoney) + "，支付成功标记处方为已取药");
        Optional<ButtonType> res = confirm.showAndWait();
        if(res.isEmpty() || res.get() != ButtonType.OK){
            return;
        }
        new Thread(()->{
            try {
                Message resp = _hospitalSrv.memPayPrescription(presId, _loginUserId, totalMoney);
                Platform.runLater(()->{
                    if(IConstant.STATUS_SUCCESS.equals(resp.getStatusCode())){
                        showAlert(Alert.AlertType.INFORMATION, "成功", String.valueOf(resp.getData()));
                        loadMyNoTakePrescription();
                    } else {
                        showAlert(Alert.AlertType.WARNING, "操作失败", String.valueOf(resp.getData()));
                    }
                });
            } catch (Exception e){
                Platform.runLater(()->showAlert(Alert.AlertType.ERROR, "异常", e.getMessage()));
            }
        }).start();
    }




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

    /**
     * 加载当前医生待就诊预约列表
     */
    private void loadDoctorPendingAppointList(){
        new Thread(()->{
            try {
                Message resp = _hospitalSrv.queryDoctorPendingAppoint(_loginUserId);
                Platform.runLater(()->{
                    if(IConstant.STATUS_SUCCESS.equals(resp.getStatusCode())){
                        List<Appointment> list = (List<Appointment>) resp.getData();
                        tvDoctorPendingAppoint.getItems().clear();
                        tvDoctorPendingAppoint.getItems().addAll(list);
                        if(list.isEmpty()){
                            showAlert(Alert.AlertType.INFORMATION,"提示","当前暂无待就诊患者");
                        }
                    }else{
                        showAlert(Alert.AlertType.ERROR,"获取待就诊记录失败",String.valueOf(resp.getData()));
                    }
                });
            }catch (Exception e){
                Platform.runLater(()->showAlert(Alert.AlertType.ERROR,"网络异常","查询待就诊记录异常"));
            }
        }).start();
    }

    /**
 * 开药弹窗，展示库存，库存0禁用
 * @param appointId 预约id
 * @param userId 用户id
 * @param doctorId 当前医生登录id
 */
private void openPrescriptionDialog(String appointId,String userId,String doctorId){
    Dialog<List<Prescription>> dialog = new Dialog<>();
    dialog.setTitle("开具处方");
    dialog.setHeaderText("请选择需要开具的药品，并填写数量（灰色=库存为0，不可选）");
    dialog.setResizable(true);
    dialog.getDialogPane().setPrefWidth(600);
    dialog.getDialogPane().setPrefHeight(480);

    VBox vBoxContent = new VBox(12);
    vBoxContent.setPadding(new Insets(15));
    vBoxContent.setPrefWidth(570);
    vBoxContent.setPrefHeight(430);

    new Thread(()->{
        try {
            Message resp = _hospitalSrv.queryAllMedicine();
            Platform.runLater(()->{
                if(IConstant.STATUS_SUCCESS.equals(resp.getStatusCode())){
                    List<Medicine> medList = (List<Medicine>) resp.getData();
                    for(Medicine m : medList){
                        HBox row = new HBox(12);
                        row.setPrefWidth(500);
                        CheckBox cbMed = new CheckBox(m.getMedicineName()+" | 库存："+m.getStock()+" | ¥"+m.getPrice());
                        cbMed.setPrefWidth(380);
                        if(m.getStock() <=0){
                            cbMed.setDisable(true);
                        }
                        TextField tfNum = new TextField("1");
                        tfNum.setPrefWidth(80);
                        tfNum.setPromptText("数量");
                        if(m.getStock() <=0){
                            tfNum.setDisable(true);
                        }
                        cbMed.setUserData(m);
                        row.getChildren().addAll(cbMed,new Label("数量:"),tfNum);
                        vBoxContent.getChildren().add(row);
                    }
                }
            });
        }catch (Exception ex){
            Platform.runLater(()->showAlert(Alert.AlertType.ERROR,"错误","获取药品列表失败"));
        }
    }).start();

    dialog.getDialogPane().setContent(vBoxContent);
    dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK,ButtonType.CANCEL);

    Optional<List<Prescription>> result = dialog.showAndWait();
    if(result.isPresent()){
        List<Prescription> presOutList = new ArrayList<>();
        for(var node : vBoxContent.getChildren()){
            if(node instanceof HBox rowBox){
                CheckBox cb = (CheckBox) rowBox.getChildren().get(0);
                TextField tfNum = (TextField) rowBox.getChildren().get(2);
                if(cb.isSelected() && !cb.isDisabled()){
                    Medicine med = (Medicine) cb.getUserData();
                    int num;
                    try{
                        num = Integer.parseInt(tfNum.getText().trim());
                        if(num <=0) continue;
                    }catch (Exception e){
                        continue;
                    }
                    Prescription p = new Prescription();
                    p.setAppointId(appointId);
                    p.setDoctorId(doctorId);
                    p.setUserId(userId);
                    p.setMedicineId(med.getMedicineId());
                    p.setMedicineName(med.getMedicineName());
                    p.setMedicineNum(num);
                    presOutList.add(p);
                }
            }
        }
        if(presOutList.isEmpty()){
            showAlert(Alert.AlertType.WARNING,"提示","没有勾选有效的药品，处方放弃保存");
            return;
        }
        new Thread(()->{
            try {
                Message resp = _hospitalSrv.savePrescriptionBatch(presOutList);
                Platform.runLater(()->{
                    if(IConstant.STATUS_SUCCESS.equals(resp.getStatusCode())){
                        showAlert(Alert.AlertType.INFORMATION,"开药成功",String.valueOf(resp.getData()));
                        loadDoctorPendingAppointList();
                    }else{
                        showAlert(Alert.AlertType.WARNING,"开药失败",String.valueOf(resp.getData()));
                    }
                });
            }catch (Exception e){
                Platform.runLater(()->showAlert(Alert.AlertType.ERROR,"异常",e.getMessage()));
            }
        }).start();
    }
}

/** 用户确认取药 */
private void doTakeMedicine(String presId,String userId){
    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
    confirm.setTitle("确认取药");
    confirm.setContentText("确认完成取药？确认后该处方标记为已取药");
    Optional<ButtonType> res = confirm.showAndWait();
    if(res.isEmpty() || res.get()!=ButtonType.OK){
        return;
    }
    new Thread(()->{
        try {
            Message resp = _hospitalSrv.takeMedicine(presId,userId);
            Platform.runLater(()->{
                if(IConstant.STATUS_SUCCESS.equals(resp.getStatusCode())){
                    showAlert(Alert.AlertType.INFORMATION,"成功",String.valueOf(resp.getData()));
                    loadMyNoTakePrescription();
                }else{
                    showAlert(Alert.AlertType.WARNING,"操作失败",String.valueOf(resp.getData()));
                }
            });
        }catch (Exception e){
            Platform.runLater(()->showAlert(Alert.AlertType.ERROR,"异常",e.getMessage()));
        }
    }).start();
}

    /**
     * 执行完成就诊，待就诊改为已就诊
     * @param appointId 预约编号
     * @param doctorId 当前登录医生ID
     */
    private void doFinishAppoint(String appointId,String doctorId){
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("确认完成就诊");
        confirm.setContentText("确认将该条记录设置为【已就诊】？");
        Optional<ButtonType> res = confirm.showAndWait();
        if(res.isEmpty() || res.get()!=ButtonType.OK){
            return;
        }
        new Thread(()->{
            try {
                Message resp = _hospitalSrv.finishAppointment(appointId,doctorId);
                Platform.runLater(()->{
                    if(IConstant.STATUS_SUCCESS.equals(resp.getStatusCode())){
                        showAlert(Alert.AlertType.INFORMATION,"成功",String.valueOf(resp.getData()));
                        loadDoctorPendingAppointList();
                    }else{
                        showAlert(Alert.AlertType.WARNING,"操作失败",String.valueOf(resp.getData()));
                    }
                });
            }catch (Exception e){
                Platform.runLater(()->showAlert(Alert.AlertType.ERROR,"异常",e.getMessage()));
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
        String skill = tfAddDocSkill.getText().trim();

        if (id.isBlank() || name.isBlank() || dept.isBlank() || title.isBlank()) {
            showAlert(Alert.AlertType.WARNING, "提示", "医生ID、姓名、科室、职称不能为空");
            return;
        }
        Doctor doc = new Doctor();
        doc.setDoctorId(id);
        doc.setName(name);
        doc.setDepartment(dept);
        doc.setTitle(title);
        doc.setSkill(skill);

        new Thread(() -> {
            try {
                Message resp = _hospitalSrv.addDoctor(_loginUserId, doc);
                Platform.runLater(() -> {
                    if (IConstant.STATUS_SUCCESS.equals(resp.getStatusCode())) {
                        showAlert(Alert.AlertType.INFORMATION, "成功", String.valueOf(resp.getData()));
                        actionQueryAllDoctor();
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
        String skill = tfUpdDocSkill.getText().trim();

        if (id.isBlank()) {
            showAlert(Alert.AlertType.WARNING, "提示", "医生ID不能为空");
            return;
        }
        Doctor doc = new Doctor();
        doc.setDoctorId(id);
        doc.setName(name);
        doc.setDepartment(dept);
        doc.setTitle(title);
        doc.setSkill(skill);

        new Thread(() -> {
            try {
                Message resp = _hospitalSrv.updateDoctor(_loginUserId, doc);
                Platform.runLater(() -> {
                    if (IConstant.STATUS_SUCCESS.equals(resp.getStatusCode())) {
                        showAlert(Alert.AlertType.INFORMATION, "成功", String.valueOf(resp.getData()));
                        actionQueryAllDoctor();
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
    /**加载管理员药品库存列表*/
private void loadAdminMedicineStockList(){
    new Thread(()->{
        try {
            Message resp = _hospitalSrv.adminQueryAllMedicine();
            Platform.runLater(()->{
                if(IConstant.STATUS_SUCCESS.equals(resp.getStatusCode())){
                    List<Medicine> list = (List<Medicine>) resp.getData();
                    tvAdminMedicine.getItems().clear();
                    tvAdminMedicine.getItems().addAll(list);
                }else{
                    showAlert(Alert.AlertType.ERROR,"加载失败",String.valueOf(resp.getData()));
                }
            });
        }catch (Exception e){
            Platform.runLater(()->showAlert(Alert.AlertType.ERROR,"网络异常","加载药品列表出错"));
        }
    }).start();
}

/**弹出修改库存弹窗*/
private void openEditStockDialog(String medId,String medName,int oldStock){
    TextInputDialog dialog=new TextInputDialog(String.valueOf(oldStock));
    dialog.setTitle("修改药品库存");
    dialog.setHeaderText("药品："+medName+"("+medId+")");
    dialog.setContentText("请输入新库存(≥0的整数)：");
    Optional<String> res=dialog.showAndWait();
    if(res.isEmpty()){
        return;
    }
    String inputStr=res.get().trim();
    int newStock;
    try{
        newStock=Integer.parseInt(inputStr);
        if(newStock<0){
            showAlert(Alert.AlertType.WARNING,"输入错误","库存不能是负数！");
            return;
        }
    }catch (NumberFormatException ex){
        showAlert(Alert.AlertType.WARNING,"输入错误","请输入合法整数数字");
        return;
    }
    //提交修改
    new Thread(()->{
        try {
            Message msg=_hospitalSrv.adminUpdateMedicineStock(medId,newStock);
            Platform.runLater(()->{
                if(IConstant.STATUS_SUCCESS.equals(msg.getStatusCode())){
                    showAlert(Alert.AlertType.INFORMATION,"成功","库存修改完成");
                    loadAdminMedicineStockList();
                }else{
                    showAlert(Alert.AlertType.WARNING,"失败",String.valueOf(msg.getData()));
                }
            });
        }catch (Exception e){
            Platform.runLater(()->showAlert(Alert.AlertType.ERROR,"异常","修改库存网络异常"));
        }
    }).start();
}
    /** 根据药品编号构建药品图片容器，加载失败显示占位色块 */
    private StackPane buildMedicineImage(String medicineId) {
        StackPane imgHolder = new StackPane();
        imgHolder.setPrefSize(180,180);
        imgHolder.setMinSize(180,180);
        imgHolder.setMaxSize(180,180);
        // 默认占位背景色
        String bgColor = "#73b8e8";
        imgHolder.setStyle("-fx-background-radius:10; -fx-background-color:"+bgColor+";");
        Rectangle clip = new Rectangle(180,180);
        clip.setArcWidth(12);
        clip.setArcHeight(12);
        imgHolder.setClip(clip);
        //占位emoji
        Label emojiLabel = new Label("💊");
        emojiLabel.setFont(Font.font("System",48));
        imgHolder.getChildren().add(emojiLabel);

        // 后台线程加载图片，路径 /vcampus/client/view/assets/hospital/M001.jpg
        String resPath = "/vcampus/client/view/assets/hospital/"+medicineId+".jpg";
        new Thread(() -> {
            var url = getClass().getResource(resPath);
            if(url != null){
                Image image = new Image(url.toExternalForm(),180,180,true,true,false);
                if(!image.isError()){
                    ImageView iv = new ImageView(image);
                    iv.setFitWidth(180);
                    iv.setFitHeight(180);
                    iv.setPreserveRatio(true);
                    Platform.runLater(()->{
                        imgHolder.setStyle(""); //清除占位背景
                        imgHolder.getChildren().setAll(iv);
                    });
                }
            }
        }).start();
        return imgHolder;
    }

}