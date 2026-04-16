package com.example.skapi;

import java.util.Random;

public class ExponentialBackoffRetry {
    private final int maxRetries;
    private final Random random;

    public ExponentialBackoffRetry(int maxRetries) {
        this.maxRetries = maxRetries;
        this.random = new Random();
    }

    public boolean execute(Runnable task) {
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                System.out.println("尝试第" + (attempt + 1) + "次调用API");
                task.run();
                System.out.println("API调用成功");
                return true;
            } catch (Exception e) {
                if (attempt == maxRetries - 1) {
                    System.out.println("达到最大重试次数，失败");
                    return false;
                }
                // 指数退避 + 随机抖动
                double sleepTime = Math.pow(2, attempt) + random.nextDouble();
                System.out.println("等待" + sleepTime + "秒后重试");
                try {
                    Thread.sleep((long) (sleepTime * 1000));
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }
        return false;
    }

    // 示例使用
    public static void main(String[] args) {
        ExponentialBackoffRetry retry = new ExponentialBackoffRetry(3);
        retry.execute(() -> {
            // 模拟API调用，前两次故意抛出异常
            if (Math.random() > 0.5) {
                throw new RuntimeException("限流错误");
            }
        });
    }
}
