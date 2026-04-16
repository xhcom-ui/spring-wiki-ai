package com.xxx.rag.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.embedding.EmbeddingClient;
import org.springframework.ai.openai.OpenAiChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingClient;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI 配置类
 */
@Configuration
public class SpringAiConfig {

    /**
     * 配置 OpenAI API 客户端
     */
    @Bean
    public OpenAiApi openAiApi() {
        return new OpenAiApi();
    }

    /**
     * 配置 ChatClient
     */
    @Bean
    public ChatClient chatClient(OpenAiApi openAiApi) {
        return OpenAiChatClient.builder(openAiApi)
                .withDefaultOptions(OpenAiChatOptions.builder()
                        .withModel("gpt-4")
                        .withTemperature(0.7f)
                        .withMaxTokens(1000)
                        .build())
                .build();
    }

    /**
     * 配置 EmbeddingClient
     */
    @Bean
    public EmbeddingClient embeddingClient(OpenAiApi openAiApi) {
        return new OpenAiEmbeddingClient(openAiApi);
    }

    /**
     * 配置聊天记忆顾问
     */
    @Bean
    public AbstractChatMemoryAdvisor chatMemoryAdvisor() {
        return new MessageChatMemoryAdvisor(new InMemoryChatMemory());
    }
}
