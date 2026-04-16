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
public class KnowledgeAgent extends BaseAgent {

    @Autowired
    private AgentScope agentScope;

    public KnowledgeAgent() {
        super("knowledge_agent");
    }

    @Override
    public Message process(List<Message> messages) {
        log.info("KnowledgeAgent processing messages");

        // 构建检索查询
        String query = messages.get(messages.size() - 1).getContent();

        // 这里应该集成向量数据库进行知识检索
        // 示例：使用 AgentScope 的检索能力
        String retrievedKnowledge = retrieveKnowledge(query);

        // 构建回复
        String response = "基于知识库的回答：\n" + retrievedKnowledge;

        return Message.builder()
                .role("assistant")
                .content(response)
                .type(MessageType.TEXT)
                .build();
    }

    private String retrieveKnowledge(String query) {
        // 模拟知识检索
        // 实际应用中应该调用向量数据库进行检索
        return "从知识库中检索到的相关信息：\n" +
                "1. 公司的年假规则：每年15天，可累积\n" +
                "2. 新员工报销流程：填写报销单 -> 部门经理审批 -> 财务审批 -> 打款\n" +
                "3. 销售激励政策：季度销售额达到100万，奖励5%\n";
    }
}
