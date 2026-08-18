package com.museum.ai.rag.support;

import com.museum.ai.rag.model.RagChunk;
import com.museum.ai.rag.model.RagDocument;

import java.util.ArrayList;
import java.util.List;

/**
 * 按段落或定长窗口切分文档。
 */
public final class Chunker {

    private static final int MAX_CHARS = 420;

    private Chunker() {
    }

    public static List<RagChunk> chunk(RagDocument doc) {
        List<RagChunk> out = new ArrayList<>();
        if (doc == null || doc.body() == null || doc.body().isBlank()) {
            return out;
        }
        String[] paragraphs = doc.body().split("\\n{2,}|\\r\\n{2,}");
        int idx = 0;
        for (String p : paragraphs) {
            String text = p.replaceAll("\\s+", " ").trim();
            if (text.isEmpty()) {
                continue;
            }
            if (text.length() <= MAX_CHARS) {
                out.add(toChunk(doc, idx++, text));
            } else {
                for (int start = 0; start < text.length(); start += MAX_CHARS) {
                    int end = Math.min(text.length(), start + MAX_CHARS);
                    out.add(toChunk(doc, idx++, text.substring(start, end)));
                }
            }
        }
        if (out.isEmpty()) {
            out.add(toChunk(doc, 0, doc.body().trim()));
        }
        return out;
    }

    private static RagChunk toChunk(RagDocument doc, int idx, String text) {
        return new RagChunk(
                doc.id() + "#" + idx,
                doc.sourceType(),
                doc.sourceId(),
                doc.title(),
                text,
                doc.publishedAt()
        );
    }
}
