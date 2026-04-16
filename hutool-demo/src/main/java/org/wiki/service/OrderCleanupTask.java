package org.wiki.service;

import cn.hutool.core.thread.ThreadUtil;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class OrderCleanupTask {

//    @Autowired
//    private OrderService orderService;

    // 定时任务线程池
    private ScheduledThreadPoolExecutor executor;

    @PostConstruct
    public void init() {
        // 创建定时任务线程池
        executor = new ScheduledThreadPoolExecutor(1);

        // 启动定时清理任务
        startCleanupTask();

        log.info("订单清理定时任务已启动");
    }

    /**
     * 启动定时清理任务
     */
    private void startCleanupTask() {
        // 延迟 10 秒后开始，每隔 1 分钟执行一次
        ThreadUtil.schedule(executor, () -> {
            try {
                log.info("开始清理过期订单：{}", LocalDateTime.now());

                // 查询并清理过期订单
                //int cleanedCount = orderService.cleanExpiredOrders();
                int cleanedCount = 0;

                log.info("清理完成，共清理 {} 个过期订单", cleanedCount);

            } catch (Exception e) {
                log.error("清理过期订单失败", e);
            }
        }, 10, 60, TimeUnit.SECONDS, true);
    }

    /**
     * 停止定时任务
     */
    public void shutdown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
            log.info("订单清理定时任务已停止");
        }
    }
}