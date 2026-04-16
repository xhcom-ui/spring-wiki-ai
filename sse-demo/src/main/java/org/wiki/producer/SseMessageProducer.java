package org.wiki.producer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.wiki.config.SseRabbitConfig;
import org.wiki.entity.SseMessage;
import org.wiki.tools.JsonUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
@Slf4j
public class SseMessageProducer {
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    /**
     * 发送给单个用户
     */
    public void sendToUser(String userId, String message) {
        String routingKey = SseRabbitConfig.ROUTING_KEY_PREFIX + userId;
        rabbitTemplate.convertAndSend(
            SseRabbitConfig.SSE_EXCHANGE, 
            routingKey, 
            message, 
            m -> {
                m.getMessageProperties().setHeader("userId", userId);
                m.getMessageProperties().setMessageId(UUID.randomUUID().toString());
                return m;
            }
        );
    }
    
    /**
     * 发送给多个用户（广播模式）
     */
    public void sendToUsers(List<String> userIds, String message) {
        for (String userId : userIds) {
            sendToUser(userId, message);
        }
    }
    
    /**
     * 发送系统公告（给所有在线用户）
     * 注意：需要结合在线用户列表实现
     */
    public void broadcastSystemNotice(String noticeContent) {
        // 获取所有在线用户（可从Redis获取）
        Set<String> onlineUsers = getOnlineUsers();
        
        SseMessage message = new SseMessage();
        message.setType(SseMessage.MessageType.SYSTEM_NOTICE);
        message.setContent(noticeContent);
        
        for (String userId : onlineUsers) {
            message.setUserId(userId);
            String jsonMessage = JsonUtils.toJson(message);
            sendToUser(userId, jsonMessage);
        }
    }
    
    private Set<String> getOnlineUsers() {
        // 实际应从Redis中获取在线用户列表
        return new HashSet<>();
    }
}