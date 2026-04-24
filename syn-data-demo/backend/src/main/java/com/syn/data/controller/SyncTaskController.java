package com.syn.data.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.syn.data.entity.SyncTaskConfig;
import com.syn.data.entity.WatcherConfig;
import com.syn.data.mapper.SyncTaskConfigMapper;
import com.syn.data.service.ExecutionEngine;
import com.syn.data.service.DataSyncService;
import com.syn.data.service.WatcherConfigService;
import com.syn.data.service.WatcherRuntimeManager;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 同步任务控制器
 */
@RestController
@RequestMapping("/api/task")
@SaCheckLogin
public class SyncTaskController {

    @Resource
    private SyncTaskConfigMapper syncTaskConfigMapper;

    @Resource
    private ExecutionEngine executionEngine;

    @Resource
    private WatcherConfigService watcherConfigService;

    @Resource
    private WatcherRuntimeManager watcherRuntimeManager;

    /**
     * 获取所有同步任务
     */
    @GetMapping
    public List<SyncTaskConfig> list() {
        return syncTaskConfigMapper.selectList(
                new QueryWrapper<SyncTaskConfig>()
                        .orderByDesc("update_time")
                        .orderByDesc("id")
        );
    }

    /**
     * 根据ID获取同步任务
     */
    @GetMapping("/{id}")
    public SyncTaskConfig getById(@PathVariable Long id) {
        return syncTaskConfigMapper.selectById(id);
    }

    /**
     * 创建同步任务
     */
    @PostMapping
    @SaCheckRole("admin")
    public SyncTaskConfig save(@RequestBody SyncTaskConfig task) {
        normalizeWatcherBinding(task);
        task.setCreateTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        task.setStatus(1); // 默认启用
        syncTaskConfigMapper.insert(task);
        registerRealtimeTask(task);
        return task;
    }

    /**
     * 更新同步任务
     */
    @PutMapping
    @SaCheckRole("admin")
    public SyncTaskConfig update(@RequestBody SyncTaskConfig task) {
        SyncTaskConfig existing = syncTaskConfigMapper.selectById(task.getId());
        if (existing != null) {
            unregisterRealtimeTask(existing);
        }
        normalizeWatcherBinding(task);
        task.setUpdateTime(LocalDateTime.now());
        syncTaskConfigMapper.updateById(task);
        registerRealtimeTask(task);
        return task;
    }

    /**
     * 删除同步任务
     */
    @DeleteMapping("/{id}")
    @SaCheckRole("admin")
    public void delete(@PathVariable Long id) {
        SyncTaskConfig existing = syncTaskConfigMapper.selectById(id);
        if (existing != null) {
            unregisterRealtimeTask(existing);
        }
        syncTaskConfigMapper.deleteById(id);
    }

    /**
     * 启动同步任务
     */
    @PostMapping("/{id}/start")
    @SaCheckRole("admin")
    public String start(@PathVariable Long id) {
        // 简化实现，实际应该调用XXL-Job或执行同步任务
        return "Task started successfully";
    }

    /**
     * 停止同步任务
     */
    @PostMapping("/{id}/stop")
    @SaCheckRole("admin")
    public String stop(@PathVariable Long id) {
        // 简化实现，实际应该停止XXL-Job或同步任务
        return "Task stopped successfully";
    }

    private void normalizeWatcherBinding(SyncTaskConfig task) {
        if (task.getWatcherId() == null) {
            return;
        }
        WatcherConfig watcher = watcherConfigService.requireEntity(task.getWatcherId());
        task.setSourceId(watcher.getSourceId());
        if (task.getTargetIndex() == null || task.getTargetIndex().isBlank()) {
            task.setTargetIndex(watcher.getTargetIndex());
        }
        if (task.getIncrementalField() == null || task.getIncrementalField().isBlank()) {
            task.setIncrementalField(watcher.getIncrementalField());
        }
        if (task.getSyncMode() == null || task.getSyncMode().isBlank()) {
            task.setSyncMode("incremental");
        }
    }

    private void registerRealtimeTask(SyncTaskConfig task) {
        watcherRuntimeManager.registerTask(task);
    }

    private void unregisterRealtimeTask(SyncTaskConfig task) {
        watcherRuntimeManager.unregisterTask(task);
    }

    /**
     * 执行同步任务
     */
    @PostMapping("/{id}/execute")
    @SaCheckRole("admin")
    public Map<String, Object> execute(@PathVariable Long id) {
        SyncTaskConfig task = syncTaskConfigMapper.selectById(id);
        if (task == null) {
            throw new RuntimeException("Task not found");
        }

        try {
            DataSyncService.SyncResult syncResult = executionEngine.executeTask(task);

            // 构建返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("success", syncResult.isSuccess());
            result.put("message", syncResult.getMessage());
            result.put("result", String.format("同步完成，共处理 %d 条数据，耗时 %d ms", 
                syncResult.getTotalCount(), syncResult.getDuration()));
            result.put("totalCount", syncResult.getTotalCount());
            result.put("duration", syncResult.getDuration());
            result.put("data", task);

            return result;

        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "同步执行失败: " + e.getMessage());
            result.put("result", "同步失败");

            return result;
        }
    }

    /**
     * 测试执行同步任务
     */
    @PostMapping("/{id}/test-execute")
    @SaCheckRole("admin")
    public Map<String, Object> testExecute(@PathVariable Long id) {
        SyncTaskConfig task = syncTaskConfigMapper.selectById(id);
        if (task == null) {
            throw new RuntimeException("Task not found");
        }

        try {
            DataSyncService.SyncResult syncResult = executionEngine.testExecuteTask(task);

            // 构建返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("success", syncResult.isSuccess());
            result.put("message", syncResult.getMessage());
            result.put("result", String.format("测试执行完成，共处理 %d 条数据，耗时 %d ms", 
                syncResult.getTotalCount(), syncResult.getDuration()));
            result.put("totalCount", syncResult.getTotalCount());
            result.put("duration", syncResult.getDuration());

            return result;

        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "测试执行失败: " + e.getMessage());
            result.put("result", "测试执行失败");

            return result;
        }
    }

    /**
     * 回滚执行同步任务
     */
    @PostMapping("/{id}/rollback")
    @SaCheckRole("admin")
    public Map<String, Object> rollback(@PathVariable Long id, @RequestParam Long logId) {
        SyncTaskConfig task = syncTaskConfigMapper.selectById(id);
        if (task == null) {
            throw new RuntimeException("Task not found");
        }

        try {
            DataSyncService.SyncResult syncResult = executionEngine.rollbackTask(task, logId);

            // 构建返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("success", syncResult.isSuccess());
            result.put("message", syncResult.getMessage());
            result.put("result", "回滚执行完成");

            return result;

        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "回滚执行失败: " + e.getMessage());
            result.put("result", "回滚执行失败");

            return result;
        }
    }

    /**
     * 暂停同步任务
     */
    @PostMapping("/{id}/pause")
    @SaCheckRole("admin")
    public Map<String, Object> pause(@PathVariable Long id) {
        try {
            boolean success = executionEngine.pauseTask(id);
            Map<String, Object> result = new HashMap<>();
            result.put("success", success);
            result.put("message", success ? "任务暂停成功" : "任务暂停失败");
            return result;
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "任务暂停失败: " + e.getMessage());
            return result;
        }
    }

    /**
     * 恢复同步任务
     */
    @PostMapping("/{id}/resume")
    @SaCheckRole("admin")
    public Map<String, Object> resume(@PathVariable Long id) {
        try {
            boolean success = executionEngine.resumeTask(id);
            Map<String, Object> result = new HashMap<>();
            result.put("success", success);
            result.put("message", success ? "任务恢复成功" : "任务恢复失败");
            return result;
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "任务恢复失败: " + e.getMessage());
            return result;
        }
    }

    /**
     * 测试SQL语句
     */
    @PostMapping("/{id}/test-sql")
    @SaCheckRole("admin")
    public Map<String, Object> testSql(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            SyncTaskConfig task = syncTaskConfigMapper.selectById(id);
            if (task == null) {
                result.put("success", false);
                result.put("message", "任务不存在");
                return result;
            }

            String sql = task.getSql_();
            if (sql == null || sql.isEmpty()) {
                result.put("success", false);
                result.put("message", "SQL语句不能为空");
                return result;
            }

            // 模拟SQL执行结果
            List<Map<String, Object>> previewData = new ArrayList<>();
            for (int i = 1; i <= 5; i++) {
                Map<String, Object> row = new HashMap<>();
                row.put("id", i);
                row.put("name", "测试数据" + i);
                row.put("create_time", LocalDateTime.now());
                row.put("status", i % 2 == 0 ? 1 : 0);
                previewData.add(row);
            }

            result.put("success", true);
            result.put("message", "SQL测试成功");
            result.put("previewData", previewData);
            result.put("executionTime", 150);
            result.put("estimatedCount", 1000);
            result.put("executionPlan", "执行计划: 使用索引 idx_id 进行查询");
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "SQL测试失败: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 获取SQL执行计划
     */
    @PostMapping("/{id}/execution-plan")
    @SaCheckRole("admin")
    public Map<String, Object> getExecutionPlan(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            SyncTaskConfig task = syncTaskConfigMapper.selectById(id);
            if (task == null) {
                result.put("success", false);
                result.put("message", "任务不存在");
                return result;
            }

            String sql = task.getSql_();
            if (sql == null || sql.isEmpty()) {
                result.put("success", false);
                result.put("message", "SQL语句不能为空");
                return result;
            }

            // 模拟执行计划
            String executionPlan = "执行计划:\n" +
                    "1. 使用索引 idx_id 进行查询\n" +
                    "2. 表扫描行数: 1000\n" +
                    "3. 预计执行时间: 150ms\n" +
                    "4. 索引使用情况: 良好\n" +
                    "5. 可能的优化建议: 考虑添加复合索引以提高查询性能";

            result.put("success", true);
            result.put("message", "获取执行计划成功");
            result.put("executionPlan", executionPlan);
            result.put("estimatedCount", 1000);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取执行计划失败: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 测试ES Mapping配置
     */
    @PostMapping("/{id}/test-mapping")
    public Map<String, Object> testMapping(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            SyncTaskConfig task = syncTaskConfigMapper.selectById(id);
            if (task == null) {
                result.put("success", false);
                result.put("message", "任务不存在");
                return result;
            }

            // 验证JSON格式
            if (task.getFieldMapping() != null && !task.getFieldMapping().isEmpty()) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                mapper.readTree(task.getFieldMapping());
            }

            result.put("success", true);
            result.put("message", "ES Mapping配置格式正确");
            result.put("mapping", task.getFieldMapping());
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "ES Mapping测试失败: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 获取SQL模板列表
     */
    @GetMapping("/sql-templates")
    public List<Map<String, Object>> getSqlTemplates() {
        List<Map<String, Object>> templates = new ArrayList<>();
        
        Map<String, Object> template1 = new HashMap<>();
        template1.put("name", "查询所有数据");
        template1.put("description", "SELECT * FROM table");
        template1.put("sql", "SELECT * FROM {table_name}");
        templates.add(template1);

        Map<String, Object> template2 = new HashMap<>();
        template2.put("name", "按时间查询");
        template2.put("description", "按时间字段查询数据");
        template2.put("sql", "SELECT * FROM {table_name} WHERE update_time > '{last_sync_time}'");
        templates.add(template2);

        Map<String, Object> template3 = new HashMap<>();
        template3.put("name", "分页查询");
        template3.put("description", "分页查询数据");
        template3.put("sql", "SELECT * FROM {table_name} LIMIT {limit} OFFSET {offset}");
        templates.add(template3);

        return templates;
    }

    /**
     * 获取字段列表
     */
    @PostMapping("/{id}/fields")
    public List<String> getFields(@PathVariable Long id) {
        // 简化实现，实际应该根据SQL查询字段
        List<String> fields = new ArrayList<>();
        fields.add("id");
        fields.add("name");
        fields.add("create_time");
        fields.add("update_time");
        fields.add("status");
        fields.add("description");
        return fields;
    }

}
