package org.wiki.controller;

import org.springframework.web.bind.annotation.*;
import org.wiki.service.RagService;

@RestController
@RequestMapping("/rag")
public class RagController {

    private final RagService ragService;

    public RagController(RagService ragService) {
        this.ragService = ragService;
    }

    /**
     * 上传本地文档（TXT/PDF/Word）
     * 示例：http://localhost:8080/rag/upload?filePath=D:/test.pdf
     */
    @GetMapping("/upload")
    public String upload(@RequestParam String filePath) {
        return ragService.uploadDocument(filePath);
    }

    /**
     * RAG 问答
     * 示例：http://localhost:8080/rag/ask?question=SpringAI 支持哪些向量库？
     */
    @GetMapping("/ask")
    public String ask(@RequestParam String question) {
        return ragService.askQuestion(question);
    }
}