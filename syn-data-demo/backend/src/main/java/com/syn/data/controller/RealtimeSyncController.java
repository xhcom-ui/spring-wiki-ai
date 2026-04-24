package com.syn.data.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.syn.data.entity.SyncTaskConfig;
import com.syn.data.service.WatcherRuntimeManager;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * 实时同步任务管理控制器。
 * 当前直接基于 watcher 配置模型管理运行时监听状态。
 */
@RestController
@RequestMapping("/api/realtime")
@SaCheckRole("admin")
public class RealtimeSyncController {

    @Resource
    private WatcherRuntimeManager watcherRuntimeManager;

    /**
     * 启动 MySQL watcher
     */
    @PostMapping("/mysql/start/{watcherId}")
    public Map<String, Object> startMySQLListener(@PathVariable Long watcherId) {
        Map<String, Object> result = new HashMap<>();
        try {
            watcherRuntimeManager.start(watcherId, "mysql");
            result.put("success", true);
            result.put("message", "MySQL watcher 启动成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "MySQL watcher 启动失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 停止 MySQL watcher
     */
    @PostMapping("/mysql/stop/{watcherId}")
    public Map<String, Object> stopMySQLListener(@PathVariable Long watcherId) {
        Map<String, Object> result = new HashMap<>();
        try {
            watcherRuntimeManager.stop(watcherId, "mysql");
            result.put("success", true);
            result.put("message", "MySQL watcher 停止成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "MySQL watcher 停止失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 获取 MySQL watcher 状态
     */
    @GetMapping("/mysql/status/{watcherId}")
    public Map<String, Object> getMySQLListenerStatus(@PathVariable Long watcherId) {
        return watcherRuntimeManager.status(watcherId, "mysql");
    }

    /**
     * 注册MySQL增量同步任务
     */
    @PostMapping("/mysql/register")
    public Map<String, Object> registerMySQLTask(@RequestBody SyncTaskConfig task) {
        Map<String, Object> result = new HashMap<>();
        try {
            requireTaskWatcher(task, "mysql");
            watcherRuntimeManager.registerTask(task);
            result.put("success", true);
            result.put("message", "MySQL增量同步任务注册成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "MySQL增量同步任务注册失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 取消注册MySQL增量同步任务
     */
    @PostMapping("/mysql/unregister")
    public Map<String, Object> unregisterMySQLTask(@RequestBody SyncTaskConfig task) {
        Map<String, Object> result = new HashMap<>();
        try {
            requireTaskWatcher(task, "mysql");
            watcherRuntimeManager.unregisterTask(task);
            result.put("success", true);
            result.put("message", "MySQL增量同步任务取消注册成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "MySQL增量同步任务取消注册失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 启动 PostgreSQL watcher
     */
    @PostMapping("/postgres/start/{watcherId}")
    public Map<String, Object> startPostgresListener(@PathVariable Long watcherId) {
        Map<String, Object> result = new HashMap<>();
        try {
            watcherRuntimeManager.start(watcherId, "postgresql");
            result.put("success", true);
            result.put("message", "PostgreSQL watcher 启动成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "PostgreSQL watcher 启动失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 停止 PostgreSQL watcher
     */
    @PostMapping("/postgres/stop/{watcherId}")
    public Map<String, Object> stopPostgresListener(@PathVariable Long watcherId) {
        Map<String, Object> result = new HashMap<>();
        try {
            watcherRuntimeManager.stop(watcherId, "postgresql");
            result.put("success", true);
            result.put("message", "PostgreSQL watcher 停止成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "PostgreSQL watcher 停止失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 获取 PostgreSQL watcher 状态
     */
    @GetMapping("/postgres/status/{watcherId}")
    public Map<String, Object> getPostgresListenerStatus(@PathVariable Long watcherId) {
        return watcherRuntimeManager.status(watcherId, "postgresql");
    }

    /**
     * 注册PostgreSQL增量同步任务
     */
    @PostMapping("/postgres/register")
    public Map<String, Object> registerPostgresTask(@RequestBody SyncTaskConfig task) {
        Map<String, Object> result = new HashMap<>();
        try {
            requireTaskWatcher(task, "postgresql");
            watcherRuntimeManager.registerTask(task);
            result.put("success", true);
            result.put("message", "PostgreSQL增量同步任务注册成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "PostgreSQL增量同步任务注册失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 取消注册PostgreSQL增量同步任务
     */
    @PostMapping("/postgres/unregister")
    public Map<String, Object> unregisterPostgresTask(@RequestBody SyncTaskConfig task) {
        Map<String, Object> result = new HashMap<>();
        try {
            requireTaskWatcher(task, "postgresql");
            watcherRuntimeManager.unregisterTask(task);
            result.put("success", true);
            result.put("message", "PostgreSQL增量同步任务取消注册成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "PostgreSQL增量同步任务取消注册失败: " + e.getMessage());
        }
        return result;
    }

    private void requireTaskWatcher(SyncTaskConfig task, String sourceType) {
        if (task.getWatcherId() == null) {
            throw new RuntimeException("task.watcherId 不能为空");
        }
        watcherRuntimeManager.requireWatcher(task.getWatcherId(), sourceType);
    }

}
