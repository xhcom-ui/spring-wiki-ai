package org.wiki.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 对话消息模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    /**
     * 消息ID
     */
    private String id;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 角色：user / assistant
     */
    private String role;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 对话模式：rag / deepseek
     */
    private String mode;

    /**
     * 引用信息（RAG 检索到的知识库文档来源）
     */
    private String reference;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 创建用户消息
     */
    public static ChatMessage userMessage(String sessionId, String content, String mode) {
        return ChatMessage.builder()
                .id(java.util.UUID.randomUUID().toString())
                .sessionId(sessionId)
                .role("user")
                .content(content)
                .mode(mode)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * 创建助手消息
     */
    public static ChatMessage assistantMessage(String sessionId, String content, String mode) {
        return ChatMessage.builder()
                .id(java.util.UUID.randomUUID().toString())
                .sessionId(sessionId)
                .role("assistant")
                .content(content)
                .mode(mode)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
