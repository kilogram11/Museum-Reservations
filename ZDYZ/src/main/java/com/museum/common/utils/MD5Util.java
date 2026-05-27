package com.museum.common.utils;
//# MD5加密工具（密码加密）


import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * MD5加密工具
 * 用于管理员密码加密
 */
public class MD5Util {

    /**
     * MD5加密
     */
    public static String encrypt(String str) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(str.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(bytes);
        } catch (Exception e) {
            throw new RuntimeException("MD5加密失败", e);
        }
    }

    /**
     * 字节数组转16进制字符串
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(b & 0xFF);
            if (hex.length() < 2) {
                sb.append(0);
            }
            sb.append(hex);
        }
        return sb.toString();
    }

    /**
     * 加盐加密（更安全）
     */
    public static String encryptWithSalt(String password, String salt) {
        return encrypt(password + salt);
    }
}