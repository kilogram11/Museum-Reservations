package com.museum.common.utils;
// # ID生成器（生成ADMIN_ID、USER_ID等业务主键）


import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;

/**
 * ID生成器
 * 生成各种业务主键：adminId, userId, joinId等
 */
public class IdGenerator {

    private static final Snowflake snowflake = IdUtil.getSnowflake(1, 1);

    /**
     * 生成雪花ID（纯数字）
     */
    public static String generateId() {
        return String.valueOf(snowflake.nextId());
    }

    /**
     * 生成带前缀的ID
     */
    public static String generateId(String prefix) {
        return prefix + "_" + snowflake.nextId();
    }

    // 具体业务ID生成方法

    public static String generateAdminId() {
        return generateId("admin");
    }

    public static String generateUserId() {
        return generateId("user");
    }

    public static String generateIdentityId() {
        return generateId("identity");
    }

    public static String generateJoinId() {
        return generateId("join");
    }

    public static String generateMuseumId() {
        return generateId("museum");
    }

    public static String generateActivityId() {
        return generateId("activity");
    }

    public static String generateNewsId() {
        return generateId("news");
    }

    public static String generateDayId() {
        return generateId("day");
    }

    public static String generateTimeId() {
        return generateId("time");
    }

    public static String generateLogId() {
        return generateId("log");
    }
}