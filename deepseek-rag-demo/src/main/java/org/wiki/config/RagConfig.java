package org.wiki.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * RAG 向量库配置
 * 使用 SimpleVectorStore，支持本地文件持久化
 * 应用关闭时自动保存向量数据，启动时自动加载
 * 生产环境建议替换为 Milvus / Chroma / Redis 等持久化方案
 */
@Slf4j
@Configuration
public class RagConfig {

    @Value("${rag.vector-store.persist-dir:./vector-store}")
    private String persistDir;

    private SimpleVectorStore vectorStore;

    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        this.vectorStore = SimpleVectorStore.builder(embeddingModel).build();

        // 启动时尝试加载已有向量数据
        Path persistPath = Paths.get(persistDir, "vector-store.json");
        if (Files.exists(persistPath)) {
            try {
                vectorStore.load(new File(persistPath.toUri()));
                log.info("向量库数据已加载: {}", persistPath);
            } catch (Exception e) {
                log.warn("加载向量库数据失败，将从空库开始: {}", e.getMessage());
            }
        } else {
            log.info("未找到持久化向量数据，从空库开始");
        }

        return vectorStore;
    }

    /**
     * 应用关闭时保存向量数据到文件
     */
    @EventListener
    public void onApplicationClose(ContextClosedEvent event) {
        if (vectorStore != null) {
            try {
                Files.createDirectories(Paths.get(persistDir));
                Path persistPath = Paths.get(persistDir, "vector-store.json");
                vectorStore.save(new File(persistPath.toUri()));
                log.info("向量库数据已持久化: {}", persistPath);
            } catch (Exception e) {
                log.error("保存向量库数据失败: {}", e.getMessage());
            }
        }
    }
}
