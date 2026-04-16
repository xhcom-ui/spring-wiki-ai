package com.example.agentscope.controller;

import com.example.agentscope.model.Message;
import com.example.agentscope.service.MessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/messages")
public class MessageController {

    @Autowired
    private MessageService messageService;

    // 创建消息
    @PostMapping
    public ResponseEntity<Message> createMessage(@RequestBody Map<String, Object> request) {
        try {
            Long conversationId = Long.valueOf(request.get("conversationId").toString());
            String role = request.get("role").toString();
            String content = request.get("content").toString();
            String type = request.get("type").toString();
            Message message = messageService.createMessage(conversationId, role, content, type);
            return ResponseEntity.ok(message);
        } catch (Exception e) {
            log.error("Create message failed", e);
            return ResponseEntity.badRequest().body(null);
        }
    }

    // 获取对话的消息列表
    @GetMapping("/conversation/{conversationId}")
    public ResponseEntity<List<Message>> getMessagesByConversationId(@PathVariable Long conversationId) {
        try {
            List<Message> messages = messageService.getMessagesByConversationId(conversationId);
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            log.error("Get messages failed", e);
            return ResponseEntity.badRequest().body(null);
        }
    }

    // 根据ID获取消息
    @GetMapping("/{id}")
    public ResponseEntity<Message> getMessageById(@PathVariable Long id) {
        try {
            Message message = messageService.getMessageById(id);
            if (message == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(message);
        } catch (Exception e) {
            log.error("Get message failed", e);
            return ResponseEntity.badRequest().body(null);
        }
    }

    // 删除消息
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteMessage(@PathVariable Long id) {
        try {
            messageService.deleteMessage(id);
            return ResponseEntity.ok(Map.of("message", "消息删除成功"));
        } catch (Exception e) {
            log.error("Delete message failed", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
