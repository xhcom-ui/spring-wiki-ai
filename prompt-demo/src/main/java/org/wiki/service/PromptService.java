package org.wiki.service;

import org.wiki.entity.Prompt;
import org.wiki.repository.PromptRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PromptService {
    @Autowired
    private PromptRepository promptRepository;

    // 创建 Prompt 模板
    public Prompt createPrompt(Prompt prompt) {
        prompt.setCreatedAt(LocalDateTime.now());
        prompt.setUpdatedAt(LocalDateTime.now());
        return promptRepository.save(prompt);
    }

    // 更新 Prompt 模板
    public Prompt updatePrompt(Long id, Prompt prompt) {
        Prompt existingPrompt = promptRepository.findById(id).orElseThrow();
        existingPrompt.setName(prompt.getName());
        existingPrompt.setContent(prompt.getContent());
        existingPrompt.setDescription(prompt.getDescription());
        existingPrompt.setUpdatedAt(LocalDateTime.now());
        return promptRepository.save(existingPrompt);
    }

    // 删除 Prompt 模板
    public void deletePrompt(Long id) {
        promptRepository.deleteById(id);
    }

    // 获取所有 Prompt 模板
    public List<Prompt> getAllPrompts() {
        return promptRepository.findAll();
    }

    // 根据 ID 获取 Prompt 模板
    public Prompt getPromptById(Long id) {
        return promptRepository.findById(id).orElseThrow();
    }

    // 根据名称获取 Prompt 模板
    public Prompt getPromptByName(String name) {
        return promptRepository.findByName(name);
    }

    // 生成完整的 Prompt 文本（替换变量）
    public String generatePromptText(String promptContent, Object... variables) {
        String result = promptContent;
        for (int i = 0; i < variables.length; i++) {
            result = result.replace("{" + i + "}", variables[i].toString());
        }
        return result;
    }
}
