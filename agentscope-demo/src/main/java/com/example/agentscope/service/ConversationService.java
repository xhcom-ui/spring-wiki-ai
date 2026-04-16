package com.example.agentscope.service;

import com.example.agentscope.model.Conversation;
import com.example.agentscope.repository.ConversationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class ConversationService {

    @Autowired
    private ConversationRepository conversationRepository;

    // 创建对话
    public Conversation createConversation(Long userId, String title) {
        Conversation conversation = new Conversation();
        conversation.setUserId(userId);
        conversation.setTitle(title);
        conversation.setStatus("active");
        conversation.setCreatedAt(LocalDateTime.now());
        conversation.setUpdatedAt(LocalDateTime.now());
        return conversationRepository.save(conversation);
    }

    // 获取用户的对话列表
    public List<Conversation> getConversationsByUserId(Long userId) {
        return conversationRepository.findByUserId(userId);
    }

    // 根据ID获取对话
    public Conversation getConversationById(Long id) {
        return conversationRepository.findById(id).orElse(null);
    }

    // 更新对话
    public Conversation updateConversation(Conversation conversation) {
        conversation.setUpdatedAt(LocalDateTime.now());
        return conversationRepository.save(conversation);
    }

    // 删除对话
    public void deleteConversation(Long id) {
        conversationRepository.deleteById(id);
    }
}
