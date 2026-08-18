package com.museum.ai.rag.service;

import com.museum.ai.rag.config.RagProperties;
import com.museum.ai.rag.loader.NewsRagLoader;
import com.museum.ai.rag.loader.StaticRulesLoader;
import com.museum.ai.rag.model.RagChunk;
import com.museum.ai.rag.model.RagDocument;
import com.museum.ai.rag.model.RagHit;
import com.museum.ai.rag.store.InMemoryRagStore;
import com.museum.ai.rag.support.Chunker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@ConditionalOnProperty(prefix = "museum.ai.rag", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RagService {

    public static final String MISS_REPLY =
            "我目前没有查到相关馆规或公告，请以馆内现场提示或小程序「预约须知」页面为准，不要把我的推测当作规定。";

    private static final Logger log = LoggerFactory.getLogger(RagService.class);

    private final RagProperties properties;
    private final NewsRagLoader newsRagLoader;
    private final StaticRulesLoader staticRulesLoader;
    private final InMemoryRagStore store;

    public RagService(
            RagProperties properties,
            NewsRagLoader newsRagLoader,
            StaticRulesLoader staticRulesLoader,
            InMemoryRagStore store) {
        this.properties = properties;
        this.newsRagLoader = newsRagLoader;
        this.staticRulesLoader = staticRulesLoader;
        this.store = store;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        if (properties.isRebuildOnStartup()) {
            rebuild();
        }
    }

    public void rebuild() {
        List<RagDocument> docs = new ArrayList<>();
        if (properties.getSources().isNewsEnabled()) {
            docs.addAll(newsRagLoader.load());
        }
        if (properties.getSources().isStaticRulesEnabled()) {
            docs.addAll(staticRulesLoader.load());
        }
        List<RagChunk> chunks = new ArrayList<>();
        for (RagDocument doc : docs) {
            chunks.addAll(Chunker.chunk(doc));
        }
        store.replaceAll(chunks);
        log.info("RAG 索引重建完成: docs={}, chunks={}", docs.size(), chunks.size());
    }

    public List<RagHit> retrieve(String query) {
        return store.search(query, properties.getTopK(), properties.getMinScore());
    }

    public String formatContext(List<RagHit> hits) {
        if (hits == null || hits.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("以下为检索到的馆规/公告片段，回答必须只依据这些内容，并点明来源类型（news 或 static_rules）。\n");
        int i = 1;
        for (RagHit hit : hits) {
            RagChunk c = hit.chunk();
            sb.append("[").append(i++).append("] sourceType=").append(c.sourceType())
                    .append(", title=").append(c.title())
                    .append(", sourceId=").append(c.sourceId())
                    .append("\n")
                    .append(c.text())
                    .append("\n\n");
        }
        return sb.toString().trim();
    }

    public int indexedChunkCount() {
        return store.size();
    }
}
