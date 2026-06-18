package com.museum.common.utils;

/**
 * 分页参数默认值工具。
 */
public final class PageParamUtil {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_LIMIT = 10;

    private PageParamUtil() {
    }

    public static int defaultPage(Object pageValue) {
        return positiveIntOrDefault(pageValue, DEFAULT_PAGE);
    }

    public static int defaultLimit(Object limitValue) {
        return positiveIntOrDefault(limitValue, DEFAULT_LIMIT);
    }

    private static int positiveIntOrDefault(Object value, int defaultValue) {
        if (value instanceof Number number) {
            int intValue = number.intValue();
            return intValue > 0 ? intValue : defaultValue;
        }
        if (value instanceof String text) {
            try {
                int intValue = Integer.parseInt(text);
                return intValue > 0 ? intValue : defaultValue;
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }
}
