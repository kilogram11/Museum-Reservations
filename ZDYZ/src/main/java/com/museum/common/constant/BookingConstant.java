package com.museum.common.constant;

/**
 * 预约业务常量
 * 消除魔术数字，集中管理
 */
public final class BookingConstant {

    private BookingConstant() {
    }

    public static final String DEFAULT_PID = "1";

    public static final String MOCK_VERIFY_CODE = "1234";

    public static final int MAX_BOOKING_COUNT = 3;

    public static final int BOOKING_DAYS_TO_SHOW = 7;

    public static final int QR_CODE_WIDTH = 300;

    public static final int QR_CODE_HEIGHT = 300;

    public static final long LOCK_TIMEOUT_MS = 15_000L;

    public static final long LOCK_RETRY_SLEEP_MS = 100L;

    public static final int REDIS_LOCK_TTL_SECONDS = 5;

    public static final int CHECKIN_LOCK_TTL_SECONDS = 10;

    public static final String LOCK_KEY_PREFIX = "lock:booking:";

    public static final String CHECKIN_LOCK_KEY_PREFIX = "lock:checkin:";

    /** Redis 时段剩余名额：booking:stock:{timeMark} */
    public static final String STOCK_KEY_PREFIX = "booking:stock:";

    /** Redis 一证一约：booking:booked:{day}:{identityId} */
    public static final String BOOKED_KEY_PREFIX = "booking:booked:";

    /** booked key 在参观日结束后额外保留的秒数（2 小时） */
    public static final long BOOKED_TTL_BUFFER_SECONDS = 2 * 60 * 60L;

    public static final String DEFAULT_USER_NAME_PREFIX = "用户";

    public static final int MOBILE_SUFFIX_LENGTH = 4;

    public static final String JOIN_ID_PREFIX = "join_";

    public static final String EMPTY_JSON_ARRAY = "[]";

    public static String stockKey(String timeMark) {
        return STOCK_KEY_PREFIX + timeMark;
    }

    public static String bookedKey(String day, String identityId) {
        return BOOKED_KEY_PREFIX + day + ":" + identityId;
    }
}
