package org.wiki;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * DeepSeek + RAG 知识库问答系统
 * 自实现 RAG 全流程：文档解析 → 切分 → 向量化 → 检索 → 生成
 */
@SpringBootApplication
public class DeepSeekRagApplication {
    public static void main(String[] args) {
        SpringApplication.run(DeepSeekRagApplication.class, args);
    }
}
