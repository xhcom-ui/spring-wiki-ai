package com.example.health.service;

import com.example.health.datasource.ConnectionProxy;
import com.example.health.datasource.DataSourceProxy;
import com.example.health.metrics.ConnectionMetrics;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class ConnectionLeakDetector {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private ConnectionMetrics metrics;

    // 存储所有活跃的连接
    private final Map<Connection, ConnectionProxy> activeConnections = new ConcurrentHashMap<>();

    @Value("${connection.leak.detection.check-interval:60000}")
    private long checkInterval;

    @Value("${connection.leak.detection.alert-enabled:true}")
    private boolean alertEnabled;

    @Value("${connection.leak.detection.notify-url}")
    private String notifyUrl;

    @Value("${spring.datasource.hikari.leak-detection-threshold:60000}")
    private long leakDetectionThreshold;

    @PostConstruct
    public void init() {
        // 启动定时任务，检测连接泄漏
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(() -> {
            try {
                detectConnectionLeaks();
            } catch (Exception e) {
                log.error("检测连接泄漏失败", e);
            }
        }, 0, checkInterval, TimeUnit.MILLISECONDS);
    }

    /**
     * 注册活跃连接
     */
    public void registerConnection(Connection connection) {
        if (connection instanceof ConnectionProxy) {
            activeConnections.put(connection, (ConnectionProxy) connection);
            // 更新活跃连接数
            metrics.updateActiveConnectionsCount(activeConnections.size());
        }
    }

    /**
     * 移除活跃连接
     */
    public void removeConnection(Connection connection) {
        activeConnections.remove(connection);
        // 更新活跃连接数
        metrics.updateActiveConnectionsCount(activeConnections.size());
    }

    /**
     * 检测连接泄漏
     */
    public void detectConnectionLeaks() {
        log.info("开始检测连接泄漏");
        try {
            // 检查活跃连接
            for (Map.Entry<Connection, ConnectionProxy> entry : activeConnections.entrySet()) {
                Connection connection = entry.getKey();
                ConnectionProxy connectionProxy = entry.getValue();
                long usageTime = connectionProxy.getUsageTime();
                // 检查连接使用时间是否超过阈值
                if (usageTime > leakDetectionThreshold) {
                    log.warn("检测到连接泄漏: 使用时间={}ms, 阈值={}ms", usageTime, leakDetectionThreshold);
                    log.warn("连接获取堆栈: {}", connectionProxy.getStackTrace());
                    // 增加连接泄漏计数
                    metrics.incrementConnectionLeakCount();
                    // 触发告警
                    if (alertEnabled) {
                        String message = String.format("【数据库连接泄漏告警】\n使用时间: %dms\n阈值: %dms\n获取堆栈: %s",
                                usageTime, leakDetectionThreshold, connectionProxy.getStackTrace());
                        sendAlert(message);
                    }
                    // 尝试回收连接
                    try {
                        connection.close();
                        removeConnection(connection);
                        log.info("已回收泄漏的连接");
                    } catch (SQLException e) {
                        log.error("回收连接失败", e);
                    }
                }
            }
            // 获取 Hikari 数据源状态
            if (dataSource instanceof DataSourceProxy) {
                DataSourceProxy dataSourceProxy = (DataSourceProxy) dataSource;
                // 这里可以获取原始数据源并检查状态
            } else if (dataSource instanceof HikariDataSource) {
                HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
                HikariPoolMXBean poolMXBean = hikariDataSource.getHikariPoolMXBean();
                // 获取连接池状态
                int activeConnections = poolMXBean.getActiveConnections();
                int idleConnections = poolMXBean.getIdleConnections();
                int totalConnections = poolMXBean.getTotalConnections();
                int threadsAwaitingConnection = poolMXBean.getThreadsAwaitingConnection();
                log.info("连接池状态: 活跃连接={}, 空闲连接={}, 总连接={}, 等待连接的线程数={}",
                        activeConnections, idleConnections, totalConnections, threadsAwaitingConnection);
            }
        } catch (Exception e) {
            log.error("检测连接泄漏失败", e);
        }
        log.info("连接泄漏检测完成");
    }

    /**
     * 发送告警
     */
    private void sendAlert(String message) {
        try {
            // 发送告警
            if (alertEnabled && !notifyUrl.isEmpty()) {
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(notifyUrl))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString("{\"msgtype\":\"text\",\"text\":{\"content\":\"" + message + "\"}}"))
                        .build();
                client.send(request, HttpResponse.BodyHandlers.ofString());
                log.info("告警发送成功");
            } else {
                log.info("告警地址未配置，仅记录日志: {}", message);
            }
        } catch (Exception e) {
            log.error("发送告警失败", e);
        }
    }
}
