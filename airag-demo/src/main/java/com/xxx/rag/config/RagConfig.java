package com.xxx.rag.config;

import com.xxx.rag.rag.embedding.EmbeddingService;
import com.xxx.rag.rag.store.VectorStoreService;
import com.xxx.rag.rag.retriever.VectorRetriever;
import com.xxx.rag.rag.rerank.Reranker;
import com.xxx.rag.service.facades.RagServiceFacade;
import com.xxx.rag.service.impl.RagServiceImpl;
import org.springframework.ai.chroma.ChromaEmbeddingStore;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RAG 配置类
 */
@Configuration
public class RagConfig {

    /**
     * 配置 EmbeddingService
     */
    @Bean
    public EmbeddingService embeddingService(EmbeddingClient embeddingClient) {
        return new EmbeddingService(embeddingClient);
    }

    /**
     * 配置 VectorStoreService
     */
    @Bean
    public VectorStoreService vectorStoreService(ChromaEmbeddingStore chromaEmbeddingStore) {
        return new VectorStoreService(chromaEmbeddingStore);
    }

    /**
     * 配置 VectorRetriever
     */
    @Bean
    public VectorRetriever vectorRetriever(VectorStoreService vectorStoreService) {
        return new VectorRetriever(vectorStoreService);
    }

    /**
     * 配置 Reranker
     */
    @Bean
    public Reranker reranker(ChatClient chatClient) {
        return new Reranker(chatClient);
    }

    /**
     * 配置 RagServiceFacade
     */
    @Bean
    public RagServiceFacade ragServiceFacade(VectorStoreService vectorStoreService, 
                                           VectorRetriever vectorRetriever, 
                                           Reranker reranker, 
                                           ChatClient chatClient) {
        return new RagServiceFacade(vectorStoreService, vectorRetriever, reranker, chatClient);
    }

    /**
     * 配置 RagServiceImpl
     */
    @Bean
    public RagServiceImpl ragServiceImpl(RagServiceFacade ragServiceFacade) {
        return new RagServiceImpl(ragServiceFacade);
    }
}
