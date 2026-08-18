package com.museum.ai.config;

import com.museum.ai.tool.BookingTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * 博物馆预约助手：用 application.yml 中的 DeepSeek（OpenAI 兼容）配置装配 ChatClient。
 * 本项目不使用环境变量注入 Key。
 * <p>
 * 双 Client：booking（含 Tool）与 rules（无 Tool，纯馆规 RAG）。
 */
@Configuration
public class AiChatClientConfig {

    public static final String BOOKING_SYSTEM_PROMPT = """
            你是博物馆预约助手。余票、预约结果、预约号只信 Tool 返回，禁止编造 remain、joinId 或取消结果。
            用户问有没有票时，先调用 queryDays 或 queryTimes，再根据 Tool 结果回答。
            「有票就帮我订」时，确认时段与游客 identityIds 后调用 submitBooking；成功若需要预约号再调用 listRecords，禁止编造预约号。
            取消必须使用 listRecords 返回的真实 joinId，禁止猜测。
            未登录时写操作会失败：提示用户先登录，不要假装已下单或已取消。
            馆规、须知、公告类事实只信用户消息中附带的「检索片段」；没有检索片段时不要编造馆规。
            若用户同时问预约和馆规：分段回答——预约部分只信 Tool，馆规部分只信检索片段，并点明来源类型（news 或 static_rules）。
            """;

    public static final String RULES_SYSTEM_PROMPT = """
            你是博物馆参观须知助手。你只能根据用户消息中提供的「检索片段」回答馆规、开放时间、禁带物品、存包、拍照、公告等问题。
            禁止编造未出现在检索片段中的规定；禁止调用或假装已调用任何预约工具。
            回答时尽量点明信息来源类型（如根据参观须知 static_rules，或根据公告 news）。
            若检索片段不足以回答，明确说不知道。
            """;

    /** @deprecated 保留别名供旧测试引用；与 BOOKING_SYSTEM_PROMPT 相同语义起点 */
    public static final String SYSTEM_PROMPT = BOOKING_SYSTEM_PROMPT;

    @Bean
    @Primary
    @ConditionalOnExpression("T(org.springframework.util.StringUtils).hasText('${spring.ai.openai.api-key:}')")
    public ChatModel deepSeekChatModel(
            @Value("${spring.ai.openai.api-key:}") String apiKey,
            @Value("${spring.ai.openai.base-url:https://api.deepseek.com}") String baseUrl,
            @Value("${spring.ai.openai.chat.options.model:deepseek-v4-flash}") String model) {
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .build();
        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(OpenAiChatOptions.builder().model(model).build())
                .build();
    }

    @Bean
    @ConditionalOnExpression("T(org.springframework.util.StringUtils).hasText('${spring.ai.openai.api-key:}')")
    public ChatClient museumBookingChatClient(ChatModel deepSeekChatModel, BookingTools bookingTools) {
        return ChatClient.builder(deepSeekChatModel)
                .defaultSystem(BOOKING_SYSTEM_PROMPT)
                .defaultTools(bookingTools)
                .build();
    }

    /**
     * 纯馆规问答：禁止绑定 BookingTools，避免误下单。
     */
    @Bean
    @ConditionalOnExpression("T(org.springframework.util.StringUtils).hasText('${spring.ai.openai.api-key:}')")
    public ChatClient museumRulesChatClient(ChatModel deepSeekChatModel) {
        return ChatClient.builder(deepSeekChatModel)
                .defaultSystem(RULES_SYSTEM_PROMPT)
                .build();
    }
}
