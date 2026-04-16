package com.gateway.plugin.impl;

import com.gateway.plugin.GatewayPlugin;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.RequestRateLimiterGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.function.Function;

import java.util.HashMap;
import java.util.Map;

/**
 * 限流插件
 */
@Slf4j
@Component
public class RateLimitPlugin implements GatewayPlugin {

    private boolean enabled = false;
    private final RequestRateLimiterGatewayFilterFactory rateLimiterFactory;

    public RateLimitPlugin(RequestRateLimiterGatewayFilterFactory rateLimiterFactory) {
        this.rateLimiterFactory = rateLimiterFactory;
    }

    @Override
    public String getName() {
        return "rateLimit";
    }

    @Override
    public void initialize() {
        log.info("限流插件初始化");
    }

    @Override
    public void start() {
        enabled = true;
        log.info("限流插件启动");
    }

    @Override
    public void stop() {
        enabled = false;
        log.info("限流插件停止");
    }

    @Override
    public void destroy() {
        log.info("限流插件销毁");
    }

    @Override
    public GatewayFilter getGatewayFilter() {
        if (!enabled) {
            return null;
        }

        Map<String, Object> config = new HashMap<>();
        config.put("key-resolver", "userKeyResolver");
        config.put("rate-limiter", "redisRateLimiter");
        config.put("replenishRate", 10); // 每秒允许的请求数
        config.put("burstCapacity", 20); // 允许的突发请求数

        return rateLimiterFactory.apply(config);
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
     * Redis限流器
     */
    @Bean
    public RedisRateLimiter redisRateLimiter() {
        return new RedisRateLimiter(10, 20);
    }

    /**
     * 用户Key解析器
     */
    @Bean
    public Function<ServerWebExchange, Mono<String>> userKeyResolver() {
        return exchange -> Mono.just(exchange.getRequest().getRemoteAddress().getAddress().getHostAddress());
    }
}
