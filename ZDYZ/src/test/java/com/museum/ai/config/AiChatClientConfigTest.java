package com.museum.ai.config;

import com.museum.ai.converter.BookingToolConverter;
import com.museum.ai.tool.BookingTools;
import com.museum.service.JoinService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DisplayName("AiChatClientConfig - DeepSeek Bean 装配")
class AiChatClientConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(AiChatClientConfig.class)
            .withBean(BookingTools.class,
                    () -> new BookingTools(mock(JoinService.class), mock(BookingToolConverter.class)));

    @Test
    @DisplayName("api-key 非空时装配 ChatModel 与双 ChatClient")
    void registersChatBeansWhenApiKeyHasText() {
        contextRunner
                .withPropertyValues(
                        "spring.ai.openai.api-key=sk-test",
                        "spring.ai.openai.base-url=https://api.deepseek.com",
                        "spring.ai.openai.chat.options.model=deepseek-v4-flash")
                .run(context -> {
                    assertThat(context).hasBean("deepSeekChatModel");
                    assertThat(context).hasSingleBean(ChatModel.class);
                    assertThat(context).hasBean("museumBookingChatClient");
                    assertThat(context).hasBean("museumRulesChatClient");
                    assertThat(context.getBeansOfType(ChatClient.class)).hasSize(2);
                });
    }

    @Test
    @DisplayName("api-key 为空白时跳过 ChatModel 与 ChatClient")
    void skipsChatBeansWhenApiKeyIsBlank() {
        contextRunner
                .withPropertyValues("spring.ai.openai.api-key=   ")
                .run(context -> {
                    assertThat(context).doesNotHaveBean("deepSeekChatModel");
                    assertThat(context).doesNotHaveBean("museumBookingChatClient");
                    assertThat(context).doesNotHaveBean("museumRulesChatClient");
                    assertThat(context).doesNotHaveBean(ChatModel.class);
                    assertThat(context).doesNotHaveBean(ChatClient.class);
                });
    }
}
