package com.museum.ai.rag;

import com.museum.ai.rag.loader.NewsRagLoader;
import com.museum.ai.rag.loader.StaticRulesLoader;
import com.museum.ai.rag.model.ChatIntent;
import com.museum.ai.rag.model.RagChunk;
import com.museum.ai.rag.model.RagDocument;
import com.museum.ai.rag.model.RagHit;
import com.museum.ai.rag.store.InMemoryRagStore;
import com.museum.ai.rag.support.Chunker;
import com.museum.ai.rag.support.HtmlTextCleaner;
import com.museum.ai.rag.support.IntentRouter;
import com.museum.ai.rag.support.LocalHashingEmbeddingModel;
import com.museum.entity.News;
import com.museum.service.NoticeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RAG 单元测试")
class RagUnitTest {

    @Mock
    private NoticeService noticeService;

    private final IntentRouter router = new IntentRouter();

    @Test
    @DisplayName("意图：馆规未命中问句仍 RULES")
    void intent_rules_missQuery() {
        assertEquals(ChatIntent.RULES, router.route("馆规允许饲养企鹅吗"));
    }

    @Test
    @DisplayName("意图：有票 → BOOKING")
    void intent_booking() {
        assertEquals(ChatIntent.BOOKING, router.route("后天下午有票吗"));
        assertEquals(ChatIntent.BOOKING, router.route("我的预约"));
    }

    @Test
    @DisplayName("意图：混合 → MIXED")
    void intent_mixed() {
        assertEquals(ChatIntent.MIXED, router.route("明天下午有票吗？另外可以带水吗"));
    }

    @Test
    @DisplayName("HTML 清洗")
    void cleaner_stripsTags() {
        assertEquals("你好 世界", HtmlTextCleaner.clean("<p>你好</p>&nbsp;<b>世界</b>"));
    }

    @Test
    @DisplayName("切片非空")
    void chunker_splits() {
        RagDocument doc = new RagDocument("id", "news", "n1", "标题", "第一段。\n\n第二段内容。", 1L);
        List<RagChunk> chunks = Chunker.chunk(doc);
        assertFalse(chunks.isEmpty());
    }

    @Test
    @DisplayName("NewsRagLoader 调用 listVisibleForRag 而非 appList")
    void newsLoader_usesFullList() {
        News n = new News();
        n.setNewsId("nid1");
        n.setNewsTitle("临时闭馆");
        n.setNewsDesc("本周六闭馆一天");
        n.setNewsAddTime(1L);
        when(noticeService.listVisibleForRag()).thenReturn(List.of(n));

        NewsRagLoader loader = new NewsRagLoader(noticeService);
        List<RagDocument> docs = loader.load();

        assertEquals(1, docs.size());
        verify(noticeService, times(1)).listVisibleForRag();
        verify(noticeService, never()).appList(any(), any());
    }

    @Test
    @DisplayName("静态须知能检索背包相关")
    void staticRules_retrieveBag() {
        StaticRulesLoader staticLoader = new StaticRulesLoader();
        List<RagDocument> docs = staticLoader.load();
        assertFalse(docs.isEmpty());

        LocalHashingEmbeddingModel emb = new LocalHashingEmbeddingModel();
        InMemoryRagStore store = new InMemoryRagStore(emb);
        List<RagChunk> chunks = docs.stream().flatMap(d -> Chunker.chunk(d).stream()).toList();
        store.replaceAll(chunks);

        List<RagHit> hits = store.search("可以带背包吗", 4, 0.05f);
        assertFalse(hits.isEmpty(), "应能从 visit-rules 召回存包/禁带相关片段");
    }

    @Test
    @DisplayName("未命中低分 → 空列表")
    void retrieve_lowScore_empty() {
        LocalHashingEmbeddingModel emb = new LocalHashingEmbeddingModel();
        InMemoryRagStore store = new InMemoryRagStore(emb);
        store.replaceAll(List.of(new RagChunk("c1", "news", "1", "t", "开放时间九点到五点", null)));
        List<RagHit> hits = store.search("xyzqwertyunrelated999", 4, 0.99f);
        assertTrue(hits.isEmpty());
    }
}
