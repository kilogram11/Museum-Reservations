package com.museum.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI Tool 统一返回壳
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ToolResult<T> {

    private boolean ok;
    private ToolError error;
    private String message;
    private T data;

    public static <T> ToolResult<T> ok(T data) {
        return new ToolResult<>(true, null, null, data);
    }

    public static <T> ToolResult<T> ok(String message, T data) {
        return new ToolResult<>(true, null, message, data);
    }

    public static <T> ToolResult<T> fail(ToolError error, String message) {
        return new ToolResult<>(false, error, message, null);
    }
}
