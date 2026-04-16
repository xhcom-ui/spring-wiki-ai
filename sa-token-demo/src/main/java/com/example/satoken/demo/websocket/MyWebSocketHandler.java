package com.example.satoken.demo.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class MyWebSocketHandler extends TextWebSocketHandler {

    // 存储所有活跃的 WebSocket 会话
    private static final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 从会话属性中获取登录用户 ID
        Object loginId = session.getAttributes().get("loginId");
        if (loginId != null) {
            String userId = loginId.toString();
            sessions.put(userId, session);
            log.info("WebSocket 连接建立，用户 ID: {}", userId);
            // 发送欢迎消息
            session.sendMessage(new TextMessage("{\"code\":200,\"msg\":\"连接成功\",\"data\":{\"userId\":\"" + userId + "\"}}"));
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 从会话属性中获取登录用户 ID
        Object loginId = session.getAttributes().get("loginId");
        if (loginId != null) {
            String userId = loginId.toString();
            log.info("收到用户 {} 的消息: {}", userId, message.getPayload());
            // 回复消息
            session.sendMessage(new TextMessage("{\"code\":200,\"msg\":\"消息已收到\",\"data\":{\"message\":\"" + message.getPayload() + "\"}}"));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        // 从会话属性中获取登录用户 ID
        Object loginId = session.getAttributes().get("loginId");
        if (loginId != null) {
            String userId = loginId.toString();
            sessions.remove(userId);
            log.info("WebSocket 连接关闭，用户 ID: {}", userId);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket 传输错误", exception);
    }

    // 向指定用户发送消息
    public void sendMessage(String userId, String message) {
        WebSocketSession session = sessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(message));
            } catch (IOException e) {
                log.error("发送消息失败", e);
            }
        }
    }

    // 向所有用户发送消息
    public void sendMessageToAll(String message) {
        for (WebSocketSession session : sessions.values()) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(message));
                } catch (IOException e) {
                    log.error("发送消息失败", e);
                }
            }
        }
    }
}
