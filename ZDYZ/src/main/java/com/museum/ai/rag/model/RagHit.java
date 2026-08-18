package com.museum.ai.rag.model;

/**
 * 检索命中。
 */
public record RagHit(
        RagChunk chunk,
        float score
) {
}
