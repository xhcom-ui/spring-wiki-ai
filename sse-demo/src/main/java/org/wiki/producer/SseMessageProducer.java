package org.wiki.producer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.wiki.config.SseRedisConfig;
import org.wiki.entity.SseMessage;
import org.wiki.tools.JsonUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@Slf4j
public class SseMessageProducer {

    private final StringRedisTemplate stringRedisTemplate;

    public SseMessageProducer(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public void sendToUser(SseMessage message) {
        String payload = JsonUtils.toJson(message);
        stringRedisTemplate.convertAndSend(SseRedisConfig.SSE_TOPIC, payload);
    }

    public void sendToUsers(List<SseMessage> messages) {
        if (messages == null) {
            return;
        }
        for (SseMessage message : messages) {
            sendToUser(message);
        }
    }

    public void broadcastSystemNotice(String noticeContent) {
        Set<String> onlineUsers = getOnlineUsers();
        for (String userId : onlineUsers) {
            SseMessage message = new SseMessage();
            message.setUserId(userId);
            message.setType(SseMessage.MessageType.SYSTEM_NOTICE);
            message.setContent(noticeContent);
            sendToUser(message);
        }
    }

    private Set<String> getOnlineUsers() {
        return new HashSet<>();
    }
}
