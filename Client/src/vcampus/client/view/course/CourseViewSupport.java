/*
 * CourseViewSupport
 *
 * Version 1.0
 *
 * 2026-09-04
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.client.view.course;

import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.TableColumn;
import javafx.scene.layout.Region;
import javafx.beans.property.SimpleStringProperty;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;
import java.util.function.Function;

/** Course JavaFX 页面共用的异步调用、表格列和格式化工具。 */
final class CourseViewSupport {

    static final String PRIMARY_BUTTON = "-fx-background-color: #1c6eb5; -fx-text-fill: white;"
            + " -fx-background-radius: 16; -fx-font-weight: bold; -fx-cursor: hand;";
    static final String SECONDARY_BUTTON = "-fx-background-color: #e8f1fa; -fx-text-fill: #1c5a97;"
            + " -fx-background-radius: 16; -fx-font-weight: bold; -fx-cursor: hand;";
    static final String DANGER_BUTTON = "-fx-background-color: #d9534f; -fx-text-fill: white;"
            + " -fx-background-radius: 16; -fx-font-weight: bold; -fx-cursor: hand;";

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

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
        column.setCellValueFactory(cell -> new SimpleStringProperty(mapper.apply(cell.getValue())));
        return column;
    }

    /** 将星期数字转换成中文。 */
    static String dayName(int dayOfWeek) {
        String[] names = {"", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日"};
        return dayOfWeek >= 1 && dayOfWeek <= 7 ? names[dayOfWeek] : "未知";
    }

    /** 格式化课程时间。 */
    static String timeRange(LocalTime start, LocalTime end) {
        return formatTime(start) + " - " + formatTime(end);
    }

    /** 解析表单中的 HH:mm 时间。 */
    static LocalTime parseTime(String value) {
        return LocalTime.parse(value == null ? "" : value.trim(), TIME_FORMAT);
    }

    /** 显示网络或业务错误。 */
    static void showError(Throwable throwable) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("操作失败");
        alert.setHeaderText(null);
        String message = throwable == null ? null : throwable.getMessage();
        alert.setContentText(message == null || message.isBlank() ? "未知错误" : message);
        alert.showAndWait();
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
