package org.wiki.service;

import cn.hutool.core.util.RuntimeUtil;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class HealthMonitorService {
    
    private static final double MEMORY_THRESHOLD = 80.0;
    private static final DateTimeFormatter FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final List<MemorySnapshot> snapshots = new ArrayList<>();
    
    public static void main(String[] args) {
        System.out.println("========== JVM健康监控服务启动 ==========");
        System.out.println("进程ID：" + RuntimeUtil.getPid());
        System.out.println("CPU核心数：" + RuntimeUtil.getProcessorCount());
        System.out.println("内存告警阈值：" + MEMORY_THRESHOLD + "%");
        System.out.println("==========================================\n");
        
        // 注册关闭钩子，保存监控数据
        RuntimeUtil.addShutdownHook(() -> {
            System.out.println("\n监控服务关闭，保存监控数据...");
            saveMonitorData();
        });
        
        // 创建定时任务执行器
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        
        // 每5秒执行一次健康检查
        scheduler.scheduleAtFixedRate(() -> {
            try {
                performHealthCheck();
            } catch (Exception e) {
                System.err.println("健康检查异常：" + e.getMessage());
            }
        }, 0, 5, TimeUnit.SECONDS);
        
        // 模拟应用运行，制造内存压力
        simulateMemoryPressure();
    }
    
    private static void performHealthCheck() {
        long maxMemory = RuntimeUtil.getMaxMemory();
        long totalMemory = RuntimeUtil.getTotalMemory();
        long freeMemory = RuntimeUtil.getFreeMemory();
        long usedMemory = totalMemory - freeMemory;
        double usagePercent = (double) usedMemory / maxMemory * 100;
        
        String timestamp = LocalDateTime.now().format(FORMATTER);
        
        // 记录快照
        MemorySnapshot snapshot = new MemorySnapshot(
            timestamp, maxMemory, totalMemory, freeMemory, usedMemory, usagePercent
        );
        snapshots.add(snapshot);
        
        // 输出监控信息
        System.out.printf("[%s] 内存使用：%.2f MB / %.2f MB (%.1f%%)\n",
            timestamp,
            usedMemory / (1024.0 * 1024),
            maxMemory / (1024.0 * 1024),
            usagePercent);
        
        // 检查是否超过阈值
        if (usagePercent > MEMORY_THRESHOLD) {
            triggerAlert(snapshot);
        }
    }
    
    private static void triggerAlert(MemorySnapshot snapshot) {
        System.out.println("============ 内存告警 ============");
        System.out.println("告警时间：" + snapshot.timestamp);
        System.out.println("当前使用率：" + String.format("%.1f%%", snapshot.usagePercent));
        System.out.println("告警阈值：" + MEMORY_THRESHOLD + "%");
        System.out.println("建议：请检查是否存在内存泄漏或考虑扩容");
        System.out.println("==================================");
        
        // 这里可以添加发送邮件、短信等告警通知逻辑
    }
    
    private static void simulateMemoryPressure() {
        List<byte[]> memoryHolder = new ArrayList<>();
        
        // 模拟内存增长
        for (int i = 0; i < 10; i++) {
            try {
                Thread.sleep(3000);
                // 每次分配10MB内存
                memoryHolder.add(new byte[10 * 1024 * 1024]);
                System.out.println(">>> 模拟分配10MB内存，当前持有：" + memoryHolder.size() * 10 + "MB");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (OutOfMemoryError e) {
                System.out.println(">>> 内存不足，停止分配");
                break;
            }
        }
    }
    
    private static void saveMonitorData() {
        System.out.println("共记录 " + snapshots.size() + " 条监控数据");
        
        if (!snapshots.isEmpty()) {
            // 计算统计信息
            double avgUsage = snapshots.stream()
                .mapToDouble(s -> s.usagePercent)
                .average()
                .orElse(0);
            double maxUsage = snapshots.stream()
                .mapToDouble(s -> s.usagePercent)
                .max()
                .orElse(0);
            
            System.out.printf("平均内存使用率：%.1f%%\n", avgUsage);
            System.out.printf("最高内存使用率：%.1f%%\n", maxUsage);
        }
    }
    
    static class MemorySnapshot {
        String timestamp;
        long maxMemory;
        long totalMemory;
        long freeMemory;
        long usedMemory;
        double usagePercent;
        
        MemorySnapshot(String timestamp, long maxMemory, long totalMemory,
                       long freeMemory, long usedMemory, double usagePercent) {
            this.timestamp = timestamp;
            this.maxMemory = maxMemory;
            this.totalMemory = totalMemory;
            this.freeMemory = freeMemory;
            this.usedMemory = usedMemory;
            this.usagePercent = usagePercent;
        }
    }
}