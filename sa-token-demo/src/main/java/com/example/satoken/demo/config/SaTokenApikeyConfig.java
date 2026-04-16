package com.example.satoken.demo.config;

import cn.dev33.satoken.apikey.SaApiKeyTemplate;
import cn.dev33.satoken.redis.starter.SaRedisTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SaTokenApikeyConfig {

    @Value("${api-key.prefix}")
    private String prefix;

    @Value("${api-key.length}")
    private int length;

    @Value("${api-key.expire}")
    private long expire;

    @Bean
    public SaApiKeyTemplate saApiKeyTemplate(SaRedisTemplate saRedisTemplate) {
        return new SaApiKeyTemplate()
                .setRedisTemplate(saRedisTemplate)
                .setKeyPrefix(prefix)
                .setKeyLength(length)
                .setExpire(expire);
    }
}
