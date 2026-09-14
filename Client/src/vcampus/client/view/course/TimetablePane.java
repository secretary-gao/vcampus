/*
 * TimetablePane
 *
 * Version 1.1
 *
 * 2026-09-08
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.client.view.course;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import vcampus.client.biz.ICourseClientSrv;
import vcampus.common.vo.Course;
import vcampus.common.vo.CourseSchedule;
import vcampus.common.vo.TeachingClass;

import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 学生课程表页面，以动态时间段和星期组成真正的周课表。 */
public class TimetablePane extends VBox {

    private static final double TIME_COLUMN_WIDTH = 116;
    private static final double DAY_MIN_WIDTH = 112;
    private static final double GRID_BORDER_WIDTH = 2;

    private final ICourseClientSrv _client;
    private final String _studentId;
    private final VBox _scheduleHost = new VBox();
    private final ScrollPane _scheduleScroll = new ScrollPane(_scheduleHost);
    private final Label _statusLabel = new Label();
    private double _minimumGridWidth;

    /** 创建学生课程表页面。 */
    public TimetablePane(ICourseClientSrv client, String studentId) {
        this._client = client;
        this._studentId = studentId;
        getStyleClass().add("course-page");
        buildView();
        refresh();
    }

    /** 从服务器刷新当前学生课程表。 */
    public void refresh() {
        _statusLabel.setText("正在读取课程表…");
        CourseViewSupport.runAsync(this, this::loadRows, rows -> {
            renderSchedule(rows);
            _statusLabel.setText("共 " + rows.size() + " 个上课时段");
        });
    }

    private void buildView() {
        Label title = new Label("我的课表");
        title.getStyleClass().add("page-title");
        Label description = new Label("按节次与星期排列；多段周次课程会分别显示，周末有课时自动扩展");
        description.getStyleClass().add("page-description");
        VBox heading = new VBox(3, title, description);
        HBox.setHgrow(heading, Priority.ALWAYS);
        Button refreshButton = new Button("刷新课表");
        refreshButton.getStyleClass().add("secondary");
        refreshButton.setOnAction(event -> refresh());
        HBox header = new HBox(12, heading, refreshButton);
        header.setAlignment(Pos.CENTER_LEFT);

        _scheduleHost.setFillWidth(true);
        _scheduleScroll.setFitToWidth(true);
        _scheduleScroll.setPannable(true);
        _scheduleScroll.getStyleClass().add("timetable-scroll");
        _scheduleScroll.viewportBoundsProperty().addListener(
                (observable, oldBounds, newBounds) -> updateHorizontalPolicy());
        VBox.setVgrow(_scheduleScroll, Priority.ALWAYS);

        _statusLabel.getStyleClass().add("status-label");
        getChildren().addAll(header, _scheduleScroll, _statusLabel);
    }

    private List<TimetableRow> loadRows() throws Exception {
        List<CourseSchedule> schedules = _client.queryStudentSchedule(_studentId);
        Map<String, Course> courses = _client.queryCourse("").stream()
                .collect(Collectors.toMap(Course::getCourseId, Function.identity(), (a, b) -> a));
        Map<String, TeachingClass> classes = _client.queryTeachingClass("").stream()
                .collect(Collectors.toMap(TeachingClass::getTeachingClassId,
                        Function.identity(), (a, b) -> a));
        return schedules.stream()
                .map(schedule -> new TimetableRow(schedule, courses.get(schedule.getCourseId()),
                        classes.get(schedule.getTeachingClassId())))
                .sorted(Comparator.comparing((TimetableRow row) -> row.schedule().getStartTime())
                        .thenComparing(row -> row.schedule().getDayOfWeek()))
                .toList();
    }

    private void renderSchedule(List<TimetableRow> rows) {
        _scheduleHost.getChildren().clear();
        if (rows.isEmpty()) {
            _minimumGridWidth = 0;
            _scheduleHost.setMinWidth(0);
            updateHorizontalPolicy();
            VBox empty = new VBox(8);
            empty.getStyleClass().addAll("course-card", "message-card");
            Label title = new Label("本周暂无课程");
            title.getStyleClass().add("section-title");
            Label description = new Label("完成选课并存在排课后，课程会显示在这里");
            description.getStyleClass().add("empty-state-label");
            empty.getChildren().addAll(title, description);
            _scheduleHost.getChildren().add(empty);
            return;
        }

        int dayCount = Math.max(5, rows.stream()
                .mapToInt(row -> row.schedule().getDayOfWeek()).max().orElse(5));
        _minimumGridWidth = TIME_COLUMN_WIDTH + dayCount * DAY_MIN_WIDTH
                + GRID_BORDER_WIDTH;
        _scheduleHost.setMinWidth(_minimumGridWidth);
        updateHorizontalPolicy();
        List<TimeSlot> slots = rows.stream()
                .map(row -> new TimeSlot(row.schedule().getStartPeriod(),
                        row.schedule().getEndPeriod(), row.schedule().getStartTime(),
                        row.schedule().getEndTime()))
                .distinct()
                .sorted(Comparator.comparingInt(TimeSlot::startPeriod)
                        .thenComparing(TimeSlot::start).thenComparing(TimeSlot::end))
                .toList();

        GridPane grid = new GridPane();
        grid.getStyleClass().add("timetable-grid");
        grid.setMaxWidth(Double.MAX_VALUE);
        ColumnConstraints timeColumn = new ColumnConstraints(
                TIME_COLUMN_WIDTH, TIME_COLUMN_WIDTH, TIME_COLUMN_WIDTH);
        grid.getColumnConstraints().add(timeColumn);
        for (int day = 1; day <= dayCount; day++) {
            ColumnConstraints dayColumn = new ColumnConstraints(
                    DAY_MIN_WIDTH, 160, Double.MAX_VALUE);
            dayColumn.setHgrow(Priority.ALWAYS);
            dayColumn.setFillWidth(true);
            grid.getColumnConstraints().add(dayColumn);
        }

        addHeader(grid, "时间", 0);
        for (int day = 1; day <= dayCount; day++) {
            addHeader(grid, CourseViewSupport.dayName(day), day);
        }

        for (int rowIndex = 0; rowIndex < slots.size(); rowIndex++) {
            TimeSlot slot = slots.get(rowIndex);
            Label period = new Label(slot.startPeriod() + "–" + slot.endPeriod() + " 节");
            period.getStyleClass().add("timetable-period");
            Label clock = new Label(CourseViewSupport.timeRange(slot.start(), slot.end()));
            clock.getStyleClass().add("timetable-clock");
            VBox time = new VBox(3, period, clock);
            time.getStyleClass().add("timetable-time");
            time.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            grid.add(time, 0, rowIndex + 1);

            for (int day = 1; day <= dayCount; day++) {
                VBox cell = new VBox(7);
                cell.getStyleClass().add("timetable-cell");
                cell.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
                final int currentDay = day;
                rows.stream()
                        .filter(row -> row.schedule().getDayOfWeek() == currentDay
                                && row.schedule().getStartPeriod() == slot.startPeriod()
                                && row.schedule().getEndPeriod() == slot.endPeriod()
                                && row.schedule().getStartTime().equals(slot.start())
                                && row.schedule().getEndTime().equals(slot.end()))
                        .forEach(row -> cell.getChildren().add(courseCard(row)));
                grid.add(cell, day, rowIndex + 1);
            }
        }
        _scheduleHost.getChildren().add(grid);
    }

    private void updateHorizontalPolicy() {
        double viewportWidth = _scheduleScroll.getViewportBounds().getWidth();
        boolean needsHorizontalScroll = viewportWidth > 0
                && viewportWidth + 0.5 < _minimumGridWidth;
        _scheduleScroll.setHbarPolicy(needsHorizontalScroll
                ? ScrollPane.ScrollBarPolicy.AS_NEEDED
                : ScrollPane.ScrollBarPolicy.NEVER);
    }

    private void addHeader(GridPane grid, String text, int column) {
        Label header = new Label(text);
        header.getStyleClass().add("timetable-header");
        header.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        grid.add(header, column, 0);
    }

    private VBox courseCard(TimetableRow row) {
        Course course = row.course();
        String courseName = course == null ? row.schedule().getCourseId() : course.getCourseName();
        String teacher = row.teachingClass() == null ? "教师待定" : CourseViewSupport.safe(
                row.teachingClass().getTeacher(), "教师待定");
        Label name = new Label(courseName);
        name.setWrapText(true);
        name.getStyleClass().add("timetable-course-name");
        Label teacherLabel = new Label(teacher);
        teacherLabel.getStyleClass().add("timetable-course-meta");
        Label weeks = new Label(row.schedule().getWeekStart() + "–"
                + row.schedule().getWeekEnd() + " 周 · "
                + (row.teachingClass() == null ? "—"
                : row.teachingClass().getClassNumber() + " 班"));
        weeks.setWrapText(true);
        weeks.getStyleClass().add("timetable-course-meta");
        Label room = new Label(CourseViewSupport.safe(
                row.schedule().getClassroom(), "教室待定"));
        room.getStyleClass().add("timetable-course-meta");
        VBox card = new VBox(3, name, weeks, teacherLabel, room);
        int variant = Math.floorMod(row.schedule().getCourseId().hashCode(), 4);
        card.getStyleClass().addAll("timetable-course", "variant-" + variant);
        return card;
    }

    private record TimeSlot(int startPeriod, int endPeriod,
                            LocalTime start, LocalTime end) {
    }

    /** 课程表展示行，将排课与课程主数据在客户端只读组合。 */
    private record TimetableRow(CourseSchedule schedule, Course course,
                                TeachingClass teachingClass) {
    }
}
