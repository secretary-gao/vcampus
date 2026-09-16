package vcampus.client.view;

import vcampus.common.vo.Student;
import vcampus.common.vo.StudentStatus;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/** 解析管理员导出的完整学籍 CSV，不读取或写入密码。 */
final class StudentCsvImporter {

    static final String HEADER = "studentId,campusCardNo,userId,name,className,major,grade,enrollmentDate,status";

    private StudentCsvImporter() {
    }

    static ImportResult read(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        List<Row> rows = new ArrayList<>();
        if (lines.isEmpty()) {
            return new ImportResult(rows, "CSV 文件为空");
        }
        String header = stripBom(lines.get(0));
        if (!HEADER.equals(header)) {
            return new ImportResult(rows, "表头不正确，请使用管理员导出的完整学籍 CSV");
        }
        for (int index = 1; index < lines.size(); index++) {
            int lineNumber = index + 1;
            if (lines.get(index).trim().isEmpty()) {
                continue;
            }
            try {
                List<String> values = parseLine(lines.get(index));
                if (values.size() != 9) {
                    throw new IllegalArgumentException("应有 9 个字段，实际为 " + values.size() + " 个");
                }
                rows.add(new Row(lineNumber, toStudent(values), null));
            } catch (RuntimeException exception) {
                rows.add(new Row(lineNumber, null, exception.getMessage()));
            }
        }
        return new ImportResult(rows, null);
    }

    private static Student toStudent(List<String> values) {
        Student student = new Student();
        student.setStudentId(required(values.get(0), "学号"));
        student.setCampusCardNo(required(values.get(1), "一卡通号"));
        student.setUserId(required(values.get(2), "用户账号"));
        student.setName(required(values.get(3), "姓名"));
        student.setClassName(required(values.get(4), "班级"));
        student.setMajor(required(values.get(5), "专业"));
        student.setGrade(required(values.get(6), "年级"));
        String date = values.get(7).trim();
        if (date.isEmpty()) {
            throw new IllegalArgumentException("入学日期不能为空");
        }
        try {
            student.setEnrollmentDate(LocalDate.parse(date));
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("入学日期格式错误，应为 yyyy-MM-dd");
        }
        String status = values.get(8).trim();
        if (status.isEmpty()) {
            throw new IllegalArgumentException("学籍状态不能为空");
        } else {
            try {
                student.setStatus(StudentStatus.fromDatabaseValue(status));
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("学籍状态无效：" + status);
            }
        }
        return student;
    }

    private static String required(String value, String label) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(label + "不能为空");
        }
        return normalized;
    }

    private static List<String> parseLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder value = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < line.length(); index++) {
            char current = line.charAt(index);
            if (current == '"') {
                if (quoted && index + 1 < line.length() && line.charAt(index + 1) == '"') {
                    value.append('"');
                    index++;
                } else {
                    quoted = !quoted;
                }
            } else if (current == ',' && !quoted) {
                values.add(value.toString());
                value.setLength(0);
            } else {
                value.append(current);
            }
        }
        if (quoted) {
            throw new IllegalArgumentException("引号未闭合");
        }
        values.add(value.toString());
        return values;
    }

    private static String stripBom(String value) {
        return value != null && !value.isEmpty() && value.charAt(0) == '\uFEFF'
                ? value.substring(1) : value;
    }

    static final class ImportResult {
        private final List<Row> _rows;
        private final String _fileError;

        ImportResult(List<Row> rows, String fileError) {
            _rows = rows;
            _fileError = fileError;
        }

        List<Row> rows() {
            return _rows;
        }

        String fileError() {
            return _fileError;
        }
    }

    static final class Row {
        private final int _lineNumber;
        private final Student _student;
        private final String _error;

        Row(int lineNumber, Student student, String error) {
            _lineNumber = lineNumber;
            _student = student;
            _error = error;
        }

        int lineNumber() {
            return _lineNumber;
        }

        Student student() {
            return _student;
        }

        String error() {
            return _error;
        }
    }
}
