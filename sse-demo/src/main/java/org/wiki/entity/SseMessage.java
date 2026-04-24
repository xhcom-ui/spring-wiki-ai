package org.wiki.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SseMessage {
    private String userId;

    private MessageType type;

    private String content;

    private String messageId = UUID.randomUUID().toString();

    private Long timestamp = System.currentTimeMillis();

    public enum MessageType {
        ORDER_UPDATE,
        APM_ALERT,
        APPROVAL_NOTIFY,
        SYSTEM_NOTICE,
        CUSTOM;

        public String eventName() {
            return name().toLowerCase();
        }
    }
}
