package com.museum.common.exception;
//自定义业务异常（如"用户名不存在"）


import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 自定义业务异常
 * 用于抛出业务相关的错误
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BusinessException extends RuntimeException {

    private Integer code;

    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    // 快捷方法
    public static BusinessException of(String message) {
        return new BusinessException(message);
    }

    public static BusinessException of(Integer code, String message) {
        return new BusinessException(code, message);
    }
}