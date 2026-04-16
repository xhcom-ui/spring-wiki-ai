package com.gateway.plugin.impl;

import com.gateway.plugin.GatewayPlugin;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 流量录制插件
 */
@Slf4j
@Component
public class TrafficRecordingPlugin implements GatewayPlugin {

    private boolean enabled = false;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // 录制配置
    private String recordingPath = "records";
    private List<String> includePaths = new ArrayList<>();
    private List<String> excludePaths = new ArrayList<>();

    public TrafficRecordingPlugin() {
        // 初始化配置
        includePaths.add("/api/**");
        excludePaths.add("/api/health");
        excludePaths.add("/api/metrics");
        
        // 创建录制目录
        File directory = new File(recordingPath);
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }

    @Override
    public String getName() {
        return "trafficRecording";
    }

    @Override
    public void initialize() {
        log.info("流量录制插件初始化");
    }

    @Override
    public void start() {
        enabled = true;
        log.info("流量录制插件启动");
    }

    @Override
    public void stop() {
        enabled = false;
        log.info("流量录制插件停止");
    }

    @Override
    public void destroy() {
        log.info("流量录制插件销毁");
    }

    @Override
    public GatewayFilter getGatewayFilter() {
        if (!enabled) {
            return null;
        }

        return (exchange, chain) -> {
            // 获取请求路径
            String path = exchange.getRequest().getURI().getPath();
            
            // 检查是否需要录制
            if (!shouldRecord(path)) {
                return chain.filter(exchange);
            }
            
            // 记录请求信息
            TrafficRecord record = new TrafficRecord();
            record.setPath(path);
            record.setMethod(exchange.getRequest().getMethod().name());
            record.setHeaders(exchange.getRequest().getHeaders().toSingleValueMap());
            record.setQueryParams(exchange.getRequest().getQueryParams().toSingleValueMap());
            record.setTimestamp(LocalDateTime.now());
            
            // 继续执行请求并记录响应
            return chain.filter(exchange).then(Mono.fromRunnable(() -> {
                // 记录响应信息
                record.setStatusCode(exchange.getResponse().getStatusCode().value());
                record.setResponseHeaders(exchange.getResponse().getHeaders().toSingleValueMap());
                
                // 保存录制的流量
                saveRecord(record);
            }));
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
     * 检查是否需要录制
     */
    private boolean shouldRecord(String path) {
        // 检查是否在排除路径中
        for (String excludePath : excludePaths) {
            if (path.matches(excludePath.replace("**", ".*"))) {
                return false;
            }
        }
        
        // 检查是否在包含路径中
        for (String includePath : includePaths) {
            if (path.matches(includePath.replace("**", ".*"))) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * 保存录制的流量
     */
    private void saveRecord(TrafficRecord record) {
        try {
            // 创建文件名
            String timestamp = record.getTimestamp().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
            String fileName = recordingPath + File.separator + "record_" + timestamp + ".json";
            
            // 写入文件
            try (FileWriter writer = new FileWriter(fileName)) {
                objectMapper.writeValue(writer, record);
            }
            
            log.info("流量录制保存成功: {}", fileName);
        } catch (IOException e) {
            log.error("流量录制保存失败: {}", e.getMessage(), e);
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
        // 创建录制目录
        File directory = new File(recordingPath);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        log.info("更新录制路径: {}", recordingPath);
    }

    /**
     * 设置包含路径
     */
    public void setIncludePaths(List<String> includePaths) {
        this.includePaths = includePaths;
        log.info("更新包含路径: {}", includePaths);
    }

    /**
     * 设置排除路径
     */
    public void setExcludePaths(List<String> excludePaths) {
        this.excludePaths = excludePaths;
        log.info("更新排除路径: {}", excludePaths);
    }

    /**
     * 获取录制配置
     */
    public Map<String, Object> getRecordingConfig() {
        Map<String, Object> config = new ConcurrentHashMap<>();
        config.put("recordingPath", recordingPath);
        config.put("includePaths", includePaths);
        config.put("excludePaths", excludePaths);
        return config;
    }
}
