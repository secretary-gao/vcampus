package vcampus.client.view.course;

import vcampus.common.vo.TeacherCourseEnrollment;
import vcampus.common.vo.TeachingClassStatistic;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** UTF-8 BOM CSV writer for Windows Excel-compatible Course exports. */
final class CourseCsvExporter {
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private CourseCsvExporter() { }

    static void writeRoster(Path path, List<TeacherCourseEnrollment> rows) throws IOException {
        StringBuilder csv = new StringBuilder("\uFEFFstudentId,studentName,className,major,selectTime\r\n");
        for (TeacherCourseEnrollment row : rows) {
            line(csv, row.getStudentId(), row.getStudentName(), row.getClassName(),
                    row.getMajor(), row.getSelectTime() == null ? ""
                            : DATE_TIME.format(row.getSelectTime()));
        }
        Files.writeString(path, csv, StandardCharsets.UTF_8);
    }

    static void writeClassStatistics(Path path, List<TeachingClassStatistic> rows)
            throws IOException {
        StringBuilder csv = new StringBuilder(
                "\uFEFFteachingClassId,courseName,teacher,capacity,selectedCount,remaining,utilization\r\n");
        for (TeachingClassStatistic row : rows) {
            line(csv, row.teachingClassId(), row.courseName(), row.teacher(),
                    row.capacity(), row.selectedCount(), row.remaining(),
                    String.format(java.util.Locale.ROOT, "%.1f%%", row.utilization() * 100));
        }
        Files.writeString(path, csv, StandardCharsets.UTF_8);
    }

    private static void line(StringBuilder target, Object... values) {
        for (int index = 0; index < values.length; index++) {
            if (index > 0) target.append(',');
            target.append(escape(String.valueOf(values[index] == null ? "" : values[index])));
        }
        target.append("\r\n");
    }

    private static String escape(String value) {
        return '"' + value.replace("\"", "\"\"") + '"';
    }
}
