/*
 * CourseViewSupport
 *
 * Version 1.1
 *
 * 2026-09-08
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.client.view.course;

import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.Region;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/** Course JavaFX 页面共用的异步调用、表格列、样式和格式化工具。 */
final class CourseViewSupport {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private CourseViewSupport() {
    }

    /** 在后台线程执行网络操作，并在 JavaFX 线程更新页面。 */
    static <T> void runAsync(Region owner, CheckedSupplier<T> operation,
                             Consumer<T> onSuccess) {
        owner.setDisable(true);
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return operation.get();
            }
        };
        task.setOnSucceeded(event -> {
            owner.setDisable(false);
            onSuccess.accept(task.getValue());
        });
        task.setOnFailed(event -> {
            owner.setDisable(false);
            showError(task.getException());
        });
        Thread thread = new Thread(task, "course-ui-request");
        thread.setDaemon(true);
        thread.start();
    }

    /** 创建使用字符串映射的表格列。 */
    static <T> TableColumn<T, String> textColumn(
            String title, double width, Function<T, String> mapper) {
        TableColumn<T, String> column = new TableColumn<>(title);
        column.setPrefWidth(width);
        column.setCellValueFactory(cell ->
                new SimpleStringProperty(safe(mapper.apply(cell.getValue()), "—")));
        return column;
    }

    /** 应用 Course 表格的统一行为与空状态。 */
    static <T> void configureTable(TableView<T> table, String emptyText) {
        table.getStyleClass().add("course-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        Label placeholder = new Label(emptyText);
        placeholder.getStyleClass().add("empty-state-label");
        table.setPlaceholder(placeholder);
    }

    /** 为对话框加载 Course 样式。 */
    static void styleDialog(DialogPane pane) {
        String stylesheet = stylesheet();
        if (stylesheet != null && !pane.getStylesheets().contains(stylesheet)) {
            pane.getStylesheets().add(stylesheet);
        }
        pane.getStyleClass().add("course-dialog");
    }

    /** 将星期数字转换成中文。 */
    static String dayName(int dayOfWeek) {
        String[] names = {"", "周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        return dayOfWeek >= 1 && dayOfWeek <= 7 ? names[dayOfWeek] : "未知";
    }

    /** 格式化课程时间。 */
    static String timeRange(LocalTime start, LocalTime end) {
        return formatTime(start) + "–" + formatTime(end);
    }

    /** 格式化选课时间。 */
    static String dateTime(LocalDateTime value) {
        return value == null ? "—" : value.format(DATE_TIME_FORMAT);
    }

    /** 解析表单中的 HH:mm 时间。 */
    static LocalTime parseTime(String value) {
        return LocalTime.parse(value == null ? "" : value.trim(), TIME_FORMAT);
    }

    /** 返回非空展示文本。 */
    static String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    /** 显示网络或业务错误。 */
    static void showError(Throwable throwable) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("操作失败");
        alert.setHeaderText("未能完成本次操作");
        String message = throwable == null ? null : throwable.getMessage();
        alert.setContentText(message == null || message.isBlank() ? "未知错误" : message);
        styleDialog(alert.getDialogPane());
        alert.showAndWait();
    }

    /** 在删除等危险操作前显示统一确认框。 */
    static boolean confirm(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("请确认");
        alert.setHeaderText(title);
        alert.setContentText(content);
        styleDialog(alert.getDialogPane());
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    /** Course 样式表的 classpath URL；缺失时安全降级为 JavaFX 默认样式。 */
    static String stylesheet() {
        var resource = CourseViewSupport.class.getResource("course.css");
        return resource == null ? null : resource.toExternalForm();
    }

    private static String formatTime(LocalTime time) {
        return time == null ? "--:--" : time.format(TIME_FORMAT);
    }

    /** 可抛出受检异常的后台操作。 */
    @FunctionalInterface
    interface CheckedSupplier<T> {
        T get() throws Exception;
    }
}
