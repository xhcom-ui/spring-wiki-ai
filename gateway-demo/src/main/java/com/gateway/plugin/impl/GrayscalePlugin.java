package com.gateway.plugin.impl;

import com.gateway.plugin.GatewayPlugin;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 灰度发布插件
 */
@Slf4j
@Component
public class GrayscalePlugin implements GatewayPlugin {

    private boolean enabled = false;
    
    // 灰度配置映射
    private final Map<String, Integer> grayscaleConfig = new ConcurrentHashMap<>();

    public GrayscalePlugin() {
        // 初始化灰度配置
        grayscaleConfig.put("v1", 90); // 旧版本 90%
        grayscaleConfig.put("v2", 10); // 新版本 10%
    }

    @Override
    public String getName() {
        return "grayscale";
    }

    @Override
    public void initialize() {
        log.info("灰度发布插件初始化");
    }

    @Override
    public void start() {
        enabled = true;
        log.info("灰度发布插件启动");
    }

    @Override
    public void stop() {
        enabled = false;
        log.info("灰度发布插件停止");
    }

    @Override
    public void destroy() {
        log.info("灰度发布插件销毁");
    }

    @Override
    public GatewayFilter getGatewayFilter() {
        if (!enabled) {
            return null;
        }

        return (exchange, chain) -> {
            // 随机选择版本
            String version = selectVersion();
            log.info("请求路径: {}, 选择版本: {}", exchange.getRequest().getURI().getPath(), version);
            
            // 将版本信息添加到请求头
            exchange.getRequest().mutate()
                    .header("X-Forwarded-Version", version)
                    .build();
            
            // 继续执行请求
            return chain.filter(exchange);
        };
    }

    @Override
    public RouteLocator buildRoute(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("grayscale-route", r -> r
                        .path("/api/**")
                        .filters(f -> f
                                .filter(getGatewayFilter())
                        )
                        .uri("http://localhost:8081")
                )
                .build();
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
     * 随机选择版本
     */
    private String selectVersion() {
        int random = ThreadLocalRandom.current().nextInt(100);
        int cumulative = 0;
        
        for (Map.Entry<String, Integer> entry : grayscaleConfig.entrySet()) {
            cumulative += entry.getValue();
            if (random < cumulative) {
                return entry.getKey();
            }
        }
        
        return "v1"; // 默认返回旧版本
    }

    /**
     * 设置灰度配置
     */
    public void setGrayscaleConfig(String version, int weight) {
        grayscaleConfig.put(version, weight);
        log.info("更新灰度配置: {}, {}%", version, weight);
    }

    /**
     * 获取灰度配置
     */
    public Map<String, Integer> getGrayscaleConfig() {
        return grayscaleConfig;
    }
}
