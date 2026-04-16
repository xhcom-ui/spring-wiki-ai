package com.example.agentscope.agent;

import com.alibaba.agentscope.core.AgentScope;
import com.alibaba.agentscope.core.agent.BaseAgent;
import com.alibaba.agentscope.core.model.message.Message;
import com.alibaba.agentscope.core.model.message.MessageType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class RouterAgent extends BaseAgent {

    @Autowired
    private AgentScope agentScope;

    @Autowired
    private KnowledgeAgent knowledgeAgent;

    @Autowired
    private ToolAgent toolAgent;

    public RouterAgent() {
        super("router_agent");
    }

    @Override
    public Message process(List<Message> messages) {
        log.info("RouterAgent processing messages");

        // 分析用户问题，决定使用哪个 Agent
        String query = messages.get(messages.size() - 1).getContent();
        String agentType = routeQuery(query);

        log.info("Routing to agent: {}", agentType);

        // 根据路由结果选择相应的 Agent
        if ("knowledge".equals(agentType)) {
            return knowledgeAgent.process(messages);
        } else if ("tool".equals(agentType)) {
            return toolAgent.process(messages);
        } else {
            // 默认使用知识 Agent
            return knowledgeAgent.process(messages);
        }
    }

    private String routeQuery(String query) {
        // 简单的路由逻辑
        // 实际应用中可以使用更复杂的 NLP 模型进行意图识别
        query = query.toLowerCase();

        if (query.contains("搜索") || query.contains("查询") || query.contains("天气") || query.contains("新闻")) {
            return "tool";
        } else {
            return "knowledge";
        }
    }
}
