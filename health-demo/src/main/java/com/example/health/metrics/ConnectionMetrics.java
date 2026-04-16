package com.example.health.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class ConnectionMetrics {

    @Autowired
    private MeterRegistry meterRegistry;

    private Counter connectionAcquireCounter;
    private Counter connectionReleaseCounter;
    private Counter connectionLeakCounter;
    private AtomicInteger activeConnectionsCount = new AtomicInteger(0);

    @PostConstruct
    public void init() {
        // 初始化连接获取计数器
        connectionAcquireCounter = Counter.builder("connection.acquire.count")
                .description("数据库连接获取数量")
                .tag("type", "connection")
                .register(meterRegistry);

        // 初始化连接释放计数器
        connectionReleaseCounter = Counter.builder("connection.release.count")
                .description("数据库连接释放数量")
                .tag("type", "connection")
                .register(meterRegistry);

        // 初始化连接泄漏计数器
        connectionLeakCounter = Counter.builder("connection.leak.count")
                .description("数据库连接泄漏数量")
                .tag("type", "connection")
                .register(meterRegistry);

        // 初始化活跃连接 gauge
        Gauge.builder("connection.active.count", activeConnectionsCount, AtomicInteger::get)
                .description("当前活跃连接数")
                .tag("type", "connection")
                .register(meterRegistry);
    }

    /**
     * 增加连接获取计数
     */
    public void incrementConnectionAcquireCount() {
        connectionAcquireCounter.increment();
    }

    /**
     * 增加连接释放计数
     */
    public void incrementConnectionReleaseCount() {
        connectionReleaseCounter.increment();
    }

    /**
     * 增加连接泄漏计数
     */
    public void incrementConnectionLeakCount() {
        connectionLeakCounter.increment();
    }

    /**
     * 更新活跃连接数
     */
    public void updateActiveConnectionsCount(int count) {
        activeConnectionsCount.set(count);
    }
}
