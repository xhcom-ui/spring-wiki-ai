package org.wiki.service;

import org.wiki.config.McpConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
public class McpService {
    private final OkHttpClient client;
    private final ObjectMapper objectMapper;
    private final McpConfig mcpConfig;

    @Autowired
    public McpService(McpConfig mcpConfig) {
        this.client = new OkHttpClient();
        this.objectMapper = new ObjectMapper();
        this.mcpConfig = mcpConfig;
    }

    // 发送 MCP 请求
    public Map<String, Object> sendMcpRequest(String endpoint, Map<String, Object> payload) throws IOException {
        String apiUrl = mcpConfig.getApiUrl() + endpoint;

        String jsonBody = objectMapper.writeValueAsString(payload);

        Request request = new Request.Builder()
                .url(apiUrl)
                .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }

            String responseBody = response.body().string();
            return objectMapper.readValue(responseBody, Map.class);
        }
    }

    // 创建模型会话
    public Map<String, Object> createSession(String model) throws IOException {
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", model != null ? model : mcpConfig.getDefaultModel());
        payload.put("action", "create_session");

        return sendMcpRequest("/api/mcp/session", payload);
    }

    // 发送消息到模型
    public Map<String, Object> sendMessage(String sessionId, String message) throws IOException {
        Map<String, Object> payload = new HashMap<>();
        payload.put("session_id", sessionId);
        payload.put("message", message);
        payload.put("action", "send_message");

        return sendMcpRequest("/api/mcp/message", payload);
    }

    // 获取模型响应
    public Map<String, Object> getResponse(String sessionId) throws IOException {
        Map<String, Object> payload = new HashMap<>();
        payload.put("session_id", sessionId);
        payload.put("action", "get_response");

        return sendMcpRequest("/api/mcp/response", payload);
    }

    // 关闭模型会话
    public Map<String, Object> closeSession(String sessionId) throws IOException {
        Map<String, Object> payload = new HashMap<>();
        payload.put("session_id", sessionId);
        payload.put("action", "close_session");

        return sendMcpRequest("/api/mcp/session", payload);
    }

    // 获取模型列表
    public Map<String, Object> getModels() throws IOException {
        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "get_models");

        return sendMcpRequest("/api/mcp/models", payload);
    }
}
