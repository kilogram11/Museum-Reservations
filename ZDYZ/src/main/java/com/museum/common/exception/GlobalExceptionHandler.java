package com.museum.common.exception;
//# 错误码枚举（如 401未登录、403无权限、

import com.museum.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 全局异常处理器
 * 统一捕获并处理所有异常
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public Result handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.error("业务异常 - URI: {}, 错误: {}", request.getRequestURI(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 参数校验异常 (@Valid)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result handleValidException(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : "参数校验失败";
        log.error("参数校验异常: {}", message);
        return Result.error(400, message);
    }

    /**
     * 参数绑定异常
     */
    @ExceptionHandler(BindException.class)
    public Result handleBindException(BindException e) {
        FieldError fieldError = e.getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : "参数绑定失败";
        log.error("参数绑定异常: {}", message);
        return Result.error(400, message);
    }

    /**
     * 空指针异常
     */
    @ExceptionHandler(NullPointerException.class)
    public Result handleNullPointerException(NullPointerException e, HttpServletRequest request) {
        log.error("空指针异常 - URI: {}", request.getRequestURI(), e);
        return Result.error("系统异常，请联系管理员");
    }

    /**
     * JSON 解析异常 (如参数类型不匹配、格式错误)
     */
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public Result handleMessageNotReadableException(
            org.springframework.http.converter.HttpMessageNotReadableException e) {
        log.error("JSON解析失败: {}", e.getMessage());
        return Result.error(400, "请求体格式错误: " + e.getMessage());
    }

    /**
     * 其他未知异常
     */
    @ExceptionHandler(Exception.class)
    public Result handleException(Exception e, HttpServletRequest request) {
        log.error("系统异常 - URI: {}", request.getRequestURI(), e);
        return Result.error("系统异常，请联系管理员");
    }
}