package org.wiki.controller;

import org.wiki.service.EmbeddingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/embedding")
@CrossOrigin(origins = "*")
public class EmbeddingController {

    private final EmbeddingService embeddingService;

    @Autowired
    public EmbeddingController(EmbeddingService embeddingService) {
        this.embeddingService = embeddingService;
    }

    // 获取单个文本的嵌入
    @PostMapping("/single")
    public ResponseEntity<Map<String, Object>> getEmbedding(@RequestBody Map<String, String> request) {
        try {
            String text = request.get("text");
            if (text == null || text.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "文本不能为空"));
            }
            var embedding = embeddingService.getEmbedding(text);
            return ResponseEntity.ok(Map.of(
                    "text", text,
                    "embedding", embedding.vector(),
                    "dimension", embedding.vector().size()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "获取嵌入失败: " + e.getMessage()));
        }
    }

    // 批量获取文本的嵌入
    @PostMapping("/batch")
    public ResponseEntity<Map<String, Object>> getEmbeddings(@RequestBody Map<String, List<String>> request) {
        try {
            List<String> texts = request.get("texts");
            if (texts == null || texts.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "文本列表不能为空"));
            }
            var embeddings = embeddingService.getEmbeddings(texts);
            return ResponseEntity.ok(Map.of(
                    "texts", texts,
                    "embeddings", embeddings.stream().map(e -> e.vector()).toList(),
                    "count", embeddings.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "批量获取嵌入失败: " + e.getMessage()));
        }
    }

    // 计算两个文本的相似度
    @PostMapping("/similarity")
    public ResponseEntity<Map<String, Object>> calculateSimilarity(@RequestBody Map<String, String> request) {
        try {
            String text1 = request.get("text1");
            String text2 = request.get("text2");
            if (text1 == null || text1.isEmpty() || text2 == null || text2.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "两个文本都不能为空"));
            }
            double similarity = embeddingService.calculateSimilarity(text1, text2);
            return ResponseEntity.ok(Map.of(
                    "text1", text1,
                    "text2", text2,
                    "similarity", similarity
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "计算相似度失败: " + e.getMessage()));
        }
    }

    // 查找最相似的文本
    @PostMapping("/most-similar")
    public ResponseEntity<Map<String, Object>> findMostSimilar(@RequestBody Map<String, Object> request) {
        try {
            String query = (String) request.get("query");
            @SuppressWarnings("unchecked")
            List<String> texts = (List<String>) request.get("texts");
            if (query == null || query.isEmpty() || texts == null || texts.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "查询文本和文本列表都不能为空"));
            }
            String mostSimilar = embeddingService.findMostSimilar(query, texts);
            double similarity = embeddingService.calculateSimilarity(query, mostSimilar);
            return ResponseEntity.ok(Map.of(
                    "query", query,
                    "mostSimilar", mostSimilar,
                    "similarity", similarity
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "查找最相似文本失败: " + e.getMessage()));
        }
    }

    // 查找相似度高于阈值的文本
    @PostMapping("/similar")
    public ResponseEntity<Map<String, Object>> findSimilar(@RequestBody Map<String, Object> request) {
        try {
            String query = (String) request.get("query");
            @SuppressWarnings("unchecked")
            List<String> texts = (List<String>) request.get("texts");
            double threshold = request.get("threshold") != null ? 
                    Double.parseDouble(request.get("threshold").toString()) : 0.7;
            if (query == null || query.isEmpty() || texts == null || texts.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "查询文本和文本列表都不能为空"));
            }
            var similarTexts = embeddingService.findSimilar(query, texts, threshold);
            return ResponseEntity.ok(Map.of(
                    "query", query,
                    "similarTexts", similarTexts.stream()
                            .map(entry -> Map.of(
                                    "text", entry.getKey(),
                                    "similarity", entry.getValue()
                            ))
                            .toList(),
                    "count", similarTexts.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "查找相似文本失败: " + e.getMessage()));
        }
    }

    // 健康检查
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        try {
            boolean healthy = embeddingService.isHealthy();
            return ResponseEntity.ok(Map.of(
                    "status", healthy ? "healthy" : "unhealthy",
                    "timestamp", System.currentTimeMillis()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "健康检查失败: " + e.getMessage()));
        }
    }

    // 获取缓存统计信息
    @GetMapping("/cache/stats")
    public ResponseEntity<Map<String, Object>> getCacheStats() {
        try {
            Map<String, Object> stats = embeddingService.getCacheStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "获取缓存统计信息失败: " + e.getMessage()));
        }
    }
}
