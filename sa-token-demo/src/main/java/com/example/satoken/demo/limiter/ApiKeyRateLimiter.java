package com.example.satoken.demo.limiter;

import cn.dev33.satoken.redis.starter.SaRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

@Component
public class ApiKeyRateLimiter {

    @Resource
    private SaRedisTemplate saRedisTemplate;

    // 每个 API Key 每分钟最多调用 100 次
    private static final int MAX_REQUESTS_PER_MINUTE = 100;

    public boolean tryAcquire(String apiKey) {
        String redisKey = "rate:limit:api:" + apiKey;
        Long count = saRedisTemplate.opsForValue().increment(redisKey);
        if (count == 1) {
            // 第一次调用，设置过期时间为 1 分钟
            saRedisTemplate.expire(redisKey, 1, TimeUnit.MINUTES);
        }
        return count <= MAX_REQUESTS_PER_MINUTE;
    }
}
