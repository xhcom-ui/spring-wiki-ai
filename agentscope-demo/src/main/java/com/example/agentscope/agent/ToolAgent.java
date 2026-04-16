package com.example.agentscope.agent;

import com.alibaba.agentscope.core.AgentScope;
import com.alibaba.agentscope.core.agent.BaseAgent;
import com.alibaba.agentscope.core.model.message.Message;
import com.alibaba.agentscope.core.model.message.MessageType;
import com.alibaba.agentscope.core.model.message.ToolCall;
import com.alibaba.agentscope.core.model.message.ToolResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class ToolAgent extends BaseAgent {

    @Autowired
    private AgentScope agentScope;

    public ToolAgent() {
        super("tool_agent");
    }

    @Override
    public Message process(List<Message> messages) {
        log.info("ToolAgent processing messages");

        // 检查是否有工具调用
        Message lastMessage = messages.get(messages.size() - 1);
        if (lastMessage.getType() == MessageType.TOOL_CALL) {
            ToolCall toolCall = lastMessage.getToolCall();
            return executeTool(toolCall);
        }

        // 构建工具调用请求
        String query = lastMessage.getContent();
        String toolCallContent = buildToolCall(query);

        return Message.builder()
                .role("assistant")
                .content(toolCallContent)
                .type(MessageType.TOOL_CALL)
                .toolCall(ToolCall.builder()
                        .name("search")
                        .parameters("{\"query\": \"" + query + "\"}")
                        .build())
                .build();
    }

    private Message executeTool(ToolCall toolCall) {
        log.info("Executing tool: {}", toolCall.getName());

        // 模拟工具执行
        String result = "工具执行结果：\n" +
                "查询内容：" + toolCall.getParameters() + "\n" +
                "执行结果：这是模拟的工具执行结果";

        return Message.builder()
                .role("tool")
                .content(result)
                .type(MessageType.TOOL_RESPONSE)
                .toolResponse(ToolResponse.builder()
                        .toolCallId(toolCall.getId())
                        .output(result)
                        .build())
                .build();
    }

    private String buildToolCall(String query) {
        // 构建工具调用内容
        return "需要使用工具来回答这个问题";
    }
}
