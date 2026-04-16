package com.xxx.rag.rag.retriever;

import com.xxx.rag.common.constant.RagConstant;
import com.xxx.rag.rag.store.VectorStoreService;
import org.springframework.ai.document.Document;

import java.util.List;

/**
 * 向量检索器
 */
public class VectorRetriever {

    private final VectorStoreService vectorStoreService;

    public VectorRetriever(VectorStoreService vectorStoreService) {
        this.vectorStoreService = vectorStoreService;
    }

    /**
     * 检索相关文档
     */
    public List<Document> retrieve(String query) {
        return retrieve(query, RagConstant.DEFAULT_TOP_K, RagConstant.SIMILARITY_THRESHOLD);
    }

    /**
     * 检索相关文档
     */
    public List<Document> retrieve(String query, int topK, float similarityThreshold) {
        // 限制 topK 最大值
        if (topK > RagConstant.MAX_TOP_K) {
            topK = RagConstant.MAX_TOP_K;
        }

        // 执行向量搜索
        return vectorStoreService.search(query, topK, similarityThreshold);
    }
}
