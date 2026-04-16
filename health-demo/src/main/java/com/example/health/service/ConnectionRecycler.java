package com.example.health.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class ConnectionRecycler {

    @Autowired
    private ConnectionLeakDetector connectionLeakDetector;

    @Value("${connection.leak.detection.check-interval:60000}")
    private long checkInterval;

    @PostConstruct
    public void init() {
        // 启动定时任务，回收泄漏的连接
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(() -> {
            try {
                recycleConnections();
            } catch (Exception e) {
                log.error("回收连接失败", e);
            }
        }, 0, checkInterval, TimeUnit.MILLISECONDS);
    }

    /**
     * 回收泄漏的连接
     */
    public void recycleConnections() {
        log.info("开始回收泄漏的连接");
        try {
            // 调用连接泄漏检测器检测并回收连接
            connectionLeakDetector.detectConnectionLeaks();
        } catch (Exception e) {
            log.error("回收连接失败", e);
        }
        log.info("连接回收完成");
    }
}
