package org.wiki.controller;

import org.wiki.service.McpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/mcp")
@CrossOrigin(origins = "*") // 允许跨域请求，便于前端访问
public class McpController {
    private final McpService mcpService;

    @Autowired
    public McpController(McpService mcpService) {
        this.mcpService = mcpService;
    }

    // 创建模型会话
    @PostMapping("/session")
    public Map<String, Object> createSession(@RequestBody Map<String, Object> request) {
        try {
            String model = (String) request.get("model");
            return mcpService.createSession(model);
        } catch (IOException e) {
            e.printStackTrace();
            return Map.of("error", "Failed to create session: " + e.getMessage());
        }
    }

    // 发送消息到模型
    @PostMapping("/message")
    public Map<String, Object> sendMessage(@RequestBody Map<String, Object> request) {
        try {
            String sessionId = (String) request.get("session_id");
            String message = (String) request.get("message");
            return mcpService.sendMessage(sessionId, message);
        } catch (IOException e) {
            e.printStackTrace();
            return Map.of("error", "Failed to send message: " + e.getMessage());
        }
    }

    // 获取模型响应
    @PostMapping("/response")
    public Map<String, Object> getResponse(@RequestBody Map<String, Object> request) {
        try {
            String sessionId = (String) request.get("session_id");
            return mcpService.getResponse(sessionId);
        } catch (IOException e) {
            e.printStackTrace();
            return Map.of("error", "Failed to get response: " + e.getMessage());
        }
    }

    // 关闭模型会话
    @DeleteMapping("/session")
    public Map<String, Object> closeSession(@RequestBody Map<String, Object> request) {
        try {
            String sessionId = (String) request.get("session_id");
            return mcpService.closeSession(sessionId);
        } catch (IOException e) {
            e.printStackTrace();
            return Map.of("error", "Failed to close session: " + e.getMessage());
        }
    }

    // 获取模型列表
    @GetMapping("/models")
    public Map<String, Object> getModels() {
        try {
            return mcpService.getModels();
        } catch (IOException e) {
            e.printStackTrace();
            return Map.of("error", "Failed to get models: " + e.getMessage());
        }
    }
}
