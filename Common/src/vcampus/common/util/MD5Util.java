/*
 * MD5Util
 *
 * Version 1.0
 *
 * 2026-08-28
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 密码加密工具类，提供 MD5 摘要算法。注册时将密码明文转换为 32 位十六进制
 * 摘要后存入数据库，避免明文存储密码。当前阶段仅用于课程项目，不需要更
 * 复杂的安全设计。
 */
public final class MD5Util {

    /**
     * 私有构造方法，禁止实例化工具类。
     */
    private MD5Util() {
    }

    /**
     * 计算字符串的 MD5 摘要（32 位小写十六进制）。
     *
     * @param input 原始字符串，可为 {@code null}
     * @return 输入字符串的 MD5 摘要；若输入为 {@code null}，返回 {@code null}
     */
    public static String md5(String input) {
        if (input == null) {
            return null;
        }

        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("当前环境不支持 MD5 算法", e);
        }
    }
}
