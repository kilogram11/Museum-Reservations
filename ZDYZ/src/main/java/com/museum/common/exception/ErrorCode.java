package com.museum.common.exception;

import lombok.Getter;

/**
 * 统一错误码枚举
 * 替换散布在各处的魔术数字状态码
 */
@Getter
public enum ErrorCode {

    SUCCESS(200, "操作成功"),

    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或Token无效"),
    FORBIDDEN(403, "无权限访问"),

    USER_NOT_FOUND(1001, "用户不存在"),
    USER_MOBILE_EMPTY(1002, "手机号不能为空"),
    VERIFY_CODE_ERROR(1003, "验证码错误"),

    IDENTITY_NOT_FOUND(2001, "游客信息不存在"),
    IDENTITY_BLACKLISTED(2002, "游客在黑名单中，无法预约"),
    IDENTITY_DUPLICATE_BOOKING(2003, "今日已预约，请勿重复提交"),

    BOOKING_SLOT_INVALID(3001, "时段无效或未开放"),
    BOOKING_SLOT_FULL(3002, "该时段余量不足"),
    BOOKING_TOO_MANY(3003, "单次最多预约3人"),
    BOOKING_NO_VISITORS(3004, "请选择参观人"),
    BOOKING_NOT_FOUND(3005, "预约记录不存在"),
    BOOKING_STATUS_INVALID(3006, "当前状态不可取消"),
    BOOKING_ALREADY_CHECKED_IN(3007, "已核销或已失效，无法取消"),
    BOOKING_ALREADY_DONE(3008, "该记录已核销，请勿重复操作"),
    BOOKING_EXPIRED(3009, "该记录已失效/爽约，无法核销"),
    BOOKING_LOCK_FAILED(3010, "预约人数较多，请稍后刷新重试"),
    BOOKING_SCHEDULE_ERROR(3011, "排期日期数据异常"),

    CHECKIN_BUSY(4001, "正在核销中，请勿重复扫描"),

    INTERNAL_ERROR(500, "服务器内部错误");

    private final Integer code;
    private final String message;

    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
