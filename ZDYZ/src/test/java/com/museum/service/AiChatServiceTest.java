package com.museum.service;

import com.museum.ai.dto.AiChatResponse;
import com.museum.ai.rag.model.ChatIntent;
import com.museum.ai.rag.model.RagChunk;
import com.museum.ai.rag.model.RagHit;
import com.museum.ai.rag.service.RagService;
import com.museum.ai.rag.support.IntentRouter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiChatService - miniapp response shape")
class AiChatServiceTest {

    @Mock
    private ObjectProvider<ChatClient> bookingProvider;
    @Mock
    private ObjectProvider<ChatClient> rulesProvider;
    @Mock
    private ObjectProvider<RagService> ragProvider;
    @Mock
    private RagService ragService;

    @Test
    @DisplayName("booking client 缺失时返回配置提示和 tips block")
    void chat_whenBookingClientMissing_returnsConfigHintResponse() {
        when(bookingProvider.getIfAvailable()).thenReturn(null);
        AiChatService service = new AiChatService(
                bookingProvider, rulesProvider, ragProvider, new IntentRouter());

        AiChatResponse response = service.chat("后天下午有票吗");

        assertEquals(AiChatService.CONFIG_HINT, response.getReply());
        assertEquals(ChatIntent.BOOKING, response.getIntent());
        assertEquals(1, response.getBlocks().size());
        assertEquals("tips", response.getBlocks().get(0).getType());
        assertTrue(response.getSuggestions().contains("我的预约"));
    }

    @Test
    @DisplayName("rules 命中时返回 rules_source blocks")
    void chat_whenRulesHit_returnsRulesSourceBlocks() {
        ChatClient rulesClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        when(rulesProvider.getIfAvailable()).thenReturn(rulesClient);
        when(ragProvider.getIfAvailable()).thenReturn(ragService);
        when(ragService.retrieve("可以带背包吗")).thenReturn(List.of(
                new RagHit(new RagChunk("c1", "static_rules", "visit-rules", "参观须知",
                        "大件行李必须寄存。", 1723852800000L), 0.92f)
        ));
        when(ragService.formatContext(org.mockito.ArgumentMatchers.anyList()))
                .thenReturn("检索片段：大件行李必须寄存。");
        when(rulesClient.prompt().user(anyString()).call().content())
                .thenReturn("根据参观须知，大件行李必须寄存。");

        AiChatService service = new AiChatService(
                bookingProvider, rulesProvider, ragProvider, new IntentRouter());

        AiChatResponse response = service.chat("可以带背包吗");

        assertEquals(ChatIntent.RULES, response.getIntent());
        assertEquals("根据参观须知，大件行李必须寄存。", response.getReply());
        assertFalse(response.getBlocks().isEmpty());
        assertEquals("rules_source", response.getBlocks().get(0).getType());
        assertEquals("static_rules", response.getBlocks().get(0).getSource());
        assertTrue(response.getSuggestions().contains("几点停止入馆"));
    }

    @Test
    @DisplayName("rules 未命中时直接返回 RAG miss")
    void chat_whenRulesMiss_returnsMissReply() {
        when(ragProvider.getIfAvailable()).thenReturn(ragService);
        when(ragService.retrieve("馆规允许饲养企鹅吗")).thenReturn(List.of());
        AiChatService service = new AiChatService(
                bookingProvider, rulesProvider, ragProvider, new IntentRouter());

        AiChatResponse response = service.chat("馆规允许饲养企鹅吗");

        assertEquals(ChatIntent.RULES, response.getIntent());
        assertEquals(RagService.MISS_REPLY, response.getReply());
        assertEquals("tips", response.getBlocks().get(0).getType());
        assertEquals("rag_miss", response.getBlocks().get(0).getSource());
    }
}
