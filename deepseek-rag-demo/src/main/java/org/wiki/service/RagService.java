package org.wiki.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.wiki.config.RagProperties;
import org.wiki.model.KnowledgeDoc;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 自实现 RAG 服务
 * 完整 RAG 流程：文档解析 → 文本切分 → 向量化 → 存储 → 检索 → 生成
 */
@Slf4j
@Service
public class RagService {

    private final VectorStore vectorStore;
    private final ChatClient chatClient;
    private final RagProperties ragProperties;
    private final String uploadDir;

    /**
     * 本地文档元信息存储
     */
    private final Map<String, KnowledgeDoc> docStore = new ConcurrentHashMap<>();

    public RagService(VectorStore vectorStore, ChatClient.Builder chatClientBuilder,
                      RagProperties ragProperties) {
        this.vectorStore = vectorStore;
        this.chatClient = chatClientBuilder.build();
        this.ragProperties = ragProperties;
        this.uploadDir = ragProperties.getUploadDir();
        // 确保上传目录存在
        try {
            Files.createDirectories(Paths.get(uploadDir));
        } catch (IOException e) {
            log.warn("创建上传目录失败: {}", e.getMessage());
        }
    }

    // ==================== 文档管理 ====================

    /**
     * 上传文档 → 解析 → 切分 → 向量化 → 存入向量库
     *
     * @param file 上传的文件
     * @return 文档信息
     */
    public KnowledgeDoc uploadDocument(MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename();
        String docId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        // 1. 保存文件到本地
        Path filePath = Paths.get(uploadDir, docId + "_" + fileName);
        Files.write(filePath, file.getBytes());
        log.info("文件已保存: {}", filePath);

        // 2. 解析文档
        List<Document> documents = parseDocument(filePath.toFile());

        // 3. 切分文档
        List<Document> splitDocs = splitDocuments(documents, fileName);

        // 4. 存入向量数据库
        vectorStore.add(splitDocs);
        log.info("文档向量化完成: fileName={}, chunks={}", fileName, splitDocs.size());

        // 5. 保存元信息
        KnowledgeDoc docInfo = KnowledgeDoc.builder()
                .id(docId)
                .fileName(fileName)
                .fileType(getFileExtension(fileName))
                .fileSize(file.getSize())
                .chunkCount(splitDocs.size())
                .uploadTime(LocalDateTime.now())
                .status("indexed")
                .build();
        docStore.put(docId, docInfo);

        return docInfo;
    }

    /**
     * 上传本地文件路径的文档
     */
    public KnowledgeDoc uploadLocalFile(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new IOException("文件不存在: " + filePath);
        }

        String fileName = file.getName();
        String docId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        List<Document> documents = parseDocument(file);
        List<Document> splitDocs = splitDocuments(documents, fileName);
        vectorStore.add(splitDocs);

        KnowledgeDoc docInfo = KnowledgeDoc.builder()
                .id(docId)
                .fileName(fileName)
                .fileType(getFileExtension(fileName))
                .fileSize(file.length())
                .chunkCount(splitDocs.size())
                .uploadTime(LocalDateTime.now())
                .status("indexed")
                .build();
        docStore.put(docId, docInfo);

        return docInfo;
    }

    /**
     * 获取所有已上传的文档列表
     */
    public List<KnowledgeDoc> listDocuments() {
        return new ArrayList<>(docStore.values());
    }

    /**
     * 删除文档（从元信息中移除，向量库中的数据暂不支持按文档删除）
     */
    public void deleteDocument(String docId) {
        KnowledgeDoc doc = docStore.remove(docId);
        if (doc != null) {
            log.info("文档已删除: id={}, name={}", docId, doc.getFileName());
        }
    }

    // ==================== RAG 检索问答 ====================

    /**
     * RAG 知识库问答（非流式）
     * 完整流程：Query改写 → 向量检索 → 拼接上下文 → LLM生成
     *
     * @param question 用户问题
     * @return 回答
     */
    public String askQuestion(String question) {
        long startTime = System.currentTimeMillis();

        // 1. Query 改写
        String rewrittenQuery = rewriteQuery(question);
        log.info("Query改写: {} -> {}", question, rewrittenQuery);

        // 2. 向量检索
        List<Document> searchResults = vectorSearch(rewrittenQuery);
        log.info("向量检索返回 {} 条结果", searchResults.size());

        if (searchResults.isEmpty()) {
            return "知识库中未找到相关内容，请先上传文档。";
        }

        // 3. 拼接上下文
        String context = buildContext(searchResults);

        // 4. 调用 LLM 生成回答
        String answer = generateAnswer(question, context);

        long endTime = System.currentTimeMillis();
        log.info("RAG问答完成: 耗时={}ms, 检索文档数={}", endTime - startTime, searchResults.size());

        return answer;
    }

    /**
     * RAG 知识库问答（流式）
     */
    public reactor.core.publisher.Flux<String> askQuestionStream(String question) {
        // 1. Query 改写
        String rewrittenQuery = rewriteQuery(question);
        log.info("Query改写(stream): {} -> {}", question, rewrittenQuery);

        // 2. 向量检索
        List<Document> searchResults = vectorSearch(rewrittenQuery);
        log.info("向量检索返回 {} 条结果", searchResults.size());

        if (searchResults.isEmpty()) {
            return reactor.core.publisher.Flux.just("知识库中未找到相关内容，请先上传文档。");
        }

        // 3. 拼接上下文
        String context = buildContext(searchResults);

        // 4. 流式调用 LLM
        String systemPrompt = buildSystemPrompt(context);
        return chatClient.prompt()
                .system(systemPrompt)
                .user(question)
                .stream()
                .content();
    }

    /**
     * 仅检索相关文档（不调用LLM）
     */
    public List<Document> searchDocuments(String query) {
        String rewrittenQuery = rewriteQuery(query);
        return vectorSearch(rewrittenQuery);
    }

    // ==================== 内部方法 ====================

    /**
     * 解析文档（支持 TXT/PDF/Word/Excel/PPT 等）
     */
    private List<Document> parseDocument(File file) {
        TikaDocumentReader reader = new TikaDocumentReader(new FileSystemResource(file));
        return reader.get();
    }

    /**
     * 切分文档
     */
    private List<Document> splitDocuments(List<Document> documents, String fileName) {
        TokenTextSplitter splitter = new TokenTextSplitter(
                ragProperties.getChunk().getDefaultChunkSize(),
                ragProperties.getChunk().getDefaultChunkOverlap(),
                5, 100, true
        );
        List<Document> splitDocs = splitter.apply(documents);

        // 为每个 chunk 添加元信息
        for (int i = 0; i < splitDocs.size(); i++) {
            Document doc = splitDocs.get(i);
            doc.getMetadata().put("chunkIndex", String.valueOf(i + 1));
            doc.getMetadata().put("source", fileName);
        }

        return splitDocs;
    }

    /**
     * 向量检索
     */
    private List<Document> vectorSearch(String query) {
        SearchRequest searchRequest = SearchRequest.builder()
                .query(query)
                .topK(ragProperties.getSearch().getTopK())
                .similarityThreshold(ragProperties.getSearch().getSimilarityThreshold())
                .build();
        return vectorStore.similaritySearch(searchRequest);
    }

    /**
     * Query 改写 - 让查询更适合向量检索
     */
    private String rewriteQuery(String question) {
        try {
            String systemPrompt = """
                    你是一个专业的查询改写助手，负责将用户的自然语言问题改写成更适合向量检索的查询语句。
                    你的任务是：
                    1. 分析用户的意图和核心需求
                    2. 识别关键实体和概念
                    3. 生成一个更精确、更具信息量的查询语句
                    4. 保持查询语句的自然语言风格
                    5. 不要添加任何解释或额外信息，只返回改写后的查询语句
                    """;

            return chatClient.prompt()
                    .system(systemPrompt)
                    .user(question)
                    .call()
                    .content();
        } catch (Exception e) {
            log.warn("Query改写失败，使用原始问题: {}", e.getMessage());
            return question;
        }
    }

    /**
     * 拼接上下文
     */
    private String buildContext(List<Document> documents) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < documents.size(); i++) {
            Document doc = documents.get(i);
            String source = (String) doc.getMetadata().getOrDefault("source", "未知来源");
            sb.append("【文档片段 ").append(i + 1).append(" (来源: ").append(source).append(")】\n");
            sb.append(doc.getText()).append("\n\n");
        }
        return sb.toString();
    }

    /**
     * 构建 RAG 系统提示词
     */
    private String buildSystemPrompt(String context) {
        return """
                你是一个专业的知识库问答助手，请根据提供的上下文内容回答用户的问题。

                回答要求：
                1. 只使用上下文中提供的信息，不要编造答案
                2. 如果上下文中没有相关信息，请明确告知用户"根据提供的资料无法回答该问题"
                3. 回答要准确、完整、有条理
                4. 可以适当引用上下文中的原文
                5. 使用中文回答

                上下文内容：
                %s
                """.formatted(context);
    }

    /**
     * 调用 LLM 生成回答
     */
    private String generateAnswer(String question, String context) {
        String systemPrompt = buildSystemPrompt(context);
        return chatClient.prompt()
                .system(systemPrompt)
                .user(question)
                .call()
                .content();
    }

    private String getFileExtension(String fileName) {
        int idx = fileName.lastIndexOf('.');
        return idx > 0 ? fileName.substring(idx + 1) : "unknown";
    }
}

