package org.wiki.manager;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.wiki.entity.SseMessage;
import org.wiki.tools.JsonUtils;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class SseEmitterManager {

    private final long timeout;
    private final String heartbeatMessage;
    private final Map<String, Map<String, SseEmitter>> emitterMap = new ConcurrentHashMap<>();

    public SseEmitterManager(
            @Value("${app.sse.timeout:30000}") long timeout,
            @Value("${app.sse.heartbeat-message:ping}") String heartbeatMessage
    ) {
        this.timeout = timeout;
        this.heartbeatMessage = heartbeatMessage;
    }

    public SseEmitter createConnection(String userId) {
        String connectionId = UUID.randomUUID().toString();
        SseEmitter emitter = new SseEmitter(timeout);
        emitterMap.computeIfAbsent(userId, key -> new ConcurrentHashMap<>()).put(connectionId, emitter);

        emitter.onCompletion(() -> cleanup(userId, connectionId, "completion"));
        emitter.onTimeout(() -> cleanup(userId, connectionId, "timeout"));
        emitter.onError(ex -> cleanup(userId, connectionId, "error"));

        try {
            emitter.send(SseEmitter.event()
                    .id(connectionId)
                    .name("connected")
                    .data(Map.of(
                            "userId", userId,
                            "connectionId", connectionId,
                            "heartbeat", heartbeatMessage
                    )));
        } catch (IOException e) {
            cleanup(userId, connectionId, "init-failed");
            throw new IllegalStateException("初始化 SSE 连接失败", e);
        }

        return emitter;
    }

    public int pushToUser(String userId, SseMessage message) {
        Map<String, SseEmitter> connections = emitterMap.get(userId);
        if (connections == null || connections.isEmpty()) {
            log.warn("用户未连接: userId={}", userId);
            return 0;
        }

        int delivered = 0;
        String eventName = resolveEventName(message);
        String payload = JsonUtils.toJson(message);

        for (Map.Entry<String, SseEmitter> entry : connections.entrySet()) {
            try {
                entry.getValue().send(SseEmitter.event()
                        .id(message.getMessageId())
                        .name(eventName)
                        .data(payload));
                delivered++;
            } catch (IOException e) {
                log.warn("推送失败，清理连接: userId={}, connectionId={}", userId, entry.getKey(), e);
                cleanup(userId, entry.getKey(), "push-failed");
            }
        }
        return delivered;
    }

    public int countConnections(String userId) {
        Map<String, SseEmitter> connections = emitterMap.get(userId);
        return connections == null ? 0 : connections.size();
    }

    private String resolveEventName(SseMessage message) {
        if (message == null || message.getType() == null) {
            return "message";
        }
        return message.getType().eventName();
    }

    private void cleanup(String userId, String connectionId, String reason) {
        Map<String, SseEmitter> connections = emitterMap.get(userId);
        if (connections == null) {
            return;
        }
        connections.remove(connectionId);
        if (connections.isEmpty()) {
            emitterMap.remove(userId);
        }
        if (StringUtils.hasText(reason)) {
            log.info("SSE 连接释放: userId={}, connectionId={}, reason={}", userId, connectionId, reason);
        }
    }
}
