package com.syn.data.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.syn.data.entity.SyncTaskConfig;
import com.syn.data.entity.SyncTaskLog;
import com.syn.data.entity.DataSourceConfig;
import com.syn.data.entity.WatcherConfig;
import com.syn.data.mapper.AlertLogMapper;
import com.syn.data.mapper.DataSourceConfigMapper;
import com.syn.data.mapper.SyncTaskConfigMapper;
import com.syn.data.mapper.SyncTaskLogMapper;
import com.syn.data.mapper.WatcherConfigMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MonitoringService {

    @Resource
    private SyncTaskLogMapper syncTaskLogMapper;

    @Resource
    private SyncTaskConfigMapper syncTaskConfigMapper;

    @Resource
    private DataSourceConfigMapper dataSourceConfigMapper;

    @Resource
    private WatcherConfigMapper watcherConfigMapper;

    @Resource
    private AlertLogMapper alertLogMapper;

    public Map<String, Object> getRealTimeMonitoringData() {
        Map<String, Object> result = new HashMap<>();
        result.put("statusStats", getTaskStatusStats());
        result.put("recentTasks", getRecentTasks(10));
        result.put("speedStats", getExecutionSpeedStats());
        result.put("resourceUsage", getResourceUsage());
        result.put("queueLength", getQueueLength());
        result.put("summary", buildDashboardSummary());
        return result;
    }

    public Map<String, Object> getTaskLogs(int page, int size, Long taskId, String status) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(size, 1);
        QueryWrapper<SyncTaskLog> wrapper = new QueryWrapper<SyncTaskLog>()
                .orderByDesc("start_time")
                .orderByDesc("id");
        if (taskId != null) {
            wrapper.eq("task_id", taskId);
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq("status", status);
        }

        long total = syncTaskLogMapper.selectCount(wrapper);
        wrapper.last("limit " + safeSize + " offset " + ((safePage - 1) * safeSize));
        List<SyncTaskLog> records = syncTaskLogMapper.selectList(wrapper);
        if (!records.isEmpty()) {
            Map<Long, SyncTaskConfig> taskMap = syncTaskConfigMapper.selectBatchIds(
                    records.stream().map(SyncTaskLog::getTaskId).filter(Objects::nonNull).distinct().toList()
            ).stream().collect(Collectors.toMap(SyncTaskConfig::getId, item -> item));
            records.forEach(log -> {
                SyncTaskConfig task = taskMap.get(log.getTaskId());
                if (task != null) {
                    log.setSyncMode(task.getSyncMode());
                }
            });
        }

        Map<String, Object> result = new HashMap<>();
        result.put("list", records);
        result.put("total", total);
        result.put("page", safePage);
        result.put("size", safeSize);
        return result;
    }

    public Map<String, Long> getTaskStatusStats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("running", syncTaskLogMapper.selectCount(new QueryWrapper<SyncTaskLog>().eq("status", "running")));
        stats.put("success", syncTaskLogMapper.selectCount(new QueryWrapper<SyncTaskLog>().eq("status", "success")));
        stats.put("failed", syncTaskLogMapper.selectCount(new QueryWrapper<SyncTaskLog>().eq("status", "failed")));
        stats.put("pending", syncTaskConfigMapper.selectCount(new QueryWrapper<SyncTaskConfig>().eq("status", 1)));
        return stats;
    }

    public List<SyncTaskLog> getRecentTasks(int limit) {
        QueryWrapper<SyncTaskLog> wrapper = new QueryWrapper<SyncTaskLog>()
                .orderByDesc("start_time")
                .orderByDesc("id");
        if (limit > 0) {
            wrapper.last("limit " + limit);
        }
        List<SyncTaskLog> logs = syncTaskLogMapper.selectList(wrapper);
        if (logs.isEmpty()) {
            return logs;
        }
        Map<Long, SyncTaskConfig> taskMap = syncTaskConfigMapper.selectBatchIds(
                logs.stream().map(SyncTaskLog::getTaskId).filter(Objects::nonNull).distinct().toList()
        ).stream().collect(Collectors.toMap(SyncTaskConfig::getId, item -> item));
        logs.forEach(log -> {
            SyncTaskConfig task = taskMap.get(log.getTaskId());
            if (task != null) {
                log.setSyncMode(task.getSyncMode());
            }
        });
        return logs;
    }

    public Map<String, Object> getExecutionSpeedStats() {
        List<SyncTaskLog> logs = syncTaskLogMapper.selectList(
                new QueryWrapper<SyncTaskLog>()
                        .eq("status", "success")
                        .isNotNull("duration")
                        .gt("duration", 0)
                        .orderByDesc("start_time")
                        .last("limit 20")
        );
        Map<String, Object> stats = new HashMap<>();
        if (logs.isEmpty()) {
            stats.put("averageSpeed", 0);
            stats.put("maxSpeed", 0);
            stats.put("minSpeed", 0);
            stats.put("trend", "stable");
            return stats;
        }
        List<Long> speeds = logs.stream()
                .map(this::calculateSpeed)
                .filter(speed -> speed > 0)
                .toList();
        stats.put("averageSpeed", speeds.stream().mapToLong(Long::longValue).sum() / Math.max(1, speeds.size()));
        stats.put("maxSpeed", speeds.stream().mapToLong(Long::longValue).max().orElse(0L));
        stats.put("minSpeed", speeds.stream().mapToLong(Long::longValue).min().orElse(0L));
        stats.put("trend", resolveTrend(speeds));
        return stats;
    }

    public Map<String, Object> getResourceUsage() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long usedMemory = totalMemory - runtime.freeMemory();
        double memoryUsage = totalMemory == 0 ? 0D : (usedMemory * 100.0 / totalMemory);
        OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();

        Map<String, Object> usage = new HashMap<>();
        usage.put("cpuUsage", 0D);
        usage.put("memoryUsage", Math.round(memoryUsage * 10) / 10.0);
        usage.put("diskUsage", 0D);
        usage.put("networkIn", getQueueLength());
        usage.put("networkOut", getRecentTasks(5).stream().mapToLong(item -> item.getSuccessCount() == null ? 0L : item.getSuccessCount()).sum());
        usage.put("systemLoad", operatingSystemMXBean.getSystemLoadAverage());
        return usage;
    }

    public int getQueueLength() {
        return watcherConfigMapper.selectList(new QueryWrapper<WatcherConfig>())
                .stream()
                .mapToInt(item -> item.getQueueSize() == null ? 0 : item.getQueueSize().intValue())
                .sum();
    }

    public Map<String, Object> getHistoryMonitoringData(String timeRange) {
        Map<String, Object> result = new HashMap<>();
        result.put("trendData", getExecutionTrend(timeRange));
        result.put("performanceAnalysis", getPerformanceAnalysis(timeRange));
        result.put("successRate", getSuccessRate(timeRange));
        result.put("executionTimeDistribution", getExecutionTimeDistribution(timeRange));
        return result;
    }

    public List<Map<String, Object>> getExecutionTrend(String timeRange) {
        LocalDateTime startTime = resolveStartTime(timeRange);
        List<SyncTaskLog> logs = listLogsSince(startTime);
        Map<LocalDate, List<SyncTaskLog>> grouped = logs.stream()
                .collect(Collectors.groupingBy(item -> item.getStartTime().toLocalDate()));

        List<Map<String, Object>> trendData = new ArrayList<>();
        for (LocalDate cursor = startTime.toLocalDate(); !cursor.isAfter(LocalDate.now()); cursor = cursor.plusDays(1)) {
            List<SyncTaskLog> dayLogs = grouped.getOrDefault(cursor, List.of());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("date", cursor.toString());
            row.put("successCount", dayLogs.stream().filter(item -> "success".equalsIgnoreCase(item.getStatus())).count());
            row.put("failedCount", dayLogs.stream().filter(item -> "failed".equalsIgnoreCase(item.getStatus())).count());
            row.put("totalCount", dayLogs.size());
            trendData.add(row);
        }
        return trendData;
    }

    public Map<String, Object> getPerformanceAnalysis(String timeRange) {
        List<SyncTaskLog> logs = listLogsSince(resolveStartTime(timeRange));
        Map<String, Object> analysis = new HashMap<>();
        analysis.put("averageExecutionTime", Math.round(logs.stream()
                .filter(item -> item.getDuration() != null)
                .mapToLong(SyncTaskLog::getDuration)
                .average()
                .orElse(0)));
        analysis.put("averageThroughput", logs.stream().map(this::calculateSpeed).mapToLong(Long::longValue).sum() / Math.max(1, logs.size()));
        analysis.put("bottlenecks", logs.isEmpty() ? Arrays.asList("暂无执行数据") : Arrays.asList("关注慢 SQL", "检查 Elasticsearch 写入吞吐"));
        analysis.put("optimizationSuggestions", Arrays.asList("优化 SQL 过滤条件", "合理设置 batchSize 与 queryTimeout"));
        return analysis;
    }

    public Map<String, Object> getSuccessRate(String timeRange) {
        List<SyncTaskLog> logs = listLogsSince(resolveStartTime(timeRange));
        long total = logs.size();
        long success = logs.stream().filter(item -> "success".equalsIgnoreCase(item.getStatus())).count();
        long failed = logs.stream().filter(item -> "failed".equalsIgnoreCase(item.getStatus())).count();
        Map<String, Object> successRate = new HashMap<>();
        successRate.put("rate", total == 0 ? 0D : Math.round(success * 10000.0 / total) / 100.0);
        successRate.put("totalExecutions", total);
        successRate.put("successExecutions", success);
        successRate.put("failedExecutions", failed);
        return successRate;
    }

    public Map<String, Object> getExecutionTimeDistribution(String timeRange) {
        List<SyncTaskLog> logs = listLogsSince(resolveStartTime(timeRange));
        Map<String, Object> distribution = new HashMap<>();
        distribution.put("0-10s", logs.stream().filter(item -> inRange(item.getDuration(), 0, 10_000)).count());
        distribution.put("10-30s", logs.stream().filter(item -> inRange(item.getDuration(), 10_000, 30_000)).count());
        distribution.put("30-60s", logs.stream().filter(item -> inRange(item.getDuration(), 30_000, 60_000)).count());
        distribution.put("60s+", logs.stream().filter(item -> item.getDuration() != null && item.getDuration() >= 60_000).count());
        return distribution;
    }

    public Map<String, Object> getTaskExecutionDetail(Long taskId, Long logId) {
        SyncTaskLog logRecord = syncTaskLogMapper.selectOne(
                new QueryWrapper<SyncTaskLog>()
                        .eq("task_id", taskId)
                        .eq("id", logId)
                        .last("limit 1")
        );
        if (logRecord == null) {
            throw new RuntimeException("任务执行记录不存在");
        }
        SyncTaskConfig task = syncTaskConfigMapper.selectById(taskId);

        Map<String, Object> detail = new HashMap<>();
        detail.put("taskId", taskId);
        detail.put("logId", logId);
        detail.put("taskName", logRecord.getTaskName());
        detail.put("syncMode", task != null ? task.getSyncMode() : null);
        detail.put("status", logRecord.getStatus());
        detail.put("startTime", logRecord.getStartTime());
        detail.put("endTime", logRecord.getEndTime());
        detail.put("duration", logRecord.getDuration());
        detail.put("sourceCount", logRecord.getTotalCount());
        detail.put("targetCount", logRecord.getSuccessCount());
        detail.put("errorCount", logRecord.getFailedCount());
        detail.put("errorMessages", logRecord.getErrorMessage() == null ? List.of() : List.of(logRecord.getErrorMessage()));
        detail.put("executionParams", logRecord.getExecutionParams());
        detail.put("executionSteps", buildExecutionSteps(logRecord));
        return detail;
    }

    public Map<String, Object> exportMonitoringData(String timeRange, String format) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "监控数据已整理完成");
        result.put("format", format);
        result.put("generatedAt", LocalDateTime.now());
        result.put("summary", getHistoryMonitoringData(timeRange));
        result.put("alerts", alertLogMapper.selectCount(new QueryWrapper<>()));
        return result;
    }

    private List<SyncTaskLog> listLogsSince(LocalDateTime startTime) {
        return syncTaskLogMapper.selectList(
                new QueryWrapper<SyncTaskLog>()
                        .ge("start_time", startTime)
                        .orderByDesc("start_time")
                        .orderByDesc("id")
        );
    }

    private LocalDateTime resolveStartTime(String timeRange) {
        if (timeRange == null || timeRange.isBlank()) {
            return LocalDateTime.now().minusDays(7);
        }
        String value = timeRange.trim().toLowerCase(Locale.ROOT);
        if (value.endsWith("d")) {
            return LocalDateTime.now().minusDays(Long.parseLong(value.substring(0, value.length() - 1)));
        }
        if (value.endsWith("h")) {
            return LocalDateTime.now().minusHours(Long.parseLong(value.substring(0, value.length() - 1)));
        }
        return LocalDateTime.now().minusDays(7);
    }

    private long calculateSpeed(SyncTaskLog log) {
        if (log.getDuration() == null || log.getDuration() <= 0 || log.getTotalCount() == null) {
            return 0L;
        }
        return Math.round(log.getTotalCount() * 1000.0 / log.getDuration());
    }

    private String resolveTrend(List<Long> speeds) {
        if (speeds.size() < 2) {
            return "stable";
        }
        int split = Math.max(1, speeds.size() / 2);
        double latest = speeds.subList(0, split).stream().mapToLong(Long::longValue).average().orElse(0);
        double previous = speeds.subList(split, speeds.size()).stream().mapToLong(Long::longValue).average().orElse(latest);
        if (latest > previous * 1.1) {
            return "up";
        }
        if (latest < previous * 0.9) {
            return "down";
        }
        return "stable";
    }

    private boolean inRange(Long duration, long minInclusive, long maxExclusive) {
        return duration != null && duration >= minInclusive && duration < maxExclusive;
    }

    private List<String> buildExecutionSteps(SyncTaskLog logRecord) {
        List<String> steps = new ArrayList<>();
        steps.add("准备同步任务");
        steps.add("执行数据抽取");
        if ("failed".equalsIgnoreCase(logRecord.getStatus())) {
            steps.add("任务执行失败");
        } else if ("running".equalsIgnoreCase(logRecord.getStatus())) {
            steps.add("任务仍在运行中");
        } else {
            steps.add("写入目标索引");
            steps.add("完成同步任务");
        }
        return steps;
    }

    private Map<String, Object> buildDashboardSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("dataSourceCount", dataSourceConfigMapper.selectCount(new QueryWrapper<DataSourceConfig>()));
        summary.put("syncTaskCount", syncTaskConfigMapper.selectCount(new QueryWrapper<SyncTaskConfig>()));
        summary.put("watcherCount", watcherConfigMapper.selectCount(new QueryWrapper<WatcherConfig>()));
        summary.put("alertCount", alertLogMapper.selectCount(new QueryWrapper<>()));
        summary.put("successCount", syncTaskLogMapper.selectCount(new QueryWrapper<SyncTaskLog>().eq("status", "success")));
        summary.put("failureCount", syncTaskLogMapper.selectCount(new QueryWrapper<SyncTaskLog>().eq("status", "failed")));
        summary.put("recentSuccessCount", syncTaskLogMapper.selectCount(
                new QueryWrapper<SyncTaskLog>()
                        .eq("status", "success")
                        .ge("start_time", LocalDateTime.now().minusDays(1))
        ));
        summary.put("recentFailureCount", syncTaskLogMapper.selectCount(
                new QueryWrapper<SyncTaskLog>()
                        .eq("status", "failed")
                        .ge("start_time", LocalDateTime.now().minusDays(1))
        ));
        return summary;
    }
}
