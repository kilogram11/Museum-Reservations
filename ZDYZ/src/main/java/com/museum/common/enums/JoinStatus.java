package com.museum.common.enums;

import lombok.Getter;

/**
 * 预约状态枚举
 * 替换魔术数字 1、2
 */
@Getter
public enum JoinStatus {

    SUCCESS(1, "预约成功"),
    CANCELLED(2, "已取消");

    private final Integer code;
    private final String desc;

    JoinStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static JoinStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (JoinStatus s : values()) {
            if (s.code.equals(code)) {
                return s;
            }
        }
        return null;
    }
}
