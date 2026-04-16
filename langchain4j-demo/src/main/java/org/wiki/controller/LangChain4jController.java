package org.wiki.controller;

import org.wiki.service.LangChain4jService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/langchain4j")
@CrossOrigin(origins = "*")
public class LangChain4jController {

    private final LangChain4jService langChain4jService;

    @Autowired
    public LangChain4jController(LangChain4jService langChain4jService) {
        this.langChain4jService = langChain4jService;
    }

    // 简单聊天
    @PostMapping("/chat")
    public Map<String, String> chat(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        String response = langChain4jService.chat(message);
        return Map.of("response", response);
    }

    // 带记忆的聊天
    @PostMapping("/chat/memory")
    public Map<String, String> chatWithMemory(@RequestBody Map<String, String> request) {
        String sessionId = request.get("sessionId");
        String message = request.get("message");
        
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = langChain4jService.createSession();
        }
        
        String response = langChain4jService.chatWithMemory(sessionId, message);
        return Map.of("response", response, "sessionId", sessionId);
    }

    // 清除记忆
    @PostMapping("/chat/clear")
    public Map<String, String> clearMemory(@RequestBody Map<String, String> request) {
        String sessionId = request.get("sessionId");
        langChain4jService.clearMemory(sessionId);
        return Map.of("message", "Memory cleared successfully");
    }

    // 创建新会话
    @GetMapping("/session")
    public Map<String, String> createSession() {
        String sessionId = langChain4jService.createSession();
        return Map.of("sessionId", sessionId);
    }

    // 添加文档
    @PostMapping("/documents")
    public Map<String, String> addDocument(@RequestBody Map<String, String> request) {
        String content = request.get("content");
        langChain4jService.addDocument(content);
        return Map.of("message", "Document added successfully");
    }

    // 基于文档的问答
    @PostMapping("/chat/documents")
    public Map<String, String> chatWithDocuments(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        String response = langChain4jService.chatWithDocuments(message);
        return Map.of("response", response);
    }

    // 文本分析
    @PostMapping("/analyze")
    public Map<String, String> analyzeText(@RequestBody Map<String, String> request) {
        String text = request.get("text");
        String analysisType = request.get("type");
        String response = langChain4jService.analyzeText(text, analysisType);
        return Map.of("response", response);
    }

    // 文本摘要
    @PostMapping("/summarize")
    public Map<String, String> summarize(@RequestBody Map<String, Object> request) {
        String text = (String) request.get("text");
        int maxLength = request.get("maxLength") != null ? 
            Integer.parseInt(request.get("maxLength").toString()) : 100;
        String response = langChain4jService.summarize(text, maxLength);
        return Map.of("response", response);
    }

    // 文本翻译
    @PostMapping("/translate")
    public Map<String, String> translate(@RequestBody Map<String, String> request) {
        String text = request.get("text");
        String targetLanguage = request.get("language");
        String response = langChain4jService.translate(text, targetLanguage);
        return Map.of("response", response);
    }

    // 代码生成
    @PostMapping("/generate-code")
    public Map<String, String> generateCode(@RequestBody Map<String, String> request) {
        String description = request.get("description");
        String language = request.get("language");
        String response = langChain4jService.generateCode(description, language);
        return Map.of("response", response);
    }

    // 使用 Prompt Template
    @PostMapping("/template")
    public Map<String, String> generateWithTemplate(@RequestBody Map<String, Object> request) {
        String template = (String) request.get("template");
        @SuppressWarnings("unchecked")
        Map<String, Object> variables = (Map<String, Object>) request.get("variables");
        String response = langChain4jService.generateWithTemplate(template, variables);
        return Map.of("response", response);
    }
}
