package com.syn.data.controller;

import com.syn.data.entity.SyncTaskConfig;
import com.syn.data.service.MySQLBinlogListenerService;
import com.syn.data.service.PostgresWalListenerService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * 实时同步任务管理控制器
 * 用于管理MySQL Binlog和PostgreSQL WAL的监听任务
 */
@RestController
@RequestMapping("/api/realtime")
public class RealtimeSyncController {

    @Resource
    private MySQLBinlogListenerService mySQLBinlogListenerService;

    @Resource
    private PostgresWalListenerService postgresWalListenerService;

    /**
     * 启动MySQL Binlog监听
     */
    @PostMapping("/mysql/start/{dataSourceId}")
    public Map<String, Object> startMySQLListener(@PathVariable Long dataSourceId) {
        Map<String, Object> result = new HashMap<>();
        try {
            mySQLBinlogListenerService.startListener(dataSourceId);
            result.put("success", true);
            result.put("message", "MySQL Binlog监听启动成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "MySQL Binlog监听启动失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 停止MySQL Binlog监听
     */
    @PostMapping("/mysql/stop/{dataSourceId}")
    public Map<String, Object> stopMySQLListener(@PathVariable Long dataSourceId) {
        Map<String, Object> result = new HashMap<>();
        try {
            mySQLBinlogListenerService.stopListener(dataSourceId);
            result.put("success", true);
            result.put("message", "MySQL Binlog监听停止成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "MySQL Binlog监听停止失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 获取MySQL Binlog监听状态
     */
    @GetMapping("/mysql/status/{dataSourceId}")
    public Map<String, Object> getMySQLListenerStatus(@PathVariable Long dataSourceId) {
        return mySQLBinlogListenerService.getListenerStatus(dataSourceId);
    }

    /**
     * 注册MySQL增量同步任务
     */
    @PostMapping("/mysql/register")
    public Map<String, Object> registerMySQLTask(@RequestBody SyncTaskConfig task) {
        Map<String, Object> result = new HashMap<>();
        try {
            mySQLBinlogListenerService.registerTask(task);
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
            mySQLBinlogListenerService.unregisterTask(task);
            result.put("success", true);
            result.put("message", "MySQL增量同步任务取消注册成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "MySQL增量同步任务取消注册失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 启动PostgreSQL WAL监听
     */
    @PostMapping("/postgres/start/{dataSourceId}")
    public Map<String, Object> startPostgresListener(@PathVariable Long dataSourceId) {
        Map<String, Object> result = new HashMap<>();
        try {
            postgresWalListenerService.startListener(dataSourceId);
            result.put("success", true);
            result.put("message", "PostgreSQL WAL监听启动成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "PostgreSQL WAL监听启动失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 停止PostgreSQL WAL监听
     */
    @PostMapping("/postgres/stop/{dataSourceId}")
    public Map<String, Object> stopPostgresListener(@PathVariable Long dataSourceId) {
        Map<String, Object> result = new HashMap<>();
        try {
            postgresWalListenerService.stopListener(dataSourceId);
            result.put("success", true);
            result.put("message", "PostgreSQL WAL监听停止成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "PostgreSQL WAL监听停止失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 获取PostgreSQL WAL监听状态
     */
    @GetMapping("/postgres/status/{dataSourceId}")
    public Map<String, Object> getPostgresListenerStatus(@PathVariable Long dataSourceId) {
        return postgresWalListenerService.getListenerStatus(dataSourceId);
    }

    /**
     * 注册PostgreSQL增量同步任务
     */
    @PostMapping("/postgres/register")
    public Map<String, Object> registerPostgresTask(@RequestBody SyncTaskConfig task) {
        Map<String, Object> result = new HashMap<>();
        try {
            postgresWalListenerService.registerTask(task);
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
            postgresWalListenerService.unregisterTask(task);
            result.put("success", true);
            result.put("message", "PostgreSQL增量同步任务取消注册成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "PostgreSQL增量同步任务取消注册失败: " + e.getMessage());
        }
        return result;
    }

}
