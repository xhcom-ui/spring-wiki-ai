package com.example.skapi;

import java.util.concurrent.locks.ReentrantLock;

public class TokenBucket {
    private final double capacity;
    private final double fillRate;
    private double tokens;
    private long lastRefillTime;
    private final ReentrantLock lock = new ReentrantLock();

    public TokenBucket(double capacity, double fillRate) {
        this.capacity = capacity;
        this.fillRate = fillRate;
        this.tokens = capacity;
        this.lastRefillTime = System.currentTimeMillis();
    }

    private void refill() {
        long now = System.currentTimeMillis();
        double deltaTime = (now - lastRefillTime) / 1000.0;
        double deltaTokens = deltaTime * fillRate;
        lock.lock();
        try {
            tokens = Math.min(capacity, tokens + deltaTokens);
            lastRefillTime = now;
        } finally {
            lock.unlock();
        }
    }

    public boolean acquire(double tokensToAcquire) {
        refill();
        lock.lock();
        try {
            if (tokens >= tokensToAcquire) {
                tokens -= tokensToAcquire;
                return true;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    // 示例使用
    public static void main(String[] args) throws InterruptedException {
        TokenBucket bucket = new TokenBucket(100, 50); // 容量100，每秒填充50个令牌
        for (int i = 0; i < 200; i++) {
            if (bucket.acquire(1)) {
                System.out.println("请求" + i + "：获取令牌，执行API调用");
            } else {
                System.out.println("请求" + i + "：令牌不足，排队等待");
            }
            Thread.sleep(10);
        }
    }
}
