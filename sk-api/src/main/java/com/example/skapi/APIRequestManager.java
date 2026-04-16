package com.example.skapi;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class APIRequestManager {
    private final APIKeyPool keyPool;
    private final TokenBucket tokenBucket;
    private final ExponentialBackoffRetry retry;
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicInteger failureCount = new AtomicInteger(0);

    public APIRequestManager(List<String> apiKeys, double bucketCapacity, double bucketFillRate, int maxRetries) {
        if (apiKeys == null || apiKeys.isEmpty()) {
            throw new IllegalArgumentException("API Key 列表不能为空");
        }
        if (bucketCapacity <= 0) {
            throw new IllegalArgumentException("令牌桶容量必须大于0");
        }
        if (bucketFillRate <= 0) {
            throw new IllegalArgumentException("令牌填充速率必须大于0");
        }
        if (maxRetries < 0) {
            throw new IllegalArgumentException("最大重试次数不能为负数");
        }

        this.keyPool = new APIKeyPool(apiKeys);
        this.tokenBucket = new TokenBucket(bucketCapacity, bucketFillRate);
        this.retry = new ExponentialBackoffRetry(maxRetries);
        log.info("APIRequestManager 初始化成功，API Key 数量: {}, 令牌桶容量: {}, 填充速率: {}, 最大重试次数: {}",
                apiKeys.size(), bucketCapacity, bucketFillRate, maxRetries);
    }

    public boolean executeAPIRequest(Runnable task) {
        try {
            // 1. 获取API Key
            String apiKey = keyPool.getNextKey();
            log.debug("使用API Key: {}", apiKey);

            // 2. 令牌桶控速
            if (!tokenBucket.acquire(1)) {
                log.warn("令牌桶限流，请求被拒绝");
                failureCount.incrementAndGet();
                return false;
            }

            // 3. 指数退避重试
            boolean success = retry.execute(() -> {
                try {
                    // 这里可以添加使用API Key的逻辑
                    task.run();
                } catch (Exception e) {
                    log.error("API调用失败: {}", e.getMessage(), e);
                    throw e;
                }
            });

            if (success) {
                successCount.incrementAndGet();
                log.debug("API调用成功");
            } else {
                failureCount.incrementAndGet();
                log.warn("API调用最终失败");
            }

            return success;
        } catch (Exception e) {
            log.error("执行API请求失败: {}", e.getMessage(), e);
            failureCount.incrementAndGet();
            return false;
        }
    }

    // 动态添加API Key
    public void addApiKey(String apiKey) {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalArgumentException("API Key 不能为空");
        }
        keyPool.addKey(apiKey);
    }

    // 动态移除API Key
    public void removeApiKey(String apiKey) {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalArgumentException("API Key 不能为空");
        }
        keyPool.removeKey(apiKey);
    }

    // 获取 API Key 数量
    public int getApiKeyCount() {
        return keyPool.size();
    }

    // 获取统计信息
    public int getSuccessCount() {
        return successCount.get();
    }

    public int getFailureCount() {
        return failureCount.get();
    }

    public double getSuccessRate() {
        int total = successCount.get() + failureCount.get();
        return total > 0 ? (double) successCount.get() / total : 0;
    }

    // 示例使用
    public static void main(String[] args) {
        List<String> apiKeys = List.of("key1", "key2", "key3");
        APIRequestManager manager = new APIRequestManager(apiKeys, 100, 50, 3);

        // 模拟10个API请求
        for (int i = 0; i < 10; i++) {
            final int requestId = i;
            manager.executeAPIRequest(() -> {
                log.info("执行请求{}", requestId);
                // 模拟API调用，随机失败
                if (Math.random() > 0.7) {
                    throw new RuntimeException("API调用失败");
                }
            });
        }

        // 打印统计信息
        log.info("请求完成，成功: {}, 失败: {}, 成功率: {:.2f}%",
                manager.getSuccessCount(), manager.getFailureCount(), manager.getSuccessRate() * 100);
    }
}
