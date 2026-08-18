package com.museum.ai.context;

/**
 * 当前登录用户上下文（ThreadLocal）。
 * 由后续拦截器 / Tool 入口 set/clear；本层不耦合 Web。
 */
public final class UserContext {

    private static final ThreadLocal<String> USER_ID = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(String userId) {
        USER_ID.set(userId);
    }

    public static String get() {
        return USER_ID.get();
    }

    public static void clear() {
        USER_ID.remove();
    }
}
