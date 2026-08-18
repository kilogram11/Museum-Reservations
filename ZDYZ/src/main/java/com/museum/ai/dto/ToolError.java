package com.museum.ai.dto;

/**
 * AI Tool 语义错误码（与 docs/ai-agent-design.md §5 对齐）
 */
public enum ToolError {
    UNAUTHORIZED,
    BAD_REQUEST,
    IDENTITY_NOT_FOUND,
    BLACKLISTED,
    DUPLICATE_BOOKING,
    DUPLICATE_IN_REQUEST,
    SLOT_INVALID,
    FULL,
    TOO_MANY,
    NO_VISITORS,
    NOT_FOUND,
    STATUS_INVALID,
    ALREADY_CHECKED_IN,
    SCHEDULE_ERROR,
    STOCK_UNAVAILABLE,
    INTERNAL_ERROR
}
