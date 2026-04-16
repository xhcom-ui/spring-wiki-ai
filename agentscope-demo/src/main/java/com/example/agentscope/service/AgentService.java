package com.example.agentscope.service;

import com.alibaba.agentscope.core.AgentScope;
import com.alibaba.agentscope.core.model.message.Message;
import com.alibaba.agentscope.core.model.message.MessageType;
import com.example.agentscope.agent.RouterAgent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class AgentService {

    @Autowired
    private AgentScope agentScope;

    @Autowired
    private RouterAgent routerAgent;

    // 处理用户请求
    public String processRequest(String query, List<String> history) {
        log.info("Processing request: {}", query);

        // 构建消息列表
        List<Message> messages = new ArrayList<>();

        // 添加历史消息
        for (String message : history) {
            messages.add(Message.builder()
                    .role("user")
                    .content(message)
                    .type(MessageType.TEXT)
                    .build());
        }

        // 添加当前请求
        messages.add(Message.builder()
                .role("user")
                .content(query)
                .type(MessageType.TEXT)
                .build());

        // 使用 RouterAgent 处理请求
        Message response = routerAgent.process(messages);

        log.info("Agent response: {}", response.getContent());

        return response.getContent();
    }

    // 初始化 AgentScope
    public void initAgentScope() {
        try {
            // AgentScope 已经在 AgentConfig 中初始化
            log.info("AgentScope initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize AgentScope", e);
        }
    }
}
