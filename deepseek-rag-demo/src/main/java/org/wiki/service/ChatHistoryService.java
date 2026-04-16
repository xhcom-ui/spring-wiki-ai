package org.wiki.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.wiki.model.ChatMessage;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 对话历史服务
 * 基于内存存储对话历史（生产环境建议使用数据库持久化）
 */
@Slf4j
@Service
public class ChatHistoryService {

    /**
     * 会话消息存储: sessionId -> 消息列表
     */
    private final Map<String, List<ChatMessage>> sessionMessages = new ConcurrentHashMap<>();

    /**
     * 最大存储消息数量
     */
    private static final int MAX_MESSAGES_PER_SESSION = 100;

    /**
     * 添加消息到会话
     */
    public void addMessage(ChatMessage message) {
        sessionMessages.computeIfAbsent(message.getSessionId(), k -> new ArrayList<>())
                .add(message);

        // 限制消息数量
        List<ChatMessage> messages = sessionMessages.get(message.getSessionId());
        if (messages.size() > MAX_MESSAGES_PER_SESSION) {
            messages.subList(0, messages.size() - MAX_MESSAGES_PER_SESSION).clear();
        }

        log.debug("消息已存储: sessionId={}, role={}, contentLength={}",
                message.getSessionId(), message.getRole(), message.getContent().length());
    }

    /**
     * 获取会话的所有消息
     */
    public List<ChatMessage> getMessages(String sessionId) {
        return sessionMessages.getOrDefault(sessionId, Collections.emptyList());
    }

    /**
     * 获取会话最近的N条消息
     */
    public List<ChatMessage> getRecentMessages(String sessionId, int limit) {
        List<ChatMessage> messages = sessionMessages.getOrDefault(sessionId, Collections.emptyList());
        if (messages.size() <= limit) {
            return new ArrayList<>(messages);
        }
        return new ArrayList<>(messages.subList(messages.size() - limit, messages.size()));
    }

    /**
     * 清空会话消息
     */
    public void clearSession(String sessionId) {
        sessionMessages.remove(sessionId);
        log.info("会话已清空: sessionId={}", sessionId);
    }

    /**
     * 获取所有会话ID
     */
    public Set<String> getSessionIds() {
        return sessionMessages.keySet();
    }

    /**
     * 创建新会话
     */
    public String createSession() {
        String sessionId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        sessionMessages.put(sessionId, new ArrayList<>());
        log.info("创建新会话: sessionId={}", sessionId);
        return sessionId;
    }
}
