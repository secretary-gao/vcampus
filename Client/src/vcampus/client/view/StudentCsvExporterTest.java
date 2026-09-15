package vcampus.client.view;

import vcampus.common.vo.Student;
import vcampus.common.vo.StudentStatus;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

/** Verifies student CSV headers, BOM, escaping, and teacher field restrictions. */
public class StudentCsvExporterTest {

    public static void main(String[] args) throws Exception {
        Path adminFile = Files.createTempFile("vcampus-students-admin-", ".csv");
        Path teacherFile = Files.createTempFile("vcampus-students-teacher-", ".csv");
        try {
            Student student = new Student();
            student.setStudentId("2024000001");
            student.setCampusCardNo("CARD,001");
            student.setUserId("C2400001");
            student.setName("测试学生");
            student.setClassName("计科2401");
            student.setMajor("计算机科学与技术");
            student.setGrade("2024");
            student.setEnrollmentDate(LocalDate.of(2024, 9, 1));
            student.setStatus(StudentStatus.ENROLLED);

            StudentCsvExporter.write(adminFile, List.of(student), true);
            StudentCsvExporter.write(teacherFile, List.of(student), false);
            String admin = read(adminFile);
            String teacher = read(teacherFile);
            require(admin.startsWith("\uFEFFstudentId,campusCardNo,userId"), "管理员表头完整");
            require(admin.contains("\"CARD,001\"") && admin.contains("2024-09-01"),
                    "管理员字段正确转义");
            require(teacher.startsWith("\uFEFFstudentId,campusCardNo,name"), "教师表头包含学号和一卡通号");
            require(teacher.contains("CARD,001") && !teacher.contains("C2400001"),
                    "教师导出包含学号/一卡通号但不包含账号");
            System.out.println("STUDENT_CSV_EXPORTER_TEST=PASS");
        } finally {
            Files.deleteIfExists(adminFile);
            Files.deleteIfExists(teacherFile);
        }
    }

    private static String read(Path path) throws Exception {
        byte[] bytes = Files.readAllBytes(path);
        require(bytes.length >= 3 && (bytes[0] & 0xff) == 0xef
                && (bytes[1] & 0xff) == 0xbb && (bytes[2] & 0xff) == 0xbf,
                "UTF-8 BOM 存在");
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException("FAIL: " + message);
        }
    }
}
