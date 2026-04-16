package org.wiki.controller;

import org.wiki.entity.Prompt;
import org.wiki.service.PromptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/prompts")
@CrossOrigin(origins = "*") // 允许跨域请求，便于前端访问
public class PromptController {
    @Autowired
    private PromptService promptService;

    // 创建 Prompt 模板
    @PostMapping
    public Prompt createPrompt(@RequestBody Prompt prompt) {
        return promptService.createPrompt(prompt);
    }

    // 更新 Prompt 模板
    @PutMapping("/{id}")
    public Prompt updatePrompt(@PathVariable Long id, @RequestBody Prompt prompt) {
        return promptService.updatePrompt(id, prompt);
    }

    // 删除 Prompt 模板
    @DeleteMapping("/{id}")
    public void deletePrompt(@PathVariable Long id) {
        promptService.deletePrompt(id);
    }

    // 获取所有 Prompt 模板
    @GetMapping
    public List<Prompt> getAllPrompts() {
        return promptService.getAllPrompts();
    }

    // 根据 ID 获取 Prompt 模板
    @GetMapping("/{id}")
    public Prompt getPromptById(@PathVariable Long id) {
        return promptService.getPromptById(id);
    }

    // 根据名称获取 Prompt 模板
    @GetMapping("/name/{name}")
    public Prompt getPromptByName(@PathVariable String name) {
        return promptService.getPromptByName(name);
    }

    // 生成 Prompt 文本
    @PostMapping("/generate")
    public Map<String, String> generatePromptText(@RequestBody Map<String, Object> request) {
        String content = (String) request.get("content");
        List<Object> variables = (List<Object>) request.get("variables");
        String generatedText = promptService.generatePromptText(content, variables.toArray());
        return Map.of("generatedText", generatedText);
    }
}
