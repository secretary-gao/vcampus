package vcampus.client.view;

import vcampus.common.vo.Student;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Function;

/** UTF-8 BOM CSV writer for Excel-compatible student record exports. */
final class StudentCsvExporter {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final ExportField STUDENT_ID = new ExportField(
            "studentId", "学号", Student::getStudentId);
    private static final ExportField CAMPUS_CARD_NO = new ExportField(
            "campusCardNo", "一卡通号", Student::getCampusCardNo);
    private static final ExportField USER_ID = new ExportField(
            "userId", "用户账号", Student::getUserId);
    private static final ExportField NAME = new ExportField(
            "name", "姓名", Student::getName);
    private static final ExportField CLASS_NAME = new ExportField(
            "className", "班级", Student::getClassName);
    private static final ExportField MAJOR = new ExportField(
            "major", "专业", Student::getMajor);
    private static final ExportField GRADE = new ExportField(
            "grade", "年级", Student::getGrade);
    private static final ExportField ENROLLMENT_DATE = new ExportField(
            "enrollmentDate", "入学日期", StudentCsvExporter::formatDate);
    private static final ExportField STATUS = new ExportField(
            "status", "状态", StudentCsvExporter::formatStatus);
    private static final List<ExportField> ADMIN_FIELDS = List.of(
            STUDENT_ID, CAMPUS_CARD_NO, USER_ID, NAME, CLASS_NAME, MAJOR,
            GRADE, ENROLLMENT_DATE, STATUS);
    private static final List<ExportField> TEACHER_FIELDS = List.of(
            STUDENT_ID, CAMPUS_CARD_NO, NAME, CLASS_NAME, MAJOR, GRADE, STATUS);

    private StudentCsvExporter() {
    }

    static void write(Path path, List<Student> students, boolean includePrivateFields)
            throws IOException {
        List<ExportField> fields = fields(includePrivateFields);
        StringBuilder csv = new StringBuilder("\uFEFF");
        appendHeader(csv, fields);
        for (Student student : students) {
            appendStudent(csv, fields, student);
        }
        Files.writeString(path, csv, StandardCharsets.UTF_8);
    }

    static List<ExportField> fields(boolean includePrivateFields) {
        return includePrivateFields ? ADMIN_FIELDS : TEACHER_FIELDS;
    }

    private static String formatDate(Student student) {
        return student.getEnrollmentDate() == null ? "" : DATE.format(student.getEnrollmentDate());
    }

    private static String formatStatus(Student student) {
        return student.getStatus() == null ? "" : student.getStatus().toString();
    }

    private static void appendHeader(StringBuilder target, List<ExportField> fields) {
        for (int index = 0; index < fields.size(); index++) {
            if (index > 0) {
                target.append(',');
            }
            target.append(fields.get(index).getCsvHeader());
        }
        target.append("\r\n");
    }

    private static void appendStudent(StringBuilder target, List<ExportField> fields,
                                      Student student) {
        for (int index = 0; index < fields.size(); index++) {
            if (index > 0) {
                target.append(',');
            }
            target.append(escape(fields.get(index).valueOf(student)));
        }
        target.append("\r\n");
    }

    private static String escape(String value) {
        return '"' + value.replace("\"", "\"\"") + '"';
    }

    /** One field shared by the CSV writer and its on-screen export preview. */
    static final class ExportField {

        private final String _csvHeader;
        private final String _displayName;
        private final Function<Student, String> _valueFactory;

        private ExportField(String csvHeader, String displayName,
                            Function<Student, String> valueFactory) {
            _csvHeader = csvHeader;
            _displayName = displayName;
            _valueFactory = valueFactory;
        }

        String getCsvHeader() {
            return _csvHeader;
        }

        String getDisplayName() {
            return _displayName;
        }

        String valueOf(Student student) {
            String value = _valueFactory.apply(student);
            return value == null ? "" : value;
        }
    }
}
