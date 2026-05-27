package com.museum.common.utils;
// # 时间处理工具（时间戳转日期、计算天数差等）

import java.text.SimpleDateFormat;
import java.time.LocalDate;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * 日期时间工具类
 */
public class DateUtil {

    public static final String PATTERN_DATE = "yyyy-MM-dd";
    public static final String PATTERN_TIME = "HH:mm";
    public static final String PATTERN_DATETIME = "yyyy-MM-dd HH:mm:ss";

    /**
     * 获取当前时间戳（毫秒）
     */
    public static Long getCurrentTimestamp() {
        return System.currentTimeMillis();
    }

    /**
     * 时间戳转日期字符串
     */
    public static String timestampToDateStr(Long timestamp) {
        if (timestamp == null)
            return null;
        Date date = new Date(timestamp);
        SimpleDateFormat sdf = new SimpleDateFormat(PATTERN_DATE);
        return sdf.format(date);
    }

    /**
     * 时间戳转日期时间字符串
     */
    public static String timestampToDateTimeStr(Long timestamp) {
        if (timestamp == null)
            return null;
        Date date = new Date(timestamp);
        SimpleDateFormat sdf = new SimpleDateFormat(PATTERN_DATETIME);
        return sdf.format(date);
    }

    /**
     * 日期字符串转时间戳
     */
    public static Long dateStrToTimestamp(String dateStr) {
        try {
            LocalDate localDate = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern(PATTERN_DATE));
            return localDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取今天的日期字符串
     */
    public static String getTodayStr() {
        return LocalDate.now().format(DateTimeFormatter.ofPattern(PATTERN_DATE));
    }

    /**
     * 获取N天后的日期字符串
     */
    public static String getDateAfterDays(int days) {
        return LocalDate.now().plusDays(days).format(DateTimeFormatter.ofPattern(PATTERN_DATE));
    }

    /**
     * 计算两个日期相差天数
     */
    public static Long getDaysBetween(String startDate, String endDate) {
        try {
            LocalDate start = LocalDate.parse(startDate, DateTimeFormatter.ofPattern(PATTERN_DATE));
            LocalDate end = LocalDate.parse(endDate, DateTimeFormatter.ofPattern(PATTERN_DATE));
            return ChronoUnit.DAYS.between(start, end);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 判断日期是否是今天
     */
    public static boolean isToday(String dateStr) {
        return getTodayStr().equals(dateStr);
    }

    /**
     * 判断时间戳是否是今天
     */
    public static boolean isToday(Long timestamp) {
        if (timestamp == null)
            return false;
        String dateStr = timestampToDateStr(timestamp);
        return isToday(dateStr);
    }

    /**
     * 判断时间戳是否在过去7天内
     */
    public static boolean isInLastSevenDays(Long timestamp) {
        if (timestamp == null)
            return false;
        long sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L);
        return timestamp >= sevenDaysAgo;
    }
}