package org.wiki.manager;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class SseEmitterManager {
    
    // 存储用户连接（生产环境可替换为 Redis）
    private final Map<String, SseEmitter> emitterMap = new ConcurrentHashMap<>();
    
    // 建立 SSE 连接（含心跳保活）
    public SseEmitter createConnection(String userId) {
        // 0L 表示永不超时（实际建议设 30s-60s）
        SseEmitter emitter = new SseEmitter(0L);
        emitterMap.put(userId, emitter);
        
        // 连接生命周期监听
        emitter.onCompletion(() -> {
            log.info("SSE 连接完成: userId={}", userId);
            emitterMap.remove(userId);
        });
        emitter.onTimeout(() -> {
            log.warn("SSE 连接超时: userId={}", userId);
            emitterMap.remove(userId);
        });
        emitter.onError((e) -> {
            log.error("SSE 连接错误: userId={}", userId, e);
            emitterMap.remove(userId);
        });
        
        // 发送初始心跳
        try {
            emitter.send(SseEmitter.event()
                .id("init")
                .name("connected")
                .data("SSE Connected"));
        } catch (IOException e) {
            log.error("初始消息发送失败", e);
        }
        
        return emitter;
    }
    
    // 推送消息给指定用户
    public void pushToUser(String userId, String message) {
        SseEmitter emitter = emitterMap.get(userId);
        if (emitter == null) {
            log.warn("用户未连接: userId={}", userId);
            return;
        }
        
        try {
            emitter.send(SseEmitter.event()
                .id(UUID.randomUUID().toString())
                .name("message")
                .data(message));
        } catch (IOException e) {
            log.error("推送失败，移除连接: userId={}", userId, e);
            emitterMap.remove(userId);
        }
    }
}