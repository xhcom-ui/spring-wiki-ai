package com.xxx.rag.rag.prompt;

import org.springframework.ai.document.Document;

import java.util.List;

/**
 * Prompt 模板管理
 */
public class PromptManager {

    /**
     * 构建 RAG 提示
     */
    public static String buildRagPrompt(String query, List<Document> documents) {
        StringBuilder prompt = new StringBuilder();

        // 系统提示
        prompt.append("你是一个基于知识库的问答助手，需要根据提供的文档内容回答用户的问题。\n");
        prompt.append("请严格基于文档内容回答，不要添加文档中没有的信息。\n");
        prompt.append("如果文档中没有相关信息，请明确表示无法回答。\n\n");

        // 文档内容
        prompt.append("相关文档内容：\n");
        for (int i = 0; i < documents.size(); i++) {
            prompt.append("【文档").append(i + 1).append("】\n");
            prompt.append(documents.get(i).getContent()).append("\n\n");
        }

        // 用户查询
        prompt.append("用户问题：\n").append(query).append("\n");
        prompt.append("请基于上述文档内容回答用户的问题：\n");

        return prompt.toString();
    }

    /**
     * 构建重排序提示
     */
    public static String buildRerankPrompt(String query, List<Document> documents) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请根据以下查询对文档片段进行相关性排序，返回排序后的索引（从0开始）：\n");
        prompt.append("查询: " + query + "\n");
        prompt.append("文档片段:\n");

        for (int i = 0; i < documents.size(); i++) {
            prompt.append(i).append(": ").append(documents.get(i).getContent()).append("\n");
        }

        prompt.append("\n请返回排序后的索引，用逗号分隔，例如：2,0,1");

        return prompt.toString();
    }
}
