package com.museum.ai.rag.model;

/**
 * 切片后的可检索片段。
 */
public record RagChunk(
        String chunkId,
        String sourceType,
        String sourceId,
        String title,
        String text,
        Long publishedAt
) {
}
