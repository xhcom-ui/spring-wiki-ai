package com.example.satoken.demo.limiter;

import cn.dev33.satoken.redis.starter.SaRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

@Component
public class RateLimiter {

    @Resource
    private SaRedisTemplate saRedisTemplate;

    // 每个 IP 每分钟最多请求 60 次
    private static final int MAX_REQUESTS_PER_MINUTE = 60;

    public boolean tryAcquire(String key) {
        String redisKey = "rate:limit:ip:" + key;
        Long count = saRedisTemplate.opsForValue().increment(redisKey);
        if (count == 1) {
            // 第一次请求，设置过期时间为 1 分钟
            saRedisTemplate.expire(redisKey, 1, TimeUnit.MINUTES);
        }
        return count <= MAX_REQUESTS_PER_MINUTE;
    }
}
