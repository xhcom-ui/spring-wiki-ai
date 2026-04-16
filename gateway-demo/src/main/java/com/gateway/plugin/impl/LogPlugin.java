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

/**
 * 日志插件
 */
@Slf4j
@Component
public class LogPlugin implements GatewayPlugin {

    private boolean enabled = false;

    @Override
    public String getName() {
        return "log";
    }

    @Override
    public void initialize() {
        log.info("日志插件初始化");
    }

    @Override
    public void start() {
        enabled = true;
        log.info("日志插件启动");
    }

    @Override
    public void stop() {
        enabled = false;
        log.info("日志插件停止");
    }

    @Override
    public void destroy() {
        log.info("日志插件销毁");
    }

    @Override
    public GatewayFilter getGatewayFilter() {
        if (!enabled) {
            return null;
        }

        return (exchange, chain) -> {
            // 记录请求开始时间
            long startTime = System.currentTimeMillis();

            // 记录请求信息
            log.info("Request: {} {}", 
                exchange.getRequest().getMethod(), 
                exchange.getRequest().getURI());

            // 继续执行请求
            return chain.filter(exchange).then(Mono.fromRunnable(() -> {
                // 记录响应信息
                long endTime = System.currentTimeMillis();
                log.info("Response: {} {} ms", 
                    exchange.getResponse().getStatusCode(), 
                    endTime - startTime);
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
}
