package com.example.agentscope.service;

import com.example.agentscope.model.Message;
import com.example.agentscope.repository.MessageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class MessageService {

    @Autowired
    private MessageRepository messageRepository;

    // 创建消息
    public Message createMessage(Long conversationId, String role, String content, String type) {
        Message message = new Message();
        message.setConversationId(conversationId);
        message.setRole(role);
        message.setContent(content);
        message.setType(type);
        message.setCreatedAt(LocalDateTime.now());
        return messageRepository.save(message);
    }

    // 获取对话的消息列表
    public List<Message> getMessagesByConversationId(Long conversationId) {
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
    }

    // 根据ID获取消息
    public Message getMessageById(Long id) {
        return messageRepository.findById(id).orElse(null);
    }

    // 删除消息
    public void deleteMessage(Long id) {
        messageRepository.deleteById(id);
    }
}
