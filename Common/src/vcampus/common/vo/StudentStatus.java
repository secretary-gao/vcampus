package vcampus.common.vo;

/** 学籍状态。枚举名供 Java 使用，中文值与数据库 ENUM 保持一致。 */
public enum StudentStatus {

    ENROLLED("在读"),
    SUSPENDED("休学"),
    GRADUATED("毕业"),
    WITHDRAWN("退学");

    private final String _databaseValue;

    StudentStatus(String databaseValue) {
        this._databaseValue = databaseValue;
    }

    public String getDatabaseValue() {
        return _databaseValue;
    }

    public static StudentStatus fromDatabaseValue(String value) {
        for (StudentStatus status : values()) {
            if (status._databaseValue.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的学籍状态：" + value);
    }

    @Override
    public String toString() {
        return _databaseValue;
    }
}
