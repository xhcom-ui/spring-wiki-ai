package com.gateway.filter;

import com.gateway.manager.PluginManager;
import com.gateway.plugin.GatewayPlugin;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.util.List;

/**
 * 插件网关过滤器
 */
@Slf4j
@Component
public class PluginGatewayFilter extends AbstractGatewayFilterFactory<PluginGatewayFilter.Config> {

    @Resource
    private PluginManager pluginManager;

    public PluginGatewayFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            // 获取所有启用的插件
            List<GatewayFilter> filters = pluginManager.getPlugins().values().stream()
                    .filter(GatewayPlugin::isEnabled)
                    .map(GatewayPlugin::getGatewayFilter)
                    .filter(filter -> filter != null)
                    .toList();

            // 应用插件过滤器
            return applyFilters(exchange, chain, filters, 0);
        };
    }

    /**
     * 递归应用过滤器
     */
    private Mono<Void> applyFilters(ServerWebExchange exchange, GatewayFilterChain chain, 
                                   List<GatewayFilter> filters, int index) {
        if (index >= filters.size()) {
            return chain.filter(exchange);
        }

        GatewayFilter filter = filters.get(index);
        return filter.filter(exchange, exchange1 -> 
                applyFilters(exchange1, chain, filters, index + 1)
        );
    }

    public static class Config {
        // 配置属性
    }
}
