package vcampus.client.view;

import vcampus.common.vo.Student;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** UTF-8 BOM CSV writer for Excel-compatible student record exports. */
final class StudentCsvExporter {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private StudentCsvExporter() {
    }

    static void write(Path path, List<Student> students, boolean includePrivateFields)
            throws IOException {
        String header = includePrivateFields
                ? "studentId,campusCardNo,userId,name,className,major,grade,enrollmentDate,status\r\n"
                : "studentId,campusCardNo,name,className,major,grade,status\r\n";
        StringBuilder csv = new StringBuilder("\uFEFF").append(header);
        for (Student student : students) {
            if (includePrivateFields) {
                line(csv, student.getStudentId(), student.getCampusCardNo(), student.getUserId(),
                        student.getName(), student.getClassName(), student.getMajor(),
                        student.getGrade(), formatDate(student), formatStatus(student));
            } else {
                line(csv, student.getStudentId(), student.getCampusCardNo(), student.getName(),
                        student.getClassName(), student.getMajor(), student.getGrade(),
                        formatStatus(student));
            }
        }
        Files.writeString(path, csv, StandardCharsets.UTF_8);
    }

    private static String formatDate(Student student) {
        return student.getEnrollmentDate() == null ? "" : DATE.format(student.getEnrollmentDate());
    }

    private static String formatStatus(Student student) {
        return student.getStatus() == null ? "" : student.getStatus().toString();
    }

    private static void line(StringBuilder target, Object... values) {
        for (int index = 0; index < values.length; index++) {
            if (index > 0) {
                target.append(',');
            }
            target.append(escape(values[index] == null ? "" : String.valueOf(values[index])));
        }
        target.append("\r\n");
    }

    private static String escape(String value) {
        return '"' + value.replace("\"", "\"\"") + '"';
    }
}
