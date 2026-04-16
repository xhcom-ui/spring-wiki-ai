package com.gateway.plugin;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;

/**
 * 网关插件接口
 */
public interface GatewayPlugin {
    /**
     * 插件名称
     */
    String getName();

    /**
     * 初始化插件
     */
    void initialize();

    /**
     * 启动插件
     */
    void start();

    /**
     * 停止插件
     */
    void stop();

    /**
     * 销毁插件
     */
    void destroy();

    /**
     * 获取网关过滤器
     */
    GatewayFilter getGatewayFilter();

    /**
     * 构建路由
     */
    default RouteLocator buildRoute(RouteLocatorBuilder builder) {
        return builder.routes().build();
    }

    /**
     * 插件是否启用
     */
    boolean isEnabled();

    /**
     * 设置插件启用状态
     */
    void setEnabled(boolean enabled);
}
