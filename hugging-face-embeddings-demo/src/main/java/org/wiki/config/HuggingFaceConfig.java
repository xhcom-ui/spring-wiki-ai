package org.wiki.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HuggingFaceConfig {

    @Value("${huggingface.api-key:}")
    private String apiKey;

    @Value("${huggingface.embedding-model:sentence-transformers/all-MiniLM-L6-v2}")
    private String embeddingModel;

    @Value("${huggingface.api-url:https://api-inference.huggingface.co/models/}")
    private String apiUrl;

    public String getApiKey() {
        return apiKey;
    }

    public String getEmbeddingModel() {
        return embeddingModel;
    }

    public String getApiUrl() {
        return apiUrl;
    }
}
