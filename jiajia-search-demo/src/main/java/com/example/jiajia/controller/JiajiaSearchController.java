package com.example.jiajia.controller;

import com.example.jiajia.entity.Document;
import com.example.jiajia.service.JiajiaSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/jiajia")
public class JiajiaSearchController {

    @Autowired
    private JiajiaSearchService jiajiaSearchService;

    // 生成单个文本的嵌入向量
    @PostMapping("/embedding/single")
    public ResponseEntity<Map<String, Object>> generateEmbedding(@RequestBody Map<String, String> request) {
        try {
            String text = request.get("text");
            if (text == null || text.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "文本不能为空"));
            }
            Map<String, Object> result = jiajiaSearchService.generateEmbedding(text);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "生成嵌入失败: " + e.getMessage()));
        }
    }

    // 批量生成文本的嵌入向量
    @PostMapping("/embedding/batch")
    public ResponseEntity<Map<String, Object>> generateEmbeddingsBatch(@RequestBody Map<String, List<String>> request) {
        try {
            List<String> texts = request.get("texts");
            if (texts == null || texts.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "文本列表不能为空"));
            }
            Map<String, Object> result = jiajiaSearchService.generateEmbeddingsBatch(texts);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "批量生成嵌入失败: " + e.getMessage()));
        }
    }

    // 执行搜索流水线
    @PostMapping("/search/pipeline")
    public ResponseEntity<Map<String, Object>> searchPipeline(@RequestBody Map<String, Object> request) {
        try {
            String query = (String) request.get("query");
            List<String> documents = (List<String>) request.get("documents");
            if (query == null || query.isEmpty() || documents == null || documents.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "查询和文档列表不能为空"));
            }
            Map<String, Object> result = jiajiaSearchService.searchPipeline(query, documents);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "搜索流水线失败: " + e.getMessage()));
        }
    }

    // 保存文档
    @PostMapping("/document/save")
    public ResponseEntity<Document> saveDocument(@RequestBody Map<String, String> request) {
        try {
            String title = request.get("title");
            String content = request.get("content");
            String type = request.get("type");
            if (title == null || title.isEmpty() || content == null || content.isEmpty()) {
                return ResponseEntity.badRequest().body(null);
            }
            Document document = jiajiaSearchService.saveDocument(title, content, type);
            return ResponseEntity.ok(document);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    // 获取所有文档
    @GetMapping("/documents")
    public ResponseEntity<List<Document>> getAllDocuments() {
        try {
            List<Document> documents = jiajiaSearchService.getAllDocuments();
            return ResponseEntity.ok(documents);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    // 搜索文档
    @PostMapping("/search/documents")
    public ResponseEntity<Map<String, Object>> searchDocuments(@RequestBody Map<String, String> request) {
        try {
            String query = request.get("query");
            if (query == null || query.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "查询不能为空"));
            }
            Map<String, Object> result = jiajiaSearchService.searchDocuments(query);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "搜索文档失败: " + e.getMessage()));
        }
    }
}
