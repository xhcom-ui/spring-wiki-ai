package com.syn.data.service;

import com.syn.data.entity.SyncTaskConfig;
import com.syn.data.entity.WatcherConfig;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/**
 * watcher 运行时分发器。
 * 统一根据持久化 watcher 配置路由到对应的 MySQL/PostgreSQL 运行时服务。
 */
@Service
public class WatcherRuntimeManager {

    @Resource
    private WatcherConfigService watcherConfigService;

    private final Map<String, WatcherRuntimeService> runtimeServiceMap;

    public WatcherRuntimeManager(List<WatcherRuntimeService> runtimeServices) {
        this.runtimeServiceMap = runtimeServices.stream()
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        service -> normalizeSourceType(service.getSourceType()),
                        service -> service
                ));
    }

    public void start(Long watcherId) {
        withRuntime(watcherId, runtime -> runtime.startListener(watcherId));
    }

    public void start(Long watcherId, String expectedSourceType) {
        withRuntime(requireWatcher(watcherId, expectedSourceType), runtime -> runtime.startListener(watcherId));
    }

    public void stop(Long watcherId) {
        withRuntime(watcherId, runtime -> runtime.stopListener(watcherId));
    }

    public void stop(Long watcherId, String expectedSourceType) {
        withRuntime(requireWatcher(watcherId, expectedSourceType), runtime -> runtime.stopListener(watcherId));
    }

    public Map<String, Object> status(Long watcherId) {
        return resolveRuntimeByWatcherId(watcherId).getListenerStatus(watcherId);
    }

    public Map<String, Object> status(Long watcherId, String expectedSourceType) {
        WatcherConfig watcher = requireWatcher(watcherId, expectedSourceType);
        return resolveRuntimeBySourceType(watcher.getSourceType()).getListenerStatus(watcherId);
    }

    public void registerTask(SyncTaskConfig task) {
        if (task == null || task.getWatcherId() == null || "full".equalsIgnoreCase(task.getSyncMode())) {
            return;
        }
        resolveRuntimeByWatcherId(task.getWatcherId()).registerTask(task);
    }

    public void unregisterTask(SyncTaskConfig task) {
        if (task == null || task.getWatcherId() == null || "full".equalsIgnoreCase(task.getSyncMode())) {
            return;
        }
        resolveRuntimeByWatcherId(task.getWatcherId()).unregisterTask(task);
    }

    public WatcherConfig requireWatcher(Long watcherId) {
        return watcherConfigService.requireEntity(watcherId);
    }

    public WatcherConfig requireWatcher(Long watcherId, String expectedSourceType) {
        WatcherConfig watcher = watcherConfigService.requireEntity(watcherId);
        String normalizedExpected = normalizeSourceType(expectedSourceType);
        String normalizedActual = normalizeSourceType(watcher.getSourceType());
        if (!normalizedExpected.equals(normalizedActual)) {
            throw new RuntimeException("watcher 类型不匹配，期望: " + normalizedExpected + "，实际: " + normalizedActual);
        }
        return watcher;
    }

    private void withRuntime(Long watcherId, Consumer<WatcherRuntimeService> action) {
        action.accept(resolveRuntimeByWatcherId(watcherId));
    }

    private void withRuntime(WatcherConfig watcher, Consumer<WatcherRuntimeService> action) {
        action.accept(resolveRuntimeBySourceType(watcher.getSourceType()));
    }

    private WatcherRuntimeService resolveRuntimeByWatcherId(Long watcherId) {
        return resolveRuntimeBySourceType(watcherConfigService.requireEntity(watcherId).getSourceType());
    }

    private WatcherRuntimeService resolveRuntimeBySourceType(String sourceType) {
        WatcherRuntimeService runtimeService = runtimeServiceMap.get(normalizeSourceType(sourceType));
        if (runtimeService == null) {
            throw new RuntimeException("暂不支持的数据源类型: " + sourceType);
        }
        return runtimeService;
    }

    private static String normalizeSourceType(String sourceType) {
        if (sourceType == null) {
            return "";
        }
        return sourceType.trim().toLowerCase(Locale.ROOT);
    }
}
