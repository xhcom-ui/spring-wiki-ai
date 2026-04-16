package org.wiki.controller;

import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.wiki.entity.ApiResponse;
import org.wiki.entity.BatchResult;
import org.wiki.entity.SseMessage;
import org.wiki.producer.SseMessageProducer;
import org.wiki.tools.JsonUtils;

import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/api/sse")
@Slf4j
public class SseApiController {
    
    @Autowired
    private SseMessageProducer sseMessageProducer;
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    /**
     * 通用消息发送接口
     * 供 order、apm 等服务调用
     */
    @PostMapping("/send")
    public ApiResponse<Boolean> sendMessage(@RequestBody SseMessage message) {
        try {
            // 1. 参数校验
            if (StringUtils.isBlank(message.getUserId())) {
                return ApiResponse.error("用户ID不能为空");
            }
            if (StringUtils.isBlank(message.getContent())) {
                return ApiResponse.error("消息内容不能为空");
            }
            
            // 2. 幂等性检查（防止重复发送）
            String redisKey = "sse:message:" + message.getMessageId();
            Boolean isAbsent = redisTemplate.opsForValue().setIfAbsent(
                redisKey, "1", Duration.ofMinutes(5));
            if (Boolean.FALSE.equals(isAbsent)) {
                log.warn("消息重复发送，已忽略: messageId={}", message.getMessageId());
                return ApiResponse.success(false, "消息已发送");
            }
            
            // 3. 序列化并发送到RabbitMQ
            String jsonMessage = JsonUtils.toJson(message);
            sseMessageProducer.sendToUser(message.getUserId(), jsonMessage);
            
            log.info("消息发送成功: userId={}, type={}", 
                message.getUserId(), message.getType());
            
            return ApiResponse.success(true);
            
        } catch (Exception e) {
            log.error("消息发送失败", e);
            return ApiResponse.error("消息发送失败: " + e.getMessage());
        }
    }
    
    /**
     * 批量发送接口（适合运营活动等场景）
     */
    @PostMapping("/send/batch")
    public ApiResponse<BatchResult> sendBatch(@RequestBody List<SseMessage> messages) {
        BatchResult result = new BatchResult();
        
        for (SseMessage message : messages) {
            try {
                // 调用单条发送逻辑
                ApiResponse<Boolean> response = sendMessage(message);
                if (response.isSuccess()) {
                    result.addSuccess(message.getMessageId());
                } else {
                    result.addFailure(message.getMessageId(), response.getMsg());
                }
            } catch (Exception e) {
                result.addFailure(message.getMessageId(), e.getMessage());
            }
        }
        
        return ApiResponse.success(result);
    }
}