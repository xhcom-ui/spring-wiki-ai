package org.wiki.controller;

import org.wiki.entity.KnowledgeBase;
import org.wiki.service.KnowledgeBaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/knowledge")
@CrossOrigin(origins = "*")
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    @Autowired
    public KnowledgeBaseController(KnowledgeBaseService knowledgeBaseService) {
        this.knowledgeBaseService = knowledgeBaseService;
    }

    // 上传文件
    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("serviceUrl") String serviceUrl) {
        try {
            KnowledgeBase knowledgeBase = knowledgeBaseService.uploadFile(file, serviceUrl);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "文件上传成功");
            response.put("data", knowledgeBase);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "文件上传失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // 获取所有知识库
    @GetMapping("/list")
    public ResponseEntity<?> getAllKnowledgeBases() {
        List<KnowledgeBase> knowledgeBases = knowledgeBaseService.getAllKnowledgeBases();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", knowledgeBases);
        return ResponseEntity.ok(response);
    }

    // 根据ID获取知识库
    @GetMapping("/{id}")
    public ResponseEntity<?> getKnowledgeBaseById(@PathVariable Long id) {
        KnowledgeBase knowledgeBase = knowledgeBaseService.getKnowledgeBaseById(id);
        if (knowledgeBase == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "知识库不存在");
            return ResponseEntity.notFound().build();
        }
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", knowledgeBase);
        return ResponseEntity.ok(response);
    }

    // 删除知识库
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteKnowledgeBase(@PathVariable Long id) {
        try {
            knowledgeBaseService.deleteKnowledgeBase(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "删除成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "删除失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // 搜索知识库
    @GetMapping("/search")
    public ResponseEntity<?> searchByFileName(@RequestParam("fileName") String fileName) {
        List<KnowledgeBase> knowledgeBases = knowledgeBaseService.searchByFileName(fileName);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", knowledgeBases);
        return ResponseEntity.ok(response);
    }

    // 搜索相似内容
    @PostMapping("/search-similar")
    public ResponseEntity<?> searchSimilarContent(
            @RequestBody Map<String, Object> request) {
        String query = (String) request.get("query");
        int maxResults = request.get("maxResults") != null ? 
                Integer.parseInt(request.get("maxResults").toString()) : 5;
        
        List<Map<String, Object>> results = knowledgeBaseService.searchSimilarContent(query, maxResults);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", results);
        return ResponseEntity.ok(response);
    }
}
