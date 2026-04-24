package org.wiki.consumer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.wiki.entity.SseMessage;
import org.wiki.manager.SseEmitterManager;
import org.wiki.tools.JsonUtils;

@Component
@Slf4j
public class SseMessageConsumer {

    private final SseEmitterManager sseEmitterManager;

    public SseMessageConsumer(SseEmitterManager sseEmitterManager) {
        this.sseEmitterManager = sseEmitterManager;
    }

    public void handleMessage(String payload, String channel) {
        try {
            SseMessage message = JsonUtils.fromJson(payload, SseMessage.class);
            if (message == null || !StringUtils.hasText(message.getUserId())) {
                log.warn("收到无效 SSE 消息: channel={}, payload={}", channel, payload);
                return;
            }
            String userId = message.getUserId().trim();
            int delivered = sseEmitterManager.pushToUser(userId, message);
            log.info("收到 Redis SSE 消息: channel={}, userId={}, type={}, delivered={}",
                    channel, userId, message.getType(), delivered);
        } catch (Exception ex) {
            log.error("消费 Redis SSE 消息失败: channel={}, payload={}", channel, payload, ex);
        }
    }
}
