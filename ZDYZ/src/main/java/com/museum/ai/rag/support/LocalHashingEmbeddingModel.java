package com.museum.ai.rag.support;

/**
 * 本地无网络 Embedding：字符 bigram 哈希到固定维并 L2 归一化。
 */
public class LocalHashingEmbeddingModel {

    public static final int DIMENSION = 384;

    public float[] embed(String text) {
        float[] v = new float[DIMENSION];
        if (text == null || text.isBlank()) {
            return v;
        }
        String normalized = text.toLowerCase().replaceAll("\\s+", "");
        for (int i = 0; i < normalized.length(); i++) {
            int h1 = Math.floorMod(normalized.charAt(i) * 31, DIMENSION);
            v[h1] += 1.0f;
            if (i + 1 < normalized.length()) {
                int h2 = Math.floorMod(
                        (normalized.charAt(i) * 131 + normalized.charAt(i + 1)) * 17,
                        DIMENSION);
                v[h2] += 1.5f;
            }
            if (i + 2 < normalized.length()) {
                int h3 = Math.floorMod(
                        (normalized.charAt(i) * 131 + normalized.charAt(i + 1)) * 13
                                + normalized.charAt(i + 2),
                        DIMENSION);
                v[h3] += 1.2f;
            }
        }
        double norm = 0;
        for (float x : v) {
            norm += x * x;
        }
        if (norm > 0) {
            float inv = (float) (1.0 / Math.sqrt(norm));
            for (int i = 0; i < v.length; i++) {
                v[i] *= inv;
            }
        }
        return v;
    }
}
