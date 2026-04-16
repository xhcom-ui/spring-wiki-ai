package com.xxx.rag.service.facades;

import com.xxx.rag.common.exception.RagException;
import com.xxx.rag.rag.loader.DocumentLoader;
import com.xxx.rag.rag.splitter.TextSplitter;
import com.xxx.rag.rag.store.VectorStoreService;
import com.xxx.rag.rag.retriever.VectorRetriever;
import com.xxx.rag.rag.rerank.Reranker;
import com.xxx.rag.rag.prompt.PromptManager;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * RAG 服务 facade
 */
public class RagServiceFacade {

    private final VectorStoreService vectorStoreService;
    private final VectorRetriever vectorRetriever;
    private final Reranker reranker;
    private final ChatClient chatClient;

    public RagServiceFacade(VectorStoreService vectorStoreService, VectorRetriever vectorRetriever, 
                          Reranker reranker, ChatClient chatClient) {
        this.vectorStoreService = vectorStoreService;
        this.vectorRetriever = vectorRetriever;
        this.reranker = reranker;
        this.chatClient = chatClient;
    }

    /**
     * 处理文档上传和索引
     */
    public void processDocument(File file, String docType) {
        try {
            // 1. 加载文档
            String content = DocumentLoader.loadDocument(file, docType);

            // 2. 文本分块
            List<String> chunks = TextSplitter.splitText(content);

            // 3. 构建文档对象
            List<Document> documents = new ArrayList<>();
            String docId = UUID.randomUUID().toString();
            for (String chunk : chunks) {
                Document document = vectorStoreService.buildDocument(chunk, docId, file.getName());
                documents.add(document);
            }

            // 4. 向量存储
            vectorStoreService.storeDocuments(documents);
        } catch (Exception e) {
            throw new RagException("文档处理失败: " + e.getMessage());
        }
    }

    /**
     * 处理用户查询
     */
    public String processQuery(String query) {
        try {
            // 1. 向量检索
            List<Document> retrievedDocs = vectorRetriever.retrieve(query);

            if (retrievedDocs.isEmpty()) {
                return "没有找到相关文档，请尝试其他查询。";
            }

            // 2. 重排序
            List<Document> rerankedDocs = reranker.rerank(query, retrievedDocs);

            // 3. 构建 Prompt
            String prompt = PromptManager.buildRagPrompt(query, rerankedDocs);

            // 4. 模型生成
            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            return response;
        } catch (Exception e) {
            throw new RagException("查询处理失败: " + e.getMessage());
        }
    }

    /**
     * 清空向量库
     */
    public void clearVectorStore() {
        vectorStoreService.clearCollection();
    }
}
