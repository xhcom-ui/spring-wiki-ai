package com.example.skapi;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
public class APIKeyPool {
    private List<String> apiKeys;
    private int index;
    private Random random;

    public APIKeyPool(List<String> apiKeys) {
        if (apiKeys == null || apiKeys.isEmpty()) {
            throw new IllegalArgumentException("API Key 列表不能为空");
        }
        this.apiKeys = new CopyOnWriteArrayList<>(apiKeys);
        this.index = 0;
        this.random = new Random();
        log.info("APIKeyPool 初始化成功，API Key 数量: {}", apiKeys.size());
    }

    // 轮询获取下一个 Key
    public String getNextKey() {
        if (apiKeys.isEmpty()) {
            throw new IllegalStateException("API Key 池为空");
        }
        synchronized (this) {
            String key = apiKeys.get(index);
            index = (index + 1) % apiKeys.size();
            return key;
        }
    }

    // 随机获取 Key
    public String getRandomKey() {
        if (apiKeys.isEmpty()) {
            throw new IllegalStateException("API Key 池为空");
        }
        return apiKeys.get(random.nextInt(apiKeys.size()));
    }

    // 动态添加 API Key
    public void addKey(String apiKey) {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalArgumentException("API Key 不能为空");
        }
        if (!apiKeys.contains(apiKey)) {
            apiKeys.add(apiKey);
            log.info("添加API Key: {}, 当前数量: {}", apiKey, apiKeys.size());
        } else {
            log.warn("API Key 已存在: {}", apiKey);
        }
    }

    // 动态移除 API Key
    public void removeKey(String apiKey) {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalArgumentException("API Key 不能为空");
        }
        if (apiKeys.remove(apiKey)) {
            log.info("移除API Key: {}, 当前数量: {}", apiKey, apiKeys.size());
            // 重置索引，避免数组越界
            synchronized (this) {
                if (index >= apiKeys.size() && !apiKeys.isEmpty()) {
                    index = 0;
                }
            }
        } else {
            log.warn("API Key 不存在: {}", apiKey);
        }
    }

    // 获取 API Key 数量
    public int size() {
        return apiKeys.size();
    }

    // 检查 API Key 是否存在
    public boolean contains(String apiKey) {
        return apiKeys.contains(apiKey);
    }

    // 示例使用
    public static void main(String[] args) {
        List<String> keys = List.of("key1", "key2", "key3");
        APIKeyPool keyPool = new APIKeyPool(keys);
        
        // 测试轮询获取
        log.info("测试轮询获取:");
        for (int i = 0; i < 5; i++) {
            log.info(keyPool.getNextKey());
        }
        
        // 测试添加 API Key
        keyPool.addKey("key4");
        
        // 测试随机获取
        log.info("测试随机获取:");
        for (int i = 0; i < 3; i++) {
            log.info(keyPool.getRandomKey());
        }
        
        // 测试移除 API Key
        keyPool.removeKey("key2");
        
        // 测试轮询获取
        log.info("测试轮询获取 (移除后):");
        for (int i = 0; i < 5; i++) {
            log.info(keyPool.getNextKey());
        }
    }
}
