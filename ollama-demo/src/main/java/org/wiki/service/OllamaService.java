package org.wiki.service;

import org.wiki.config.OllamaConfig;
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
public class OllamaService {
    private final OkHttpClient client;
    private final ObjectMapper objectMapper;
    private final OllamaConfig ollamaConfig;

    @Autowired
    public OllamaService(OllamaConfig ollamaConfig) {
        this.client = new OkHttpClient();
        this.objectMapper = new ObjectMapper();
        this.ollamaConfig = ollamaConfig;
    }

    // 调用 Ollama API 生成文本
    public String generateText(String prompt, String model) throws IOException {
        String apiUrl = ollamaConfig.getApiUrl() + "/api/generate";
        String finalModel = model != null ? model : ollamaConfig.getDefaultModel();

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", finalModel);
        requestBody.put("prompt", prompt);
        requestBody.put("stream", false);

        String jsonBody = objectMapper.writeValueAsString(requestBody);

        Request request = new Request.Builder()
                .url(apiUrl)
                .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }

            String responseBody = response.body().string();
            Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);
            return (String) responseMap.get("response");
        }
    }

    // 获取可用的模型列表
    public Map<String, Object> getModels() throws IOException {
        String apiUrl = ollamaConfig.getApiUrl() + "/api/tags";

        Request request = new Request.Builder()
                .url(apiUrl)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }

            String responseBody = response.body().string();
            return objectMapper.readValue(responseBody, Map.class);
        }
    }

    // 聊天模式
    public String chat(String message, String model) throws IOException {
        String apiUrl = ollamaConfig.getApiUrl() + "/api/chat";
        String finalModel = model != null ? model : ollamaConfig.getDefaultModel();

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", finalModel);
        requestBody.put("stream", false);

        Map<String, Object> messageMap = new HashMap<>();
        messageMap.put("role", "user");
        messageMap.put("content", message);

        requestBody.put("messages", new Object[]{messageMap});

        String jsonBody = objectMapper.writeValueAsString(requestBody);

        Request request = new Request.Builder()
                .url(apiUrl)
                .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }

            String responseBody = response.body().string();
            Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);
            Map<String, Object> messageResponse = (Map<String, Object>) ((Object[]) responseMap.get("message"))[0];
            return (String) messageResponse.get("content");
        }
    }
}
