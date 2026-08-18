package com.museum.service;

import com.museum.ai.dto.AiChatResponse;
import com.museum.ai.dto.ChatBlock;
import com.museum.ai.rag.model.ChatIntent;
import com.museum.ai.rag.model.RagChunk;
import com.museum.ai.rag.model.RagHit;
import com.museum.ai.rag.service.RagService;
import com.museum.ai.rag.support.IntentRouter;
import com.museum.ai.support.AiChatBlockCollector;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI chat service with intent routing, deterministic blocks and miniapp-friendly response shape.
 */
@Service
public class AiChatService {

    static final String CONFIG_HINT =
            "AI 服务未配置：请在 application.yml 中配置 spring.ai.openai.api-key（DeepSeek Key）";

    private final ObjectProvider<ChatClient> bookingChatClientProvider;
    private final ObjectProvider<ChatClient> rulesChatClientProvider;
    private final ObjectProvider<RagService> ragServiceProvider;
    private final IntentRouter intentRouter;

    public AiChatService(
            @Qualifier("museumBookingChatClient") ObjectProvider<ChatClient> bookingChatClientProvider,
            @Qualifier("museumRulesChatClient") ObjectProvider<ChatClient> rulesChatClientProvider,
            ObjectProvider<RagService> ragServiceProvider,
            IntentRouter intentRouter) {
        this.bookingChatClientProvider = bookingChatClientProvider;
        this.rulesChatClientProvider = rulesChatClientProvider;
        this.ragServiceProvider = ragServiceProvider;
        this.intentRouter = intentRouter;
    }

    public AiChatResponse chat(String userMessage) {
        String message = userMessage == null ? "" : userMessage;
        ChatIntent intent = intentRouter.route(message);
        RagService rag = ragServiceProvider.getIfAvailable();
        AiChatBlockCollector.begin();
        try {
            return switch (intent) {
                case RULES -> chatRules(message, rag);
                case MIXED -> chatMixed(message, rag);
                case BOOKING -> chatBooking(message);
            };
        } catch (Exception e) {
            return response("发生错误: " + e.getMessage(), intent,
                    List.of(AiChatBlockCollector.tipBlock("异常提示", "发生错误: " + e.getMessage(), "error")),
                    suggestionsFor(intent));
        } finally {
            AiChatBlockCollector.clear();
        }
    }

    private AiChatResponse chatBooking(String message) {
        ChatClient client = bookingChatClientProvider.getIfAvailable();
        if (client == null) {
            return configResponse(ChatIntent.BOOKING);
        }
        String reply = call(client, message);
        return response(reply, ChatIntent.BOOKING, AiChatBlockCollector.snapshot(), suggestionsFor(ChatIntent.BOOKING));
    }

    private AiChatResponse chatRules(String message, RagService rag) {
        if (rag == null) {
            return ragMissResponse(ChatIntent.RULES);
        }
        List<RagHit> hits = rag.retrieve(message);
        if (hits.isEmpty()) {
            return ragMissResponse(ChatIntent.RULES);
        }
        ChatClient client = rulesChatClientProvider.getIfAvailable();
        if (client == null) {
            return configResponse(ChatIntent.RULES);
        }
        String prompt = rag.formatContext(hits) + "\n\n用户问题：" + message;
        String reply = call(client, prompt);
        return response(reply, ChatIntent.RULES, toRulesBlocks(hits), suggestionsFor(ChatIntent.RULES));
    }

    private AiChatResponse chatMixed(String message, RagService rag) {
        ChatClient client = bookingChatClientProvider.getIfAvailable();
        if (client == null) {
            return configResponse(ChatIntent.MIXED);
        }
        List<ChatBlock> blocks = new ArrayList<>();
        StringBuilder user = new StringBuilder();
        if (rag != null) {
            List<RagHit> hits = rag.retrieve(message);
            if (!hits.isEmpty()) {
                blocks.addAll(toRulesBlocks(hits));
                user.append(rag.formatContext(hits)).append("\n\n");
            } else {
                blocks.add(AiChatBlockCollector.tipBlock("馆规检索", RagService.MISS_REPLY, "rag_miss"));
                user.append("（馆规检索未命中：馆规部分请明确说未查到相关规定。）\n\n");
            }
        } else {
            blocks.add(AiChatBlockCollector.tipBlock("馆规检索", RagService.MISS_REPLY, "rag_disabled"));
        }
        user.append("用户问题：").append(message)
                .append("\n请分段回答：预约事务只信 Tool；馆规只信上方检索片段。");
        String reply = call(client, user.toString());
        blocks.addAll(AiChatBlockCollector.snapshot());
        return response(reply, ChatIntent.MIXED, blocks, suggestionsFor(ChatIntent.MIXED));
    }

    private static String call(ChatClient client, String userMessage) {
        String content = client.prompt()
                .user(userMessage)
                .call()
                .content();
        if (content == null || content.isBlank()) {
            return "AI 服务暂无响应";
        }
        return content;
    }

    private static AiChatResponse configResponse(ChatIntent intent) {
        return response(CONFIG_HINT, intent,
                List.of(AiChatBlockCollector.tipBlock("配置提示", CONFIG_HINT, "config")),
                suggestionsFor(intent));
    }

    private static AiChatResponse ragMissResponse(ChatIntent intent) {
        return response(RagService.MISS_REPLY, intent,
                List.of(AiChatBlockCollector.tipBlock("馆规检索", RagService.MISS_REPLY, "rag_miss")),
                suggestionsFor(intent));
    }

    private static AiChatResponse response(
            String reply,
            ChatIntent intent,
            List<ChatBlock> blocks,
            List<String> suggestions) {
        return new AiChatResponse(reply, intent, new ArrayList<>(blocks), new ArrayList<>(suggestions));
    }

    private static List<ChatBlock> toRulesBlocks(List<RagHit> hits) {
        List<ChatBlock> blocks = new ArrayList<>();
        for (RagHit hit : hits) {
            RagChunk chunk = hit.chunk();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("title", chunk.title());
            item.put("text", chunk.text());
            item.put("sourceId", chunk.sourceId());
            item.put("sourceType", chunk.sourceType());
            item.put("score", hit.score());
            item.put("publishedAt", chunk.publishedAt());
            blocks.add(new ChatBlock(
                    "rules_source",
                    chunk.title(),
                    List.of(item),
                    chunk.sourceType()));
        }
        return blocks;
    }

    private static List<String> suggestionsFor(ChatIntent intent) {
        return switch (intent) {
            case BOOKING -> List.of("后天下午有票吗", "我的预约", "查询最近可预约日期");
            case RULES -> List.of("可以带背包吗", "几点停止入馆", "可以带水吗");
            case MIXED -> List.of("明天下午有票吗", "我的预约", "可以带背包吗", "几点停止入馆");
        };
    }
}
