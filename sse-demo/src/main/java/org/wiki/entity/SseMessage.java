package org.wiki.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SseMessage {
    // 必填：目标用户ID
    private String userId;
    
    // 消息类型（用于前端区分处理）
    private MessageType type;
    
    // 消息内容（建议JSON格式）
    private String content;
    
    // 消息ID（用于幂等性处理）
    private String messageId = UUID.randomUUID().toString();
    
    // 消息时间戳
    private Long timestamp = System.currentTimeMillis();
    
    public enum MessageType {
        ORDER_UPDATE,     // 订单更新
        APM_ALERT,        // 监控告警
        APPROVAL_NOTIFY,  // 审批通知
        SYSTEM_NOTICE,    // 系统公告
        CUSTOM            // 自定义消息
    }
}