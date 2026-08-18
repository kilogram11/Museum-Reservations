package com.museum.ai.rag.model;

/**
 * 入库前的完整文档（公告一条或静态须知一节）。
 */
public record RagDocument(
        String id,
        String sourceType,
        String sourceId,
        String title,
        String body,
        Long publishedAt
) {
}
