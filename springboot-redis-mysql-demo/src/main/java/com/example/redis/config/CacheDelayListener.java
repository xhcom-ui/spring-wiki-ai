package com.example.redis.config;

import org.redisson.api.RDelayedQueue;
import org.redisson.api.RQueue;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class CacheDelayListener {

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private CacheManager cacheManager;

    private static final String DELAY_QUEUE_NAME = "cache:delay:queue";
    private static final String CACHE_NAME = "product";

    @PostConstruct
    public void startListener() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(() -> {
            RQueue<String> queue = redissonClient.getQueue(DELAY_QUEUE_NAME);
            RDelayedQueue<String> delayedQueue = redissonClient.getDelayedQueue(queue);
            while (true) {
                try {
                    String cacheKey = delayedQueue.take();
                    if (cacheKey != null) {
                        handleCacheDelete(cacheKey);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
    }

    private void handleCacheDelete(String cacheKey) {
        Cache cache = cacheManager.getCache(CACHE_NAME);
        if (cache != null) {
            if (cacheKey.equals("product:all")) {
                // 删除所有商品的缓存
                cache.evict("all");
            } else {
                // 删除指定商品的缓存
                String idStr = cacheKey.replace("product:", "");
                try {
                    Long id = Long.parseLong(idStr);
                    cache.evict(id);
                } catch (NumberFormatException e) {
                    // 忽略格式错误的缓存键
                }
            }
        }
    }
}
