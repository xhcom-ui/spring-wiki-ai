package com.example.agentscope.controller;

import com.example.agentscope.model.Conversation;
import com.example.agentscope.service.ConversationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    @Autowired
    private ConversationService conversationService;

    // 创建对话
    @PostMapping
    public ResponseEntity<Conversation> createConversation(@RequestBody Map<String, Object> request) {
        try {
            Long userId = Long.valueOf(request.get("userId").toString());
            String title = request.get("title").toString();
            Conversation conversation = conversationService.createConversation(userId, title);
            return ResponseEntity.ok(conversation);
        } catch (Exception e) {
            log.error("Create conversation failed", e);
            return ResponseEntity.badRequest().body(null);
        }
    }

    // 获取用户的对话列表
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Conversation>> getConversationsByUserId(@PathVariable Long userId) {
        try {
            List<Conversation> conversations = conversationService.getConversationsByUserId(userId);
            return ResponseEntity.ok(conversations);
        } catch (Exception e) {
            log.error("Get conversations failed", e);
            return ResponseEntity.badRequest().body(null);
        }
    }

    // 根据ID获取对话
    @GetMapping("/{id}")
    public ResponseEntity<Conversation> getConversationById(@PathVariable Long id) {
        try {
            Conversation conversation = conversationService.getConversationById(id);
            if (conversation == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(conversation);
        } catch (Exception e) {
            log.error("Get conversation failed", e);
            return ResponseEntity.badRequest().body(null);
        }
    }

    // 更新对话
    @PutMapping("/{id}")
    public ResponseEntity<Conversation> updateConversation(@PathVariable Long id, @RequestBody Conversation conversation) {
        try {
            conversation.setId(id);
            Conversation updatedConversation = conversationService.updateConversation(conversation);
            return ResponseEntity.ok(updatedConversation);
        } catch (Exception e) {
            log.error("Update conversation failed", e);
            return ResponseEntity.badRequest().body(null);
        }
    }

    // 删除对话
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteConversation(@PathVariable Long id) {
        try {
            conversationService.deleteConversation(id);
            return ResponseEntity.ok(Map.of("message", "对话删除成功"));
        } catch (Exception e) {
            log.error("Delete conversation failed", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
