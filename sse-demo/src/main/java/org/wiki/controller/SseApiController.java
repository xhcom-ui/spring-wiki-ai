package org.wiki.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.wiki.entity.ApiResponse;
import org.wiki.entity.BatchResult;
import org.wiki.entity.SseMessage;
import org.wiki.producer.SseMessageProducer;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/sse")
@Slf4j
public class SseApiController {

    private final SseMessageProducer sseMessageProducer;
    private final RedisTemplate<String, String> redisTemplate;
    private final boolean idempotentEnabled;

    public SseApiController(
            SseMessageProducer sseMessageProducer,
            RedisTemplate<String, String> redisTemplate,
            @Value("${app.idempotent.enabled:true}") boolean idempotentEnabled
    ) {
        this.sseMessageProducer = sseMessageProducer;
        this.redisTemplate = redisTemplate;
        this.idempotentEnabled = idempotentEnabled;
    }

    @PostMapping("/send")
    public ApiResponse<Boolean> sendMessage(@RequestBody SseMessage message) {
        try {
            normalizeMessage(message);
            String validationError = validateMessage(message);
            if (validationError != null) {
                return ApiResponse.paramError(validationError);
            }

            if (idempotentEnabled && isDuplicate(message.getMessageId())) {
                log.warn("消息重复发送，已忽略: messageId={}", message.getMessageId());
                return ApiResponse.success(false, "消息已发送");
            }

            sseMessageProducer.sendToUser(message);
            log.info("消息发送成功: userId={}, type={}, messageId={}",
                    message.getUserId(), message.getType(), message.getMessageId());
            return ApiResponse.success(true);
        } catch (Exception e) {
            log.error("消息发送失败", e);
            return ApiResponse.error("消息发送失败: " + e.getMessage());
        }
    }

    @PostMapping("/send/batch")
    public ApiResponse<BatchResult> sendBatch(@RequestBody List<SseMessage> messages) {
        if (CollectionUtils.isEmpty(messages)) {
            return ApiResponse.paramError("消息列表不能为空");
        }

        BatchResult result = new BatchResult();
        for (SseMessage message : messages) {
            String messageId = message == null ? UUID.randomUUID().toString() : message.getMessageId();
            try {
                ApiResponse<Boolean> response = sendMessage(message);
                if (response.isSuccess() && Boolean.TRUE.equals(response.getData())) {
                    result.addSuccess(messageId);
                } else {
                    result.addFailure(messageId, response.getMsg());
                }
            } catch (Exception e) {
                result.addFailure(messageId, e.getMessage());
            }
        }
        return ApiResponse.success(result);
    }

    private void normalizeMessage(SseMessage message) {
        if (message == null) {
            throw new IllegalArgumentException("消息体不能为空");
        }
        if (StringUtils.hasText(message.getUserId())) {
            message.setUserId(message.getUserId().trim());
        }
        if (!StringUtils.hasText(message.getMessageId())) {
            message.setMessageId(UUID.randomUUID().toString());
        }
        if (message.getTimestamp() == null) {
            message.setTimestamp(System.currentTimeMillis());
        }
        if (message.getType() == null) {
            message.setType(SseMessage.MessageType.CUSTOM);
        }
    }

    private String validateMessage(SseMessage message) {
        if (!StringUtils.hasText(message.getUserId())) {
            return "用户ID不能为空";
        }
        if (!StringUtils.hasText(message.getContent())) {
            return "消息内容不能为空";
        }
        return null;
    }

    private boolean isDuplicate(String messageId) {
        String redisKey = "sse:message:" + messageId;
        Boolean isAbsent = redisTemplate.opsForValue().setIfAbsent(redisKey, "1", Duration.ofMinutes(5));
        return Boolean.FALSE.equals(isAbsent);
    }
}
