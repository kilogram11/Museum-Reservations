package com.museum.common.result;
// # 统一返回结果封装

//# 所有接口都返回这个格式：{code, message, data}

import lombok.Data;

/**
 * 架构书规范：统一响应对象
 * 所有的后端接口都必须返回这个对象给前端
 */
@Data
public class Result {
    private Integer code; // 状态码 (如 200 成功, 500 失败)
    private String msg; // 提示信息
    private Object data; // 真正的数据内容

    // 构造方法（私有，通过静态方法调用）
    private Result(Integer code, String msg, Object data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    /**
     * 成功返回 - 带数据
     */
    public static Result success(String msg, Object data) {
        return new Result(200, msg, data);
    }

    /**
     * 成功返回 - 仅提示
     */
    public static Result success(String msg) {
        return new Result(200, msg, null);
    }

    /**
     * 错误返回 - 仅提示（默认500）
     */
    public static Result error(String msg) {
        return new Result(500, msg, null);
    }

    /**
     * 错误返回 - 自定义状态码
     */
    public static Result error(Integer code, String msg) {
        return new Result(code, msg, null);
    }
}
