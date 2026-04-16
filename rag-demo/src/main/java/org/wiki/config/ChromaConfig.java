package org.wiki.config;

import org.springframework.ai.chroma.vectorstore.ChromaApi;
import org.springframework.ai.chroma.vectorstore.ChromaVectorStore;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChromaConfig {

    // 1. 正确创建 ChromaApi
    @Bean
    public ChromaApi chromaApi() {
        return new ChromaApi("http://localhost:8000");
    }

    // 2. 正确创建 VectorStore（必须传入 EmbeddingModel）
    @Bean
    public VectorStore vectorStore(ChromaApi chromaApi, EmbeddingModel embeddingModel) {
        return ChromaVectorStore.builder(chromaApi, embeddingModel).build();
    }
}