package org.wiki.service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.wiki.entity.KnowledgeBase;
import org.wiki.repository.KnowledgeBaseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class KnowledgeBaseService {

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final DocumentParserService documentParserService;
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;

    @Autowired
    public KnowledgeBaseService(KnowledgeBaseRepository knowledgeBaseRepository,
                                DocumentParserService documentParserService) {
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.documentParserService = documentParserService;
        this.embeddingModel = new AllMiniLmL6V2EmbeddingModel();
        this.embeddingStore = new InMemoryEmbeddingStore<>();
    }

    public KnowledgeBase uploadFile(MultipartFile file, String serviceUrl) throws Exception {
        // 解析文档
        String content = documentParserService.parseDocument(file);

        // 创建知识库记录
        KnowledgeBase knowledgeBase = new KnowledgeBase();
        knowledgeBase.setFileName(file.getOriginalFilename());
        knowledgeBase.setFileType(getFileExtension(file.getOriginalFilename()));
        knowledgeBase.setFileSize(file.getSize());
        knowledgeBase.setContent(content);
        knowledgeBase.setServiceUrl(serviceUrl);
        knowledgeBase.setUploadTime(LocalDateTime.now());
        knowledgeBase.setStatus("已上传");

        // 保存到数据库
        KnowledgeBase saved = knowledgeBaseRepository.save(knowledgeBase);

        // 创建嵌入并存储
        createEmbeddings(saved.getId(), content);

        return saved;
    }

    public List<KnowledgeBase> getAllKnowledgeBases() {
        return knowledgeBaseRepository.findAll();
    }

    public KnowledgeBase getKnowledgeBaseById(Long id) {
        return knowledgeBaseRepository.findById(id).orElse(null);
    }

    public void deleteKnowledgeBase(Long id) {
        knowledgeBaseRepository.deleteById(id);
    }

    public List<KnowledgeBase> searchByFileName(String fileName) {
        return knowledgeBaseRepository.findByFileNameContaining(fileName);
    }

    public List<Map<String, Object>> searchSimilarContent(String query, int maxResults) {
        // 将查询转换为嵌入
        Embedding queryEmbedding = embeddingModel.embed(query).content();

        // 搜索相似内容
        var relevant = embeddingStore.findRelevant(queryEmbedding, maxResults);

        return relevant.stream()
                .map(matchResult -> {
                    Map<String, Object> result = new java.util.HashMap<>();
                    result.put("text", matchResult.embedded().text());
                    result.put("score", matchResult.score());
                    return result;
                })
                .collect(Collectors.toList());
    }

    private void createEmbeddings(Long knowledgeBaseId, String content) {
        // 将内容分割成小块
        List<TextSegment> segments = splitContent(content);

        // 为每个片段创建嵌入
        for (TextSegment segment : segments) {
            Embedding embedding = embeddingModel.embed(segment).content();
            embeddingStore.add(embedding, segment);
        }
    }

    private List<TextSegment> splitContent(String content) {
        List<TextSegment> segments = new ArrayList<>();
        int chunkSize = 500;
        int overlap = 50;

        for (int i = 0; i < content.length(); i += chunkSize - overlap) {
            int end = Math.min(i + chunkSize, content.length());
            String chunk = content.substring(i, end);
            segments.add(TextSegment.from(chunk));
            if (end == content.length()) break;
        }

        return segments;
    }

    private String getFileExtension(String fileName) {
        if (fileName == null) return "";
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1) return "";
        return fileName.substring(lastDotIndex + 1).toLowerCase();
    }
}
