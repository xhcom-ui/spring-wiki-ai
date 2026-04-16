package org.wiki.controller;

import org.wiki.service.OllamaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/ollama")
@CrossOrigin(origins = "*") // 允许跨域请求，便于前端访问
public class OllamaController {
    private final OllamaService ollamaService;

    @Autowired
    public OllamaController(OllamaService ollamaService) {
        this.ollamaService = ollamaService;
    }

    // 生成文本
    @PostMapping("/generate")
    public Map<String, String> generateText(@RequestBody Map<String, Object> request) {
        try {
            String prompt = (String) request.get("prompt");
            String model = (String) request.get("model");
            String response = ollamaService.generateText(prompt, model);
            return Map.of("response", response);
        } catch (IOException e) {
            e.printStackTrace();
            return Map.of("error", "Failed to generate text: " + e.getMessage());
        }
    }

    // 聊天模式
    @PostMapping("/chat")
    public Map<String, String> chat(@RequestBody Map<String, Object> request) {
        try {
            String message = (String) request.get("message");
            String model = (String) request.get("model");
            String response = ollamaService.chat(message, model);
            return Map.of("response", response);
        } catch (IOException e) {
            e.printStackTrace();
            return Map.of("error", "Failed to chat: " + e.getMessage());
        }
    }

    // 获取模型列表
    @GetMapping("/models")
    public Map<String, Object> getModels() {
        try {
            return ollamaService.getModels();
        } catch (IOException e) {
            e.printStackTrace();
            return Map.of("error", "Failed to get models: " + e.getMessage());
        }
    }
}
