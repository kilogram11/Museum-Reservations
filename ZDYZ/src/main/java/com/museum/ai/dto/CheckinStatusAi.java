package com.museum.ai.dto;

/**
 * AI 层核销状态（避免与业务 {@code CheckinStatus} 枚举重名）
 */
public enum CheckinStatusAi {
    UNCHECKED,
    CHECKED_IN,
    EXPIRED
}
