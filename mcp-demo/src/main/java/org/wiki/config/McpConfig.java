package org.wiki.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpConfig {
    @Value("${mcp.api.url:http://localhost:8080}")
    private String apiUrl;

    @Value("${mcp.model:default}")
    private String defaultModel;

    public String getApiUrl() {
        return apiUrl;
    }

    public String getDefaultModel() {
        return defaultModel;
    }
}
