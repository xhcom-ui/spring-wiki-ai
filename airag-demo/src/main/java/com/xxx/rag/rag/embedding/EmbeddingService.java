package com.xxx.rag.rag.embedding;

import com.xxx.rag.common.exception.RagException;
import org.springframework.ai.embedding.EmbeddingClient;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;

import java.util.List;

/**
 * 向量化服务
 */
public class EmbeddingService {

    private final EmbeddingClient embeddingClient;

    public EmbeddingService(EmbeddingClient embeddingClient) {
        this.embeddingClient = embeddingClient;
    }

    /**
     * 生成文本向量
     */
    public List<Float> embed(String text) {
        try {
            EmbeddingRequest request = EmbeddingRequest.builder()
                    .withInput(text)
                    .build();

            EmbeddingResponse response = embeddingClient.embed(request);
            if (response.getResults().isEmpty()) {
                throw new RagException("向量化失败: 没有返回向量");
            }

            return response.getResults().get(0).getEmbedding();
        } catch (Exception e) {
            throw new RagException("向量化失败: " + e.getMessage());
        }
    }

    /**
     * 批量生成文本向量
     */
    public List<List<Float>> embedBatch(List<String> texts) {
        try {
            EmbeddingRequest request = EmbeddingRequest.builder()
                    .withInput(texts)
                    .build();

            EmbeddingResponse response = embeddingClient.embed(request);
            return response.getResults().stream()
                    .map(result -> result.getEmbedding())
                    .toList();
        } catch (Exception e) {
            throw new RagException("批量向量化失败: " + e.getMessage());
        }
    }
}
