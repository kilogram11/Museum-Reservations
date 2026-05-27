package com.museum.common.result;

import com.museum.common.exception.ErrorCode;
import lombok.Data;

/**
 * 统一响应对象
 * 所有接口都返回此格式：{code, msg, data}
 */
@Data
public class Result {
    private Integer code;
    private String msg;
    private Object data;

    private Result(Integer code, String msg, Object data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public static Result success(String msg, Object data) {
        return new Result(ErrorCode.SUCCESS.getCode(), msg, data);
    }

    public static Result success(String msg) {
        return new Result(ErrorCode.SUCCESS.getCode(), msg, null);
    }

    public static Result success(Object data) {
        return new Result(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), data);
    }

    public static Result error(String msg) {
        return new Result(ErrorCode.INTERNAL_ERROR.getCode(), msg, null);
    }

    public static Result error(Integer code, String msg) {
        return new Result(code, msg, null);
    }

    public static Result error(ErrorCode errorCode) {
        return new Result(errorCode.getCode(), errorCode.getMessage(), null);
    }

    public static Result error(ErrorCode errorCode, String detail) {
        return new Result(errorCode.getCode(), detail, null);
    }

    public boolean isSuccess() {
        return this.code != null && this.code.equals(ErrorCode.SUCCESS.getCode());
    }
}
