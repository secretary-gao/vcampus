/*
 * CourseServiceException
 *
 * Version 1.0
 *
 * 2026-09-04
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.server.srv;

/**
 * 选课业务异常，用于报告参数无效、课程不存在、重复选课或课程已满等
 * 可预期的业务失败。
 */
public class CourseServiceException extends Exception {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /**
     * 使用可展示的业务提示创建异常。
     *
     * @param message 业务失败原因
     */
    public CourseServiceException(String message) {
        super(message);
    }
}
