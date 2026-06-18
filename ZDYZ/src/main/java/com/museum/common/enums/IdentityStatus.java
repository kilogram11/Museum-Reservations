package com.museum.common.enums;

import lombok.Getter;

/**
 * 身份用户状态枚举
 * 替换魔术数字 0、1
 */
@Getter
public enum IdentityStatus {

    BLACKLISTED(0, "黑名单"),
    NORMAL(1, "正常");

    private final Integer code;
    private final String desc;

    IdentityStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
