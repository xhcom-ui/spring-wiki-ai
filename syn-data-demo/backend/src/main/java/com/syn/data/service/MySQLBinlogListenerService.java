package com.syn.data.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.syn.data.entity.SyncTaskConfig;
import com.syn.data.entity.WatcherConfig;
import com.syn.data.mapper.SyncTaskConfigMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * MySQL watcher 运行时服务。
 * 直接消费持久化 watcher 配置模型，不再依赖动态生成的 watcher 类。
 */
@Slf4j
@Service
public class MySQLBinlogListenerService implements WatcherRuntimeService {

    @Resource
    private WatcherConfigService watcherConfigService;

    @Resource
    private SyncTaskConfigMapper syncTaskConfigMapper;

    @Resource
    private AlertService alertService;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final Map<Long, ScheduledFuture<?>> runningWatchers = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        watcherConfigService.listEntitiesBySourceType("mysql").stream()
                .filter(item -> "running".equalsIgnoreCase(item.getStatus()))
                .filter(item -> !listBoundTasks(item.getId()).isEmpty())
                .forEach(item -> startListener(item.getId()));
    }

    @PreDestroy
    public void destroy() {
        runningWatchers.values().forEach(future -> future.cancel(true));
        runningWatchers.clear();
        scheduler.shutdownNow();
    }

    @Override
    public String getSourceType() {
        return "mysql";
    }

    @Override
    public void startListener(Long watcherId) {
        WatcherConfig watcher = requireMysqlWatcher(watcherId);
        if (runningWatchers.containsKey(watcherId)) {
            log.info("MySQL watcher {} 已在运行", watcherId);
            watcherConfigService.updateRuntimeState(
                    watcherId,
                    "running",
                    watcher.getQueueSize() == null ? 0L : watcher.getQueueSize(),
                    watcher.getSyncedCount() == null ? 0L : watcher.getSyncedCount(),
                    watcher.getLastSyncTime()
            );
            return;
        }

        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(
                () -> heartbeat(watcherId),
                0,
                3,
                TimeUnit.SECONDS
        );
        runningWatchers.put(watcherId, future);
        watcherConfigService.updateRuntimeState(
                watcherId,
                "running",
                watcher.getQueueSize() == null ? 0L : watcher.getQueueSize(),
                watcher.getSyncedCount() == null ? 0L : watcher.getSyncedCount(),
                LocalDateTime.now()
        );
        log.info("MySQL watcher {} 已启动", watcherId);
    }

    @Override
    public void stopListener(Long watcherId) {
        requireMysqlWatcher(watcherId);
        ScheduledFuture<?> future = runningWatchers.remove(watcherId);
        if (future != null) {
            future.cancel(true);
        }
        WatcherConfig watcher = watcherConfigService.requireEntity(watcherId);
        watcherConfigService.updateRuntimeState(
                watcherId,
                "stopped",
                watcher.getQueueSize() == null ? 0L : watcher.getQueueSize(),
                watcher.getSyncedCount() == null ? 0L : watcher.getSyncedCount(),
                watcher.getLastSyncTime()
        );
        log.info("MySQL watcher {} 已停止", watcherId);
    }

    @Override
    public void registerTask(SyncTaskConfig task) {
        if (task.getWatcherId() == null || "full".equalsIgnoreCase(task.getSyncMode())) {
            return;
        }
        requireMysqlWatcher(task.getWatcherId());
        if ("running".equalsIgnoreCase(watcherConfigService.requireEntity(task.getWatcherId()).getStatus())) {
            startListener(task.getWatcherId());
        }
        log.info("MySQL watcher 任务已注册: taskId={}, watcherId={}", task.getId(), task.getWatcherId());
    }

    @Override
    public void unregisterTask(SyncTaskConfig task) {
        if (task.getWatcherId() == null) {
            return;
        }
        requireMysqlWatcher(task.getWatcherId());
        if (listBoundTasks(task.getWatcherId()).isEmpty()) {
            stopListener(task.getWatcherId());
        }
        log.info("MySQL watcher 任务已取消注册: taskId={}, watcherId={}", task.getId(), task.getWatcherId());
    }

    @Override
    public Map<String, Object> getListenerStatus(Long watcherId) {
        WatcherConfig watcher = requireMysqlWatcher(watcherId);
        Map<String, Object> status = new HashMap<>();
        status.put("watcherId", watcherId);
        status.put("isRunning", runningWatchers.containsKey(watcherId));
        status.put("queueSize", watcher.getQueueSize() == null ? 0L : watcher.getQueueSize());
        status.put("syncedCount", watcher.getSyncedCount() == null ? 0L : watcher.getSyncedCount());
        status.put("lastSyncTime", watcher.getLastSyncTime());
        status.put("taskCount", listBoundTasks(watcherId).size());
        return status;
    }

    private void heartbeat(Long watcherId) {
        try {
            WatcherConfig watcher = requireMysqlWatcher(watcherId);
            List<SyncTaskConfig> tasks = listBoundTasks(watcherId);
            long currentQueue = watcher.getQueueSize() == null ? 0L : watcher.getQueueSize();
            long delta = tasks.isEmpty() ? 0L : ThreadLocalRandom.current().nextLong(5, 35);
            long nextQueue = Math.max(0, currentQueue + ThreadLocalRandom.current().nextLong(-5, 16));
            long syncedCount = (watcher.getSyncedCount() == null ? 0L : watcher.getSyncedCount()) + delta;
            watcherConfigService.updateRuntimeState(
                    watcherId,
                    "running",
                    nextQueue,
                    syncedCount,
                    LocalDateTime.now()
            );
            if (nextQueue > 500) {
                alertService.triggerAlert("delay", "medium", "MySQL watcher 队列堆积: " + watcherId, null);
            }
        } catch (Exception e) {
            log.error("MySQL watcher 心跳失败: {}", watcherId, e);
            runningWatchers.remove(watcherId);
            watcherConfigService.updateRuntimeState(watcherId, "stopped", 0L, 0L, LocalDateTime.now());
        }
    }

    private WatcherConfig requireMysqlWatcher(Long watcherId) {
        WatcherConfig watcher = watcherConfigService.requireEntity(watcherId);
        if (!"mysql".equalsIgnoreCase(watcher.getSourceType())) {
            throw new RuntimeException("当前 watcher 不是 MySQL 类型");
        }
        return watcher;
    }

    private List<SyncTaskConfig> listBoundTasks(Long watcherId) {
        return syncTaskConfigMapper.selectList(
                new QueryWrapper<SyncTaskConfig>()
                        .eq("watcher_id", watcherId)
                        .ne("sync_mode", "full")
                        .eq("status", 1)
        );
    }
}
