package com.xxx.rag.rag.store;

import com.xxx.rag.common.constant.RagConstant;
import com.xxx.rag.common.exception.RagException;
import org.springframework.ai.chroma.ChromaEmbeddingStore;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 向量存储服务
 */
public class VectorStoreService {

    private final ChromaEmbeddingStore embeddingStore;

    public VectorStoreService(ChromaEmbeddingStore embeddingStore) {
        this.embeddingStore = embeddingStore;
    }

    /**
     * 存储文档
     */
    public void storeDocuments(List<Document> documents) {
        try {
            // 确保集合存在
            embeddingStore.createCollection(RagConstant.COLLECTION_NAME);

            // 批量存储文档
            embeddingStore.add(documents, RagConstant.COLLECTION_NAME);
        } catch (Exception e) {
            throw new RagException("向量存储失败: " + e.getMessage());
        }
    }

    /**
     * 构建文档
     */
    public Document buildDocument(String content, String docId, String source) {
        Map<String, Object> metadata = Map.of(
                RagConstant.META_DOC_ID, docId,
                RagConstant.META_SOURCE, source,
                RagConstant.META_CREATE_TIME, LocalDateTime.now().toString()
        );

        return new Document(content, metadata);
    }

    /**
     * 搜索文档
     */
    public List<Document> search(String query, int topK, float similarityThreshold) {
        try {
            List<Document> results = embeddingStore.similaritySearch(
                    query, topK, similarityThreshold, RagConstant.COLLECTION_NAME
            );
            return results;
        } catch (Exception e) {
            throw new RagException("向量搜索失败: " + e.getMessage());
        }
    }

    /**
     * 清空集合
     */
    public void clearCollection() {
        try {
            embeddingStore.deleteCollection(RagConstant.COLLECTION_NAME);
        } catch (Exception e) {
            throw new RagException("清空集合失败: " + e.getMessage());
        }
    }
}
