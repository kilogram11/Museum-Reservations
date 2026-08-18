package com.museum.ai.rag.store;

import com.museum.ai.rag.model.RagChunk;
import com.museum.ai.rag.model.RagHit;
import com.museum.ai.rag.support.LocalHashingEmbeddingModel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 进程内向量库：chunk + embedding，余弦 TopK。
 */
public class InMemoryRagStore {

    private final LocalHashingEmbeddingModel embeddingModel;
    private final CopyOnWriteArrayList<Entry> entries = new CopyOnWriteArrayList<>();

    public InMemoryRagStore(LocalHashingEmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public void replaceAll(List<RagChunk> chunks) {
        List<Entry> next = new ArrayList<>();
        if (chunks != null) {
            for (RagChunk chunk : chunks) {
                float[] vector = embeddingModel.embed(chunk.title() + "\n" + chunk.text());
                next.add(new Entry(chunk, vector));
            }
        }
        entries.clear();
        entries.addAll(next);
    }

    public int size() {
        return entries.size();
    }

    public List<RagHit> search(String query, int topK, float minScore) {
        float[] q = embeddingModel.embed(query == null ? "" : query);
        List<RagHit> scored = new ArrayList<>();
        for (Entry e : entries) {
            float score = cosine(q, e.vector);
            if (score >= minScore) {
                scored.add(new RagHit(e.chunk, score));
            }
        }
        scored.sort(Comparator.comparingDouble(RagHit::score).reversed());
        if (scored.size() > topK) {
            return new ArrayList<>(scored.subList(0, topK));
        }
        return scored;
    }

    private static float cosine(float[] a, float[] b) {
        double dot = 0;
        double na = 0;
        double nb = 0;
        int n = Math.min(a.length, b.length);
        for (int i = 0; i < n; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        if (na == 0 || nb == 0) {
            return 0f;
        }
        return (float) (dot / (Math.sqrt(na) * Math.sqrt(nb)));
    }

    private record Entry(RagChunk chunk, float[] vector) {
    }
}
