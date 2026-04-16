package com.xxx.rag.config;

import org.springframework.ai.chroma.ChromaClient;
import org.springframework.ai.chroma.ChromaEmbeddingStore;
import org.springframework.ai.embedding.EmbeddingClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Chroma 向量库配置类
 */
@Configuration
public class ChromaConfig {

    /**
     * 配置 Chroma 客户端
     */
    @Bean
    public ChromaClient chromaClient() {
        return new ChromaClient("http://localhost:8000");
    }

    /**
     * 配置 Chroma 嵌入存储
     */
    @Bean
    public ChromaEmbeddingStore chromaEmbeddingStore(ChromaClient chromaClient, EmbeddingClient embeddingClient) {
        return new ChromaEmbeddingStore(chromaClient, embeddingClient);
    }
}
