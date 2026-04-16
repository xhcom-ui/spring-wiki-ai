package com.gateway.plugin.impl;

import com.gateway.plugin.GatewayPlugin;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 流量回放插件
 */
@Slf4j
@Component
public class TrafficReplayPlugin implements GatewayPlugin {

    private boolean enabled = false;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WebClient webClient = WebClient.create();
    
    // 回放配置
    private String recordingPath = "records";
    private String targetUrl = "http://localhost:8081";

    @Override
    public String getName() {
        return "trafficReplay";
    }

    @Override
    public void initialize() {
        log.info("流量回放插件初始化");
    }

    @Override
    public void start() {
        enabled = true;
        log.info("流量回放插件启动");
    }

    @Override
    public void stop() {
        enabled = false;
        log.info("流量回放插件停止");
    }

    @Override
    public void destroy() {
        log.info("流量回放插件销毁");
    }

    @Override
    public GatewayFilter getGatewayFilter() {
        if (!enabled) {
            return null;
        }

        return (exchange, chain) -> {
            // 流量回放插件主要通过API接口触发，不直接作为过滤器使用
            return chain.filter(exchange);
        };
    }

    @Override
    public RouteLocator buildRoute(RouteLocatorBuilder builder) {
        return builder.routes().build();
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 开始回放流量
     */
    public void startReplay() {
        log.info("开始回放流量");
        
        // 遍历录制目录
        File directory = new File(recordingPath);
        if (!directory.exists() || !directory.isDirectory()) {
            log.error("录制目录不存在: {}", recordingPath);
            return;
        }
        
        File[] files = directory.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null || files.length == 0) {
            log.error("录制目录中没有JSON文件: {}", recordingPath);
            return;
        }
        
        // 回放每个录制的流量
        for (File file : files) {
            try {
                // 读取录制的流量
                String content = Files.readString(Paths.get(file.getAbsolutePath()));
                TrafficRecord record = objectMapper.readValue(content, TrafficRecord.class);
                
                // 回放流量
                replayRecord(record);
            } catch (IOException e) {
                log.error("回放流量失败: {}", e.getMessage(), e);
            }
        }
        
        log.info("流量回放完成");
    }

    /**
     * 回放单个录制的流量
     */
    private void replayRecord(TrafficRecord record) {
        log.info("回放流量: {} {}", record.getMethod(), record.getPath());
        
        try {
            // 构建请求
            WebClient.RequestBodySpec requestSpec = webClient
                    .method(HttpMethod.valueOf(record.getMethod()))
                    .uri(targetUrl + record.getPath());
            
            // 添加请求头
            if (record.getHeaders() != null) {
                for (Map.Entry<String, String> header : record.getHeaders().entrySet()) {
                    requestSpec = requestSpec.header(header.getKey(), header.getValue());
                }
            }
            
            // 添加查询参数
            if (record.getQueryParams() != null) {
                for (Map.Entry<String, String> param : record.getQueryParams().entrySet()) {
                    requestSpec = requestSpec.queryParam(param.getKey(), param.getValue());
                }
            }
            
            // 发送请求并获取响应
            var response = requestSpec
                    .retrieve()
                    .toEntity(String.class)
                    .block();
            
            // 记录回放结果
            log.info("回放结果: {} {}", response.getStatusCode(), record.getPath());
            
            // 对比响应状态码
            if (response.getStatusCode().value() != record.getStatusCode()) {
                log.warn("响应状态码不匹配: 期望={}, 实际={}, 路径={}", 
                        record.getStatusCode(), response.getStatusCode().value(), record.getPath());
            }
        } catch (Exception e) {
            log.error("回放流量失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 流量记录实体
     */
    private static class TrafficRecord {
        private String path;
        private String method;
        private Map<String, String> headers;
        private Map<String, String> queryParams;
        private int statusCode;
        private Map<String, String> responseHeaders;
        private LocalDateTime timestamp;

        // getters and setters
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        public String getMethod() { return method; }
        public void setMethod(String method) { this.method = method; }
        public Map<String, String> getHeaders() { return headers; }
        public void setHeaders(Map<String, String> headers) { this.headers = headers; }
        public Map<String, String> getQueryParams() { return queryParams; }
        public void setQueryParams(Map<String, String> queryParams) { this.queryParams = queryParams; }
        public int getStatusCode() { return statusCode; }
        public void setStatusCode(int statusCode) { this.statusCode = statusCode; }
        public Map<String, String> getResponseHeaders() { return responseHeaders; }
        public void setResponseHeaders(Map<String, String> responseHeaders) { this.responseHeaders = responseHeaders; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    }

    /**
     * 设置录制路径
     */
    public void setRecordingPath(String recordingPath) {
        this.recordingPath = recordingPath;
        log.info("更新录制路径: {}", recordingPath);
    }

    /**
     * 设置目标URL
     */
    public void setTargetUrl(String targetUrl) {
        this.targetUrl = targetUrl;
        log.info("更新目标URL: {}", targetUrl);
    }

    /**
     * 获取回放配置
     */
    public Map<String, Object> getReplayConfig() {
        Map<String, Object> config = new ConcurrentHashMap<>();
        config.put("recordingPath", recordingPath);
        config.put("targetUrl", targetUrl);
        return config;
    }
}
