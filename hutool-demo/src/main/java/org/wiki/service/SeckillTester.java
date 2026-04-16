package org.wiki.service;

import cn.hutool.core.thread.ConcurrencyTester;
import cn.hutool.core.thread.ThreadUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class SeckillTester {

    /**
     * 测试秒杀接口的并发性能
     */
    public static TestResult testSeckill(
            SeckillService seckillService,
            int threadCount,
            int initialStock,
            String testName) {

        log.info("========== {} 开始 ==========", testName);
        log.info("并发线程数：{}", threadCount);
        log.info("初始库存：{}", initialStock);

        // 重置数据
        seckillService.reset(initialStock);

        // 用户 ID 生成器
        AtomicLong userIdGenerator = new AtomicLong(1);

        // 记录开始时间
        long startTime = System.currentTimeMillis();

        // 执行并发测试
        ConcurrencyTester tester = ThreadUtil.concurrencyTest(threadCount, () -> {
            Long userId = userIdGenerator.getAndIncrement();
            Long productId = 1001L;

            // 调用秒杀接口
            seckillService.seckillOptimistic(userId, productId);
        });

        // 记录结束时间
        long endTime = System.currentTimeMillis();

        // 获取统计信息
        SeckillStats stats = seckillService.getStats();

        // 构建测试结果
        TestResult result = new TestResult();
        result.setTestName(testName);
        result.setThreadCount(threadCount);
        result.setInitialStock(initialStock);
        result.setRemainingStock(stats.getRemainingStock());
        result.setSuccessCount(stats.getSuccessCount());
        result.setFailCount(stats.getFailCount());
        result.setTotalTime(endTime - startTime);
        result.setAvgTime(tester.getInterval() / threadCount);

        // 输出测试结果
        log.info("剩余库存：{}", result.getRemainingStock());
        log.info("成功抢购：{}", result.getSuccessCount());
        log.info("失败次数：{}", result.getFailCount());
        log.info("总耗时：{} ms", result.getTotalTime());
        log.info("平均耗时：{} ms", result.getAvgTime());
        log.info("TPS：{}", (threadCount * 1000.0 / result.getTotalTime()));
        log.info("========== {} 结束 ==========\n", testName);

        return result;
    }

    /**
     * 对比测试不同方案
     */
    public static void compareTest(SeckillService seckillService) {
        List<TestResult> results = new ArrayList<>();

        // 测试 1：100 并发，100 库存
        results.add(testSeckill(seckillService, 100, 100, "测试1：100并发-100库存"));

        // 测试 2：500 并发，100 库存
        results.add(testSeckill(seckillService, 500, 100, "测试2：500并发-100库存"));

        // 测试 3：1000 并发，100 库存
        results.add(testSeckill(seckillService, 1000, 100, "测试3：1000并发-100库存"));

        // 测试 4：5000 并发，100 库存（极限压测）
        results.add(testSeckill(seckillService, 5000, 100, "测试4：5000并发-100库存"));

        // 输出对比报告
        printCompareReport(results);
    }

    /**
     * 输出对比报告
     */
    private static void printCompareReport(List<TestResult> results) {
        log.info("========== 对比报告 ==========");
        log.info(String.format("%-20s %-10s %-10s %-10s %-10s %-10s",
                "测试名称", "并发数", "成功数", "失败数", "总耗时(ms)", "TPS"));

        for (TestResult result : results) {
            double tps = result.getThreadCount() * 1000.0 / result.getTotalTime();
            log.info(String.format("%-20s %-10d %-10d %-10d %-10d %-10.2f",
                    result.getTestName(),
                    result.getThreadCount(),
                    result.getSuccessCount(),
                    result.getFailCount(),
                    result.getTotalTime(),
                    tps));
        }

        log.info("==============================\n");
    }
}

// 测试结果实体
@Data
class TestResult {
    private String testName;
    private Integer threadCount;
    private Integer initialStock;
    private Integer remainingStock;
    private Integer successCount;
    private Integer failCount;
    private Long totalTime;
    private Long avgTime;
}