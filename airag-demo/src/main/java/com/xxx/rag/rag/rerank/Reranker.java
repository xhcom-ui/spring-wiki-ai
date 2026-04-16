package com.xxx.rag.rag.rerank;

import com.xxx.rag.common.exception.RagException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 重排序器
 */
public class Reranker {

    private final ChatClient chatClient;

    public Reranker(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /**
     * 重排序文档
     */
    public List<Document> rerank(String query, List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return documents;
        }

        try {
            // 构建重排序提示
            StringBuilder prompt = new StringBuilder();
            prompt.append("请根据以下查询对文档片段进行相关性排序，返回排序后的索引（从0开始）：\n");
            prompt.append("查询: " + query + "\n");
            prompt.append("文档片段:\n");

            for (int i = 0; i < documents.size(); i++) {
                prompt.append(i).append(": ").append(documents.get(i).getContent()).append("\n");
            }

            prompt.append("\n请返回排序后的索引，用逗号分隔，例如：2,0,1");

            // 调用模型进行重排序
            String response = chatClient.prompt()
                    .user(prompt.toString())
                    .call()
                    .content();

            // 解析排序结果
            List<Integer> rankedIndices = parseRanking(response, documents.size());

            // 按照排序结果重新组织文档
            return rankedIndices.stream()
                    .map(documents::get)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RagException("重排序失败: " + e.getMessage());
        }
    }

    /**
     * 解析排序结果
     */
    private List<Integer> parseRanking(String response, int size) {
        try {
            // 提取逗号分隔的索引
            String[] parts = response.replaceAll("\\s+", "").split(",");
            List<Integer> indices = new java.util.ArrayList<>();

            for (String part : parts) {
                try {
                    int index = Integer.parseInt(part);
                    if (index >= 0 && index < size && !indices.contains(index)) {
                        indices.add(index);
                    }
                } catch (NumberFormatException e) {
                    // 忽略无效索引
                }
            }

            // 确保所有文档都被排序
            for (int i = 0; i < size; i++) {
                if (!indices.contains(i)) {
                    indices.add(i);
                }
            }

            return indices;
        } catch (Exception e) {
            // 如果解析失败，返回原始顺序
            List<Integer> indices = new java.util.ArrayList<>();
            for (int i = 0; i < size; i++) {
                indices.add(i);
            }
            return indices;
        }
    }
}
