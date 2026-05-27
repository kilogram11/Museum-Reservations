package com.museum.common.enums;

import lombok.Getter;

/**
 * 核销状态枚举
 * 替换魔术数字 0、1、3
 */
@Getter
public enum CheckinStatus {

    UNCHECKED(0, "未核销"),
    CHECKED_IN(1, "已核销"),
    EXPIRED(3, "逾期未核销（爽约）");

    private final Integer code;
    private final String desc;

    CheckinStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static CheckinStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (CheckinStatus s : values()) {
            if (s.code.equals(code)) {
                return s;
            }
        }
        return null;
    }
}
