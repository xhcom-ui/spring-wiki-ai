package com.gateway.plugin.impl;

import cn.dev33.satoken.stp.StpUtil;
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

/**
 * 鉴权插件
 */
@Slf4j
@Component
public class AuthPlugin implements GatewayPlugin {

    private boolean enabled = false;

    @Override
    public String getName() {
        return "auth";
    }

    @Override
    public void initialize() {
        log.info("鉴权插件初始化");
    }

    @Override
    public void start() {
        enabled = true;
        log.info("鉴权插件启动");
    }

    @Override
    public void stop() {
        enabled = false;
        log.info("鉴权插件停止");
    }

    @Override
    public void destroy() {
        log.info("鉴权插件销毁");
    }

    @Override
    public GatewayFilter getGatewayFilter() {
        if (!enabled) {
            return null;
        }

        return (exchange, chain) -> {
            // 从请求头中获取token
            String token = exchange.getRequest().getHeaders().getFirst("so-token");
            
            // 验证token
            if (token == null || !StpUtil.checkToken(token)) {
                // 未授权，返回401
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            // 鉴权通过，继续执行
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
}
