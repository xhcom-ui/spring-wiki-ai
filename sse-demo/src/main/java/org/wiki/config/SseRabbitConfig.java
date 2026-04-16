package org.wiki.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SseRabbitConfig {
    
    // SSE 专属直连交换机
    public static final String SSE_EXCHANGE = "sse.exchange";
    // 队列前缀（按用户ID隔离）
    public static final String SSE_QUEUE_PREFIX = "sse.queue.";
    // 路由键格式
    public static final String ROUTING_KEY_PREFIX = "sse.user.";

    @Bean
    public DirectExchange sseExchange() {
        return new DirectExchange(SSE_EXCHANGE, true, false);
    }

    // 动态队列：为每个用户创建独立的持久化队列
    @Bean
    public Queue userSseQueue(@Value("${app.user.id}") String userId) {
        return new Queue(SSE_QUEUE_PREFIX + userId, true, false, false);
    }

    // 动态绑定：将用户队列绑定到交换机
    @Bean
    public Binding userBinding(Queue userSseQueue, DirectExchange sseExchange) {
        String routingKey = ROUTING_KEY_PREFIX + userSseQueue.getName().replace(SSE_QUEUE_PREFIX, "");
        return BindingBuilder.bind(userSseQueue).to(sseExchange).with(routingKey);
    }
}