package vcampus.client.view.course;

import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import vcampus.client.biz.ICourseClientSrv;
import vcampus.common.vo.CourseDashboardStats;
import vcampus.common.vo.TeachingClassStatistic;

import java.io.File;
import java.util.Locale;

/** Lightweight, database-backed academic operations dashboard. */
public class CourseDashboardPane extends VBox {
    private final ICourseClientSrv _client;
    private final GridPane _metrics = new GridPane();
    private final BarChart<String, Number> _popular = new BarChart<>(
            new CategoryAxis(), new NumberAxis());
    private final TableView<TeachingClassStatistic> _available = new TableView<>();
    private final Label _status = new Label("正在读取教务统计…");
    private CourseDashboardStats _stats;

    public CourseDashboardPane(ICourseClientSrv client) {
        _client = client;
        getStyleClass().add("course-page");
        buildView();
        refresh();
    }

    public void refresh() {
        _status.setText("正在读取教务统计…");
        CourseViewSupport.runAsync(this, _client::queryDashboard, this::render);
    }

    @SuppressWarnings("unchecked")
    private void buildView() {
        Label title = new Label("教务概览");
        title.getStyleClass().add("page-title");
        Label description = new Label("课程供给、选课规模与教学班容量的实时快照");
        description.getStyleClass().add("page-description");
        VBox heading = new VBox(3, title, description);
        HBox.setHgrow(heading, Priority.ALWAYS);
        Button export = new Button("导出教学班统计 CSV");
        export.getStyleClass().add("secondary");
        export.setOnAction(event -> export());
        Button refresh = new Button("刷新");
        refresh.getStyleClass().add("primary");
        refresh.setOnAction(event -> refresh());
        HBox header = new HBox(9, heading, export, refresh);
        header.setAlignment(Pos.CENTER_LEFT);

        _metrics.setHgap(12);
        _metrics.setVgap(12);
        _popular.setTitle("热门教学班 Top 5");
        _popular.getStyleClass().add("dashboard-popular-chart");
        _popular.setCategoryGap(18);
        _popular.setLegendVisible(false);
        _popular.setAnimated(false);
        _available.getColumns().addAll(
                CourseViewSupport.textColumn("教学班", 150, TeachingClassStatistic::teachingClassId),
                CourseViewSupport.textColumn("课程", 180, TeachingClassStatistic::courseName),
                CourseViewSupport.textColumn("教师", 100, TeachingClassStatistic::teacher),
                CourseViewSupport.textColumn("剩余名额", 90,
                        value -> String.valueOf(value.remaining())));
        CourseViewSupport.configureTable(_available, "暂无教学班数据");
        VBox tableCard = new VBox(8, label("剩余名额最多 Top 5", "section-title"), _available);
        tableCard.getStyleClass().add("course-card");
        VBox.setVgrow(_available, Priority.ALWAYS);
        HBox body = new HBox(14, _popular, tableCard);
        HBox.setHgrow(_popular, Priority.ALWAYS);
        HBox.setHgrow(tableCard, Priority.ALWAYS);
        _popular.setMaxWidth(Double.MAX_VALUE);
        tableCard.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(body, Priority.ALWAYS);
        _status.getStyleClass().add("status-label");
        getChildren().addAll(header, _metrics, body, _status);
    }

    private void render(CourseDashboardStats stats) {
        _stats = stats;
        _metrics.getChildren().clear();
        String[] labels = {"课程数", "教学班数", "选课学生", "选课记录",
                "平均容量", "平均已选", "满员班级", "容量利用率"};
        String[] values = {String.valueOf(stats.getCourseCount()),
                String.valueOf(stats.getTeachingClassCount()),
                String.valueOf(stats.getStudentCount()), String.valueOf(stats.getSelectionCount()),
                decimal(stats.getAverageCapacity()), decimal(stats.getAverageSelected()),
                String.valueOf(stats.getFullClassCount()), percent(stats.getCapacityUtilization())};
        for (int i = 0; i < labels.length; i++) _metrics.add(metric(labels[i], values[i]), i % 4, i / 4);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (TeachingClassStatistic row : stats.getPopularClasses()) {
            series.getData().add(new XYChart.Data<>(row.teachingClassId(), row.selectedCount()));
        }
        _popular.getData().setAll(series);
        _available.setItems(FXCollections.observableArrayList(stats.getAvailableClasses()));
        _status.setText("统计已更新 · 共 " + stats.getAllClasses().size() + " 个教学班");
    }

    private VBox metric(String title, String value) {
        Label name = label(title, "metric-label");
        Label number = label(value, "metric-value");
        VBox card = new VBox(5, name, number);
        card.getStyleClass().add("metric-card");
        card.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private void export() {
        if (_stats == null) return;
        FileChooser chooser = new FileChooser();
        chooser.setTitle("导出教学班选课统计");
        chooser.setInitialFileName("course-class-statistics.csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV 文件", "*.csv"));
        File file = chooser.showSaveDialog(getScene() == null ? null : getScene().getWindow());
        if (file == null) return;
        try {
            CourseCsvExporter.writeClassStatistics(file.toPath(), _stats.getAllClasses());
            _status.setText("已导出：" + file.getName());
        } catch (Exception exception) {
            new Alert(Alert.AlertType.ERROR, "导出失败：" + exception.getMessage()).showAndWait();
        }
    }

    private Label label(String text, String style) {
        Label value = new Label(text);
        value.getStyleClass().add(style);
        return value;
    }
    private String decimal(double value) { return String.format(Locale.ROOT, "%.1f", value); }
    private String percent(double value) { return String.format(Locale.ROOT, "%.1f%%", value * 100); }
}
