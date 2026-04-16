package com.example.deepresearch.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class EmbeddingService {

    @Value("${huggingface.api-key}")
    private String huggingFaceApiKey;

    @Value("${huggingface.embedding-model}")
    private String embeddingModel;

    public List<Double> getEmbedding(String text) {
        // 这里应该调用 Hugging Face API 获取文本嵌入
        // 由于是示例，这里返回一个模拟的嵌入向量
        log.info("获取文本嵌入: {}", text);
        // 模拟嵌入向量，实际应用中应该调用真实的 API
        return List.of(0.1, 0.2, 0.3, 0.4, 0.5);
    }
}
