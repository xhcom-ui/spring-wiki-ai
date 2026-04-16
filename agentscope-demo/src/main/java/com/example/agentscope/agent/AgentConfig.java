package com.example.agentscope.agent;

import com.alibaba.agentscope.core.AgentScope;
import com.alibaba.agentscope.core.config.AgentScopeConfig;
import com.alibaba.agentscope.core.model.llm.LlmConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AgentConfig {

    @Value("${agentscope.model.api-key}")
    private String apiKey;

    @Value("${agentscope.model.model-name}")
    private String modelName;

    @Bean
    public AgentScope agentScope() {
        // 配置 AgentScope
        AgentScopeConfig config = AgentScopeConfig.builder()
                .llmConfig(LlmConfig.builder()
                        .apiKey(apiKey)
                        .modelName(modelName)
                        .build())
                .build();

        // 初始化 AgentScope
        AgentScope agentScope = AgentScope.init(config);
        return agentScope;
    }
}
