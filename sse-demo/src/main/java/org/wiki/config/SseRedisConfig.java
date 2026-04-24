package org.wiki.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.wiki.consumer.SseMessageConsumer;

@Configuration
public class SseRedisConfig {

    public static final String SSE_TOPIC = "sse:push";

    @Bean
    public MessageListenerAdapter sseRedisMessageListener(SseMessageConsumer consumer) {
        return new MessageListenerAdapter(consumer, "handleMessage");
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            MessageListenerAdapter sseRedisMessageListener
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(sseRedisMessageListener, new PatternTopic(SSE_TOPIC));
        return container;
    }
}
