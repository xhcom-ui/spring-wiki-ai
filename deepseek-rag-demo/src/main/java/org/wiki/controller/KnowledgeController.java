package org.wiki.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.wiki.model.KnowledgeDoc;
import org.wiki.service.RagService;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识库管理 Controller
 * 自实现文档管理：上传/列表/删除
 */
@Slf4j
@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {

    private final RagService ragService;

    public KnowledgeController(RagService ragService) {
        this.ragService = ragService;
    }

    /**
     * 上传文档到知识库
     * POST /api/knowledge/upload
     */
    @PostMapping("/upload")
    public Map<String, Object> uploadDocument(@RequestParam("file") MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        try {
            KnowledgeDoc doc = ragService.uploadDocument(file);
            result.put("success", true);
            result.put("data", doc);
            result.put("message", "文档上传并索引成功，共 " + doc.getChunkCount() + " 个文档片段");
        } catch (IOException e) {
            log.error("上传文档失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 上传本地文件路径的文档
     * POST /api/knowledge/upload-local?filePath=xxx
     */
    @PostMapping("/upload-local")
    public Map<String, Object> uploadLocalFile(@RequestParam String filePath) {
        Map<String, Object> result = new HashMap<>();
        try {
            KnowledgeDoc doc = ragService.uploadLocalFile(filePath);
            result.put("success", true);
            result.put("data", doc);
            result.put("message", "文档上传并索引成功，共 " + doc.getChunkCount() + " 个文档片段");
        } catch (IOException e) {
            log.error("上传本地文件失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 获取知识库文档列表
     * GET /api/knowledge/documents
     */
    @GetMapping("/documents")
    public Map<String, Object> listDocuments() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<KnowledgeDoc> docs = ragService.listDocuments();
            result.put("success", true);
            result.put("data", docs);
            result.put("count", docs.size());
        } catch (Exception e) {
            log.error("获取文档列表失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 删除文档
     * DELETE /api/knowledge/documents/{docId}
     */
    @DeleteMapping("/documents/{docId}")
    public Map<String, Object> deleteDocument(@PathVariable String docId) {
        Map<String, Object> result = new HashMap<>();
        try {
            ragService.deleteDocument(docId);
            result.put("success", true);
            result.put("message", "文档已删除");
        } catch (Exception e) {
            log.error("删除文档失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
}
