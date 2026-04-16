package org.wiki.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OllamaConfig {
    @Value("${ollama.api.url:http://localhost:11434}")
    private String apiUrl;

    @Value("${ollama.model:llama2}")
    private String defaultModel;

    public String getApiUrl() {
        return apiUrl;
    }

    public String getDefaultModel() {
        return defaultModel;
    }
}
