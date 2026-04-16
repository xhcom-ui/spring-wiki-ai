package com.gateway.config;

import com.gateway.filter.PluginGatewayFilter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

/**
 * 路由配置
 */
@Configuration
public class RouterConfig {

    @Resource
    private PluginGatewayFilter pluginGatewayFilter;

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("demo-route", r -> r
                        .path("/api/**")
                        .filters(f -> f
                                .stripPrefix(1)
                                .filter(pluginGatewayFilter.apply(new PluginGatewayFilter.Config()))
                        )
                        .uri("http://localhost:8081")
                )
                .build();
    }
}
