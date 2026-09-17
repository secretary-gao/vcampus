package vcampus.client.view;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** 验证管理员学籍 CSV 导入的转义、表头和逐行错误处理。 */
public class StudentCsvImporterTest {

    public static void main(String[] args) throws Exception {
        Path file = Files.createTempFile("vcampus-students-import-", ".csv");
        try {
            String content = "\uFEFF" + StudentCsvImporter.HEADER + "\r\n"
                    + "\"2024000001\",\"CARD,001\",\"C2400001\",\"测试学生\",\"计科2401\",\"计算机科学与技术\",\"2024\",\"2024-09-01\",\"在读\"\r\n"
                    + "\"2024000002\",\"CARD-002\",\"C2400002\",\"\",\"计科2401\",\"计算机科学与技术\",\"2024\",\"\",\"在读\"\r\n";
            Files.writeString(file, content, StandardCharsets.UTF_8);
            StudentCsvImporter.ImportResult result = StudentCsvImporter.read(file);
            require(result.fileError() == null, "表头解析失败");
            require(result.rows().size() == 2, "行数解析失败");
            require(result.rows().get(0).student() != null
                    && "CARD,001".equals(result.rows().get(0).student().getCampusCardNo()),
                    "带逗号字段解析失败");
            require(result.rows().get(1).student() == null
                    && result.rows().get(1).error().contains("姓名不能为空"),
                    "错误行未被保留");
            System.out.println("STUDENT_CSV_IMPORTER_TEST=PASS");
        } finally {
            Files.deleteIfExists(file);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException("FAIL: " + message);
        }
    }
}
