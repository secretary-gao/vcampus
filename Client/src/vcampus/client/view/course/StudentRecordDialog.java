package vcampus.client.view.course;

import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import vcampus.client.biz.IStudentClientSrv;
import vcampus.client.biz.StudentClientException;
import vcampus.common.constant.StudentProtocol;
import vcampus.common.vo.Student;

/** 从选课名单查看学籍，每次打开都通过学籍服务重新校验授课关系。 */
final class StudentRecordDialog extends Dialog<Void> {

    private final IStudentClientSrv _client;
    private final String _studentId;
    private final VBox _content = new VBox(12);
    private final Button _reloadButton;
    private Task<Student> _request;

    StudentRecordDialog(Window owner, IStudentClientSrv client, String studentId) {
        _client = client;
        _studentId = studentId;
        initOwner(owner);
        setTitle("学生学籍");
        setHeaderText("学生学籍 · 只读");
        setResizable(true);
        getDialogPane().setId("teacher-student-record-dialog");
        getDialogPane().setPrefWidth(480);
        getDialogPane().setMinWidth(380);
        _content.setMinHeight(260);
        getDialogPane().setContent(_content);
        ButtonType reload = new ButtonType("重新加载", ButtonData.LEFT);
        getDialogPane().getButtonTypes().addAll(reload,
                new ButtonType("关闭", ButtonData.CANCEL_CLOSE));
        _reloadButton = (Button) getDialogPane().lookupButton(reload);
        _reloadButton.addEventFilter(ActionEvent.ACTION, event -> {
            event.consume();
            loadRecord();
        });
        CourseViewSupport.styleDialog(getDialogPane());
        setOnShown(event -> loadRecord());
        setOnHidden(event -> {
            if (_request != null) {
                _request.cancel();
            }
        });
    }

    private void loadRecord() {
        _reloadButton.setDisable(true);
        ProgressIndicator progress = new ProgressIndicator();
        progress.setMaxSize(28, 28);
        _content.getChildren().setAll(progress, message("正在读取学生学籍…"));
        _request = new Task<>() {
            @Override
            protected Student call() throws Exception {
                return _client.findByStudentId(_studentId);
            }
        };
        _request.setOnSucceeded(event -> {
            if (!isShowing()) {
                return;
            }
            _reloadButton.setDisable(false);
            Student student = _request.getValue();
            if (student == null) {
                _content.getChildren().setAll(message(
                        "未找到可查看的学籍。该学生可能已退课，或学籍信息已变更，请关闭并刷新选课名单。"));
                return;
            }
            showRecord(student);
        });
        _request.setOnFailed(event -> {
            if (isShowing()) {
                _reloadButton.setDisable(false);
                boolean forbidden = _request.getException() instanceof StudentClientException error
                        && StudentProtocol.STATUS_FORBIDDEN.equals(error.getStatusCode());
                _content.getChildren().setAll(message(forbidden ? "当前账号无法查看学籍。"
                                : "学籍加载失败，请检查连接后重新加载。"),
                        message(CourseViewSupport.safe(_request.getException().getMessage(), "连接失败")));
            }
        });
        Thread thread = new Thread(_request, "teacher-student-record");
        thread.setDaemon(true);
        thread.start();
    }

    private void showRecord(Student student) {
        GridPane details = new GridPane();
        details.getStyleClass().add("form-grid");
        ColumnConstraints titles = new ColumnConstraints(90);
        ColumnConstraints values = new ColumnConstraints();
        values.setHgrow(Priority.ALWAYS);
        values.setMinWidth(0);
        details.getColumnConstraints().addAll(titles, values);
        addField(details, 0, "学号", student.getStudentId());
        addField(details, 1, "姓名", student.getName());
        addField(details, 2, "班级", student.getClassName());
        addField(details, 3, "专业", student.getMajor());
        addField(details, 4, "年级", student.getGrade());
        addField(details, 5, "学籍状态", student.getStatus() == null
                ? null : student.getStatus().toString());
        _content.getChildren().setAll(details,
                message("如需更正学籍信息，请联系管理员。"));
    }

    private void addField(GridPane grid, int row, String title, String value) {
        Label label = new Label(title);
        label.getStyleClass().add("field-label");
        Label text = message(CourseViewSupport.safe(value, "—"));
        text.setAccessibleText(title + "：" + text.getText());
        grid.addRow(row, label, text);
    }

    private Label message(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setMaxWidth(Double.MAX_VALUE);
        return label;
    }
}
