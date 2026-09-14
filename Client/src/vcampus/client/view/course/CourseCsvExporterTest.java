package vcampus.client.view.course;

import vcampus.common.vo.TeacherCourseEnrollment;
import vcampus.common.vo.TeachingClassStatistic;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

/** Verifies CSV schema, row count, BOM, and Chinese content. */
public class CourseCsvExporterTest {
    public static void main(String[] args) throws Exception {
        Path roster = Files.createTempFile("vcampus-course-roster-", ".csv");
        Path stats = Files.createTempFile("vcampus-course-stats-", ".csv");
        try {
            TeacherCourseEnrollment row = new TeacherCourseEnrollment();
            row.setStudentId("2024000001");
            row.setStudentName("测试学生");
            row.setClassName("计科2401");
            row.setMajor("计算机科学与技术");
            row.setSelectTime(LocalDateTime.of(2026, 9, 13, 10, 0));
            CourseCsvExporter.writeRoster(roster, List.of(row));
            CourseCsvExporter.writeClassStatistics(stats, List.of(
                    new TeachingClassStatistic("TC-01", "数据库原理", "测试教师", 60, 25)));
            verify(roster, "studentId", "测试学生", 2);
            verify(stats, "teachingClassId", "数据库原理", 2);
            System.out.println("COURSE_CSV_EXPORTER_TEST=PASS");
        } finally {
            Files.deleteIfExists(roster);
            Files.deleteIfExists(stats);
            System.out.println("COURSE_CSV_EXPORTER_TEST_RESIDUE=0");
        }
    }

    private static void verify(Path path, String header, String chinese, int lines)
            throws Exception {
        byte[] bytes = Files.readAllBytes(path);
        require(bytes.length >= 3 && (bytes[0] & 0xff) == 0xef
                && (bytes[1] & 0xff) == 0xbb && (bytes[2] & 0xff) == 0xbf,
                "UTF-8 BOM 存在");
        String content = Files.readString(path, StandardCharsets.UTF_8);
        require(content.contains(header) && content.contains(chinese), "字段与中文正确");
        require(content.lines().count() == lines, "CSV 行数正确");
    }

    private static void require(boolean value, String message) {
        if (!value) throw new IllegalStateException("FAIL: " + message);
        System.out.println("PASS: " + message);
    }
}
