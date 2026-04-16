package org.wiki.consumer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.wiki.manager.SseEmitterManager;

@Component
@Slf4j
public class SseMessageConsumer {
    
    @Autowired
    private SseEmitterManager sseEmitterManager;
    
    // 监听用户专属队列
    @RabbitListener(queues = "#{@userSseQueue}")
    public void handleSseMessage(String message, @Header("userId") String userId) {
        log.info("收到 SSE 消息: userId={}, message={}", userId, message);
        sseEmitterManager.pushToUser(userId, message);
    }
}