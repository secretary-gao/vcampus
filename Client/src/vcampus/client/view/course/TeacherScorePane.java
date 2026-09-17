package vcampus.client.view.course;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import vcampus.client.biz.ICourseClientSrv;
import vcampus.common.vo.CourseScore;
import vcampus.common.vo.TeacherCourseEnrollment;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 教师成绩录入：区分未录入、本地未提交、等待审核与已发布。 */
public class TeacherScorePane extends VBox {
    private static final String UNENTERED = "UNENTERED";
    private static final String DRAFT = "LOCAL_DRAFT";
    private final ICourseClientSrv client;
    private final String teacher;
    private final ComboBox<String> classes = new ComboBox<>();
    private final TableView<ScoreRow> table = new TableView<>();
    private final Label status = new Label();

    public TeacherScorePane(ICourseClientSrv client, String teacher) {
        this.client = client;
        this.teacher = teacher;
        getStyleClass().add("course-page");
        Label title = new Label("成绩录入");
        title.getStyleClass().add("page-title");
        Label hint = new Label("双击成绩单元格录入；输入后先保存为“未提交”，点击提交后才进入管理员审核");
        hint.getStyleClass().add("page-description");
        classes.setPromptText("选择教学班");
        classes.setOnAction(event -> loadRows());
        HBox.setHgrow(classes, Priority.ALWAYS);
        Button refresh = button("刷新", "secondary", this::refresh);
        Button submit = button("提交未审核成绩", "primary", this::submit);
        HBox bar = new HBox(9, classes, refresh, submit);
        bar.getStyleClass().add("tool-bar-card");

        table.setEditable(true);
        table.getColumns().addAll(
                CourseViewSupport.textColumn("学号", 135, row -> row.score.getStudentId()),
                CourseViewSupport.textColumn("姓名", 110, row -> row.score.getStudentName()),
                CourseViewSupport.textColumn("课程", 190, row -> row.score.getCourseName()),
                scoreColumn(),
                CourseViewSupport.textColumn("状态", 150, row -> statusText(row.score.getStatus())));
        CourseViewSupport.configureTable(table, "请选择有学生的教学班");
        table.setMinHeight(360);
        VBox.setVgrow(table, Priority.ALWAYS);
        status.getStyleClass().add("status-label");
        getChildren().addAll(title, hint, bar, table, status);
        refresh();
    }

    private TableColumn<ScoreRow, String> scoreColumn() {
        TableColumn<ScoreRow, String> column = new TableColumn<>("成绩（双击录入）");
        column.setPrefWidth(135);
        column.setCellValueFactory(value -> new SimpleStringProperty(
                value.getValue().score.getScore() == null
                        ? "" : String.valueOf(value.getValue().score.getScore())));
        column.setCellFactory(TextFieldTableCell.forTableColumn());
        column.setOnEditCommit(event -> {
            ScoreRow row = event.getRowValue();
            String persistedStatus = row.score.getStatus();
            if ("PENDING".equals(persistedStatus) || "APPROVED".equals(persistedStatus)) {
                CourseViewSupport.showError(new IllegalStateException(
                        "等待审核或已发布的成绩不能修改；如被拒绝，可修改后重新提交。"));
                table.refresh();
                return;
            }
            String text = event.getNewValue() == null ? "" : event.getNewValue().trim();
            if (text.isBlank()) {
                row.score.setScore(null);
                row.score.setStatus(UNENTERED);
                table.refresh();
                status.setText("已清空成绩，当前为“未录入”状态");
                return;
            }
            try {
                int value = Integer.parseInt(text);
                if (value < 0 || value > 100) {
                    throw new IllegalArgumentException("成绩必须在 0 到 100 之间");
                }
                row.score.setScore(value);
                row.score.setStatus(DRAFT);
                table.refresh();
                status.setText("成绩已录入但尚未提交；确认后请点击“提交未审核成绩”");
            } catch (NumberFormatException exception) {
                CourseViewSupport.showError(new IllegalArgumentException("成绩必须是 0 到 100 的整数"));
                table.refresh();
            } catch (IllegalArgumentException exception) {
                CourseViewSupport.showError(exception);
                table.refresh();
            }
        });
        return column;
    }

    private void refresh() {
        status.setText("正在读取本人教学班…");
        CourseViewSupport.runAsync(this, () -> client.queryTeacherCourseEnrollments(teacher), rows -> {
            String previous = classes.getValue();
            List<String> ids = rows.stream().map(TeacherCourseEnrollment::getTeachingClassId)
                    .filter(value -> value != null).distinct().toList();
            classes.setItems(FXCollections.observableArrayList(ids));
            if (previous != null && ids.contains(previous)) classes.setValue(previous);
            else if (!ids.isEmpty()) classes.setValue(ids.get(0));
            else {
                table.getItems().clear();
                status.setText("当前没有本人负责的教学班");
            }
        });
    }

    private void loadRows() {
        String classId = classes.getValue();
        if (classId == null) return;
        status.setText("正在读取成绩…");
        CourseViewSupport.runAsync(this, () -> {
            List<TeacherCourseEnrollment> roster = client.queryTeacherCourseEnrollments(teacher)
                    .stream().filter(value -> classId.equals(value.getTeachingClassId())
                            && value.getStudentId() != null).toList();
            Map<String, CourseScore> stored = new LinkedHashMap<>();
            for (CourseScore score : client.queryTeacherScores(teacher)) {
                stored.put(score.getStudentId() + "@" + score.getTeachingClassId(), score);
            }
            return roster.stream().map(enrollment -> {
                CourseScore score = stored.get(enrollment.getStudentId() + "@" + classId);
                if (score == null) {
                    score = new CourseScore();
                    score.setStudentId(enrollment.getStudentId());
                    score.setStudentName(enrollment.getStudentName());
                    score.setCourseId(enrollment.getCourseId());
                    score.setCourseName(enrollment.getCourseName());
                    score.setTeachingClassId(classId);
                    score.setTeacher(teacher);
                    score.setStatus(UNENTERED);
                }
                return new ScoreRow(score);
            }).toList();
        }, rows -> {
            table.setItems(FXCollections.observableArrayList(rows));
            long blank = rows.stream().filter(row -> row.score.getScore() == null).count();
            long pending = rows.stream().filter(row -> "PENDING".equals(row.score.getStatus())).count();
            status.setText("当前教学班 " + rows.size() + " 名学生 · 未录入 "
                    + blank + " · 等待审核 " + pending);
        });
    }

    private void submit() {
        List<CourseScore> values = table.getItems().stream().map(row -> row.score)
                .filter(score -> DRAFT.equals(score.getStatus()) && score.getScore() != null).toList();
        if (values.isEmpty()) {
            CourseViewSupport.showError(new IllegalArgumentException(
                    "当前没有“未提交”的成绩；请先双击空白成绩单元格录入。"));
            return;
        }
        CourseViewSupport.runAsync(this, () -> client.submitScores(teacher, values), count -> {
            status.setText("已提交 " + count + " 条成绩，正在等待管理员审核");
            loadRows();
        });
    }

    private String statusText(String value) {
        return switch (value == null ? UNENTERED : value) {
            case DRAFT -> "未提交";
            case "PENDING" -> "等待管理员审核";
            case "APPROVED" -> "已发布";
            case "REJECTED" -> "已拒绝（修改后可重提）";
            default -> "未录入";
        };
    }

    private static Button button(String text, String style, Runnable action) {
        Button button = new Button(text);
        button.getStyleClass().add(style);
        button.setOnAction(event -> action.run());
        return button;
    }

    private static class ScoreRow {
        private final CourseScore score;
        private ScoreRow(CourseScore score) { this.score = score; }
    }
}
