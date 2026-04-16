package com.gateway.plugin.impl;

import com.gateway.plugin.GatewayPlugin;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 超时插件
 */
@Slf4j
@Component
public class TimeoutPlugin implements GatewayPlugin {

    private boolean enabled = false;
    
    // 超时配置映射
    private final Map<String, Integer> timeoutConfig = new ConcurrentHashMap<>();

    public TimeoutPlugin() {
        // 初始化超时配置
        timeoutConfig.put("core", 30000); // 核心接口 30s
        timeoutConfig.put("important", 10000); // 重要接口 10s
        timeoutConfig.put("normal", 5000); // 普通接口 5s
        timeoutConfig.put("fast", 1000); // 快速接口 1s
    }

    @Override
    public String getName() {
        return "timeout";
    }

    @Override
    public void initialize() {
        log.info("超时插件初始化");
    }

    @Override
    public void start() {
        enabled = true;
        log.info("超时插件启动");
    }

    @Override
    public void stop() {
        enabled = false;
        log.info("超时插件停止");
    }

    @Override
    public void destroy() {
        log.info("超时插件销毁");
    }

    @Override
    public GatewayFilter getGatewayFilter() {
        if (!enabled) {
            return null;
        }

        return (exchange, chain) -> {
            // 获取请求路径
            String path = exchange.getRequest().getURI().getPath();
            
            // 确定接口级别
            String level = determineLevel(path);
            
            // 获取对应级别的超时时间
            int timeout = timeoutConfig.getOrDefault(level, 5000);
            
            log.info("请求路径: {}, 接口级别: {}, 超时时间: {}ms", path, level, timeout);
            
            // 应用超时处理
            return chain.filter(exchange)
                    .timeout(java.time.Duration.ofMillis(timeout))
                    .onErrorResume(error -> {
                        log.warn("请求超时: {}", path, error);
                        exchange.getResponse().setStatusCode(HttpStatus.GATEWAY_TIMEOUT);
                        return exchange.getResponse().setComplete();
                    });
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
     * 确定接口级别
     */
    private String determineLevel(String path) {
        // 核心接口
        if (path.contains("/api/pay") || path.contains("/api/login") || path.contains("/api/order")) {
            return "core";
        }
        // 重要接口
        else if (path.contains("/api/user") || path.contains("/api/product")) {
            return "important";
        }
        // 快速接口
        else if (path.contains("/api/health") || path.contains("/api/status")) {
            return "fast";
        }
        // 普通接口
        else {
            return "normal";
        }
    }

    /**
     * 设置超时配置
     */
    public void setTimeoutConfig(String level, int timeout) {
        timeoutConfig.put(level, timeout);
        log.info("更新超时配置: {}, {}ms", level, timeout);
    }

    /**
     * 获取超时配置
     */
    public Map<String, Integer> getTimeoutConfig() {
        return timeoutConfig;
    }
}
