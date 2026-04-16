package org.wiki.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * RAG 自有配置属性
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "rag")
public class RagProperties {

    private VectorStore vectorStore = new VectorStore();
    private Search search = new Search();
    private Chunk chunk = new Chunk();
    private String uploadDir = "./uploads";

    @Data
    public static class VectorStore {
        private String type = "simple";
    }

    @Data
    public static class Search {
        private int topK = 5;
        private double similarityThreshold = 0.5;
    }

    @Data
    public static class Chunk {
        private int defaultChunkSize = 800;
        private int defaultChunkOverlap = 200;
    }
}
