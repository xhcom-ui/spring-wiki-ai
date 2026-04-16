package org.wiki.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class LangChain4jConfig {

    @Value("${langchain4j.model.type:ollama}")
    private String modelType;

    @Value("${langchain4j.ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Value("${langchain4j.ollama.model:llama2}")
    private String ollamaModel;

    @Value("${langchain4j.openai.api-key:}")
    private String openAiApiKey;

    @Value("${langchain4j.openai.model:gpt-3.5-turbo}")
    private String openAiModel;

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        if ("openai".equalsIgnoreCase(modelType)) {
            return OpenAiChatModel.builder()
                    .apiKey(openAiApiKey)
                    .modelName(openAiModel)
                    .timeout(Duration.ofSeconds(60))
                    .build();
        } else {
            return OllamaChatModel.builder()
                    .baseUrl(ollamaBaseUrl)
                    .modelName(ollamaModel)
                    .timeout(Duration.ofSeconds(60))
                    .build();
        }
    }
}
