package org.wiki.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.wiki.model.ChatMessage;
import org.wiki.service.ChatHistoryService;
import org.wiki.service.DeepSeekChatService;
import org.wiki.service.RagService;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 对话 Controller
 * 支持三种对话模式：
 * 1. RAG 知识库问答：自实现 RAG 流程（检索 + 生成）
 * 2. DeepSeek 直接对话：直接调用 DeepSeek API
 * 3. RAG + 流式：RAG 检索 + 流式生成
 */
@Slf4j
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final RagService ragService;
    private final DeepSeekChatService deepSeekChatService;
    private final ChatHistoryService chatHistoryService;
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    public ChatController(RagService ragService, DeepSeekChatService deepSeekChatService,
                          ChatHistoryService chatHistoryService) {
        this.ragService = ragService;
        this.deepSeekChatService = deepSeekChatService;
        this.chatHistoryService = chatHistoryService;
    }

    /**
     * RAG 知识库问答（非流式）
     * POST /api/chat/rag
     */
    @PostMapping("/rag")
    public Map<String, Object> ragChat(@RequestParam String question,
                                       @RequestParam(required = false) String sessionId) {
        Map<String, Object> result = new HashMap<>();
        try {
            String sid = sessionId != null ? sessionId : chatHistoryService.createSession();
            chatHistoryService.addMessage(ChatMessage.userMessage(sid, question, "rag"));

            String answer = ragService.askQuestion(question);
            chatHistoryService.addMessage(ChatMessage.assistantMessage(sid, answer, "rag"));

            result.put("success", true);
            result.put("answer", answer);
            result.put("sessionId", sid);
        } catch (Exception e) {
            log.error("RAG 对话失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * RAG 知识库问答（流式 SSE）
     * GET /api/chat/rag/stream?question=xxx
     */
    @GetMapping(value = "/rag/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> ragChatStream(@RequestParam String question) {
        log.info("RAG 流式对话请求: question={}", question);
        return ragService.askQuestionStream(question)
                .concatWith(Flux.just("[DONE]"));
    }

    /**
     * DeepSeek 直接对话（非流式）
     * POST /api/chat/deepseek
     */
    @PostMapping("/deepseek")
    public Map<String, Object> deepseekChat(@RequestParam String question,
                                            @RequestParam(required = false) String sessionId) {
        Map<String, Object> result = new HashMap<>();
        try {
            String sid = sessionId != null ? sessionId : chatHistoryService.createSession();
            chatHistoryService.addMessage(ChatMessage.userMessage(sid, question, "deepseek"));

            String answer = deepSeekChatService.chat(question);
            chatHistoryService.addMessage(ChatMessage.assistantMessage(sid, answer, "deepseek"));

            result.put("success", true);
            result.put("answer", answer);
            result.put("sessionId", sid);
        } catch (Exception e) {
            log.error("DeepSeek 对话失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * DeepSeek 直接对话（流式 SSE）
     * GET /api/chat/deepseek/stream?question=xxx
     */
    @GetMapping(value = "/deepseek/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> deepseekChatStream(@RequestParam String question) {
        log.info("DeepSeek 流式对话请求: question={}", question);
        return deepSeekChatService.chatStream(question)
                .concatWith(Flux.just("[DONE]"));
    }

    /**
     * 仅检索相关文档（不调用LLM生成）
     * GET /api/chat/search?query=xxx
     */
    @GetMapping("/search")
    public Map<String, Object> searchDocuments(@RequestParam String query) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Document> docs = ragService.searchDocuments(query);
            result.put("success", true);
            result.put("count", docs.size());
            result.put("data", docs.stream().map(doc -> Map.of(
                    "content", doc.getText(),
                    "metadata", doc.getMetadata()
            )).toList());
        } catch (Exception e) {
            log.error("文档检索失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    // ==================== 对话历史 API ====================

    @PostMapping("/session")
    public Map<String, Object> createSession() {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("sessionId", chatHistoryService.createSession());
        return result;
    }

    @GetMapping("/history/{sessionId}")
    public Map<String, Object> getHistory(@PathVariable String sessionId) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", chatHistoryService.getMessages(sessionId));
        return result;
    }

    @DeleteMapping("/history/{sessionId}")
    public Map<String, Object> clearHistory(@PathVariable String sessionId) {
        chatHistoryService.clearSession(sessionId);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        return result;
    }
}
