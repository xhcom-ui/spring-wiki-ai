package org.wiki.service;

import cn.hutool.core.thread.ConcurrencyTester;
import cn.hutool.core.thread.ThreadUtil;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadService {

    private static int counter = 0;

    public static void main(String[] args) {
        // 创建并发测试器，100 个线程同时执行
        ConcurrencyTester tester = ThreadUtil.concurrencyTest(100, () -> {
            // 模拟业务逻辑
            for (int i = 0; i < 1000; i++) {
                counter++;
            }
        });

        // 获取测试结果
        System.out.println("总耗时：" + tester.getInterval() + " ms");
        System.out.println("最终计数：" + counter);
        System.out.println("预期计数：" + (100 * 1000));

        // 测试普通 int（非线程安全）
        int[] normalCounter = {0};
        ThreadUtil.concurrencyTest(50, () -> {
            for (int i = 0; i < 1000; i++) {
                normalCounter[0]++;
            }
        });
        System.out.println("普通 int 结果：" + normalCounter[0]);
        // 输出可能小于 50000，说明存在线程安全问题

        // 测试 AtomicInteger（线程安全）
        AtomicInteger atomicCounter = new AtomicInteger(0);
        ThreadUtil.concurrencyTest(50, () -> {
            for (int i = 0; i < 1000; i++) {
                atomicCounter.incrementAndGet();
            }
        });
        System.out.println("AtomicInteger 结果：" + atomicCounter.get());
        // 输出必定是 50000，说明线程安全


        // 创建定时任务线程池
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(2);

        // fixedRate 模式：固定频率执行
        // 延迟 1000ms 后开始，每隔 2000ms 执行一次
        ThreadUtil.schedule(executor, () -> {
            System.out.println("fixedRate 任务执行：" + System.currentTimeMillis());
        }, 1000, 2000, true);

        // fixedDelay 模式：固定延迟执行
        // 延迟 1000ms 后开始，每次执行完成后延迟 2000ms 再执行
        ThreadUtil.schedule(executor, () -> {
            System.out.println("fixedDelay 任务执行：" + System.currentTimeMillis());
            ThreadUtil.sleep(500); // 模拟任务耗时
        }, 1000, 2000, false);

        // 主线程等待，观察定时任务执行
        ThreadUtil.sleep(10, java.util.concurrent.TimeUnit.SECONDS);
        executor.shutdown();
    }
}
