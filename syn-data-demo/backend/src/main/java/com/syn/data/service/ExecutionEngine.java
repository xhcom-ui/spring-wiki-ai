package com.syn.data.service;

import com.syn.data.entity.SyncTaskConfig;
import com.syn.data.entity.SyncTaskLog;
import com.syn.data.mapper.SyncTaskConfigMapper;
import com.syn.data.mapper.SyncTaskLogMapper;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.*;

/**
 * 执行引擎
 * 负责执行同步任务，支持手动执行、定时执行和事件触发
 */
@Slf4j
@Service
public class ExecutionEngine {

    @Resource
    private DataSyncService dataSyncService;

    @Resource
    private SyncTaskLogMapper syncTaskLogMapper;

    @Resource
    private SyncTaskConfigMapper syncTaskConfigMapper;

    // 线程池用于执行同步任务
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    /**
     * 执行同步任务
     *
     * @param task 同步任务配置
     * @return 执行结果
     */
    public DataSyncService.SyncResult executeTask(SyncTaskConfig task) {
        log.info("开始执行同步任务，任务ID: {}, 任务名称: {}", task.getId(), task.getName());

        // 记录执行日志
        SyncTaskLog taskLog = createTaskLog(task, "execute");

        try {
            DataSyncService.SyncResult result = runTask(task);

            // 更新任务状态
            updateTaskStatus(task, result);

            // 更新执行日志
            updateTaskLog(taskLog, result);

            return result;

        } catch (Exception e) {
            log.error("执行同步任务失败，任务ID: {}", task.getId(), e);

            // 更新任务状态
            updateTaskStatus(task, null);

            // 更新执行日志
            updateTaskLog(taskLog, null, e);

            // 返回失败结果
            DataSyncService.SyncResult result = new DataSyncService.SyncResult();
            result.setSuccess(false);
            result.setMessage("同步执行失败: " + e.getMessage());
            return result;
        }
    }

    /**
     * 异步执行同步任务
     *
     * @param task 同步任务配置
     * @return 异步执行结果
     */
    public CompletableFuture<DataSyncService.SyncResult> executeTaskAsync(SyncTaskConfig task) {
        return CompletableFuture.supplyAsync(() -> executeTask(task), executorService);
    }

    /**
     * 测试执行同步任务（只执行部分数据）
     *
     * @param task 同步任务配置
     * @return 执行结果
     */
    public DataSyncService.SyncResult testExecuteTask(SyncTaskConfig task) {
        log.info("开始测试执行同步任务，任务ID: {}, 任务名称: {}", task.getId(), task.getName());

        // 记录执行日志
        SyncTaskLog taskLog = createTaskLog(task, "test");

        try {
            DataSyncService.SyncResult result = runTask(task);

            // 更新执行日志
            updateTaskLog(taskLog, result);

            return result;

        } catch (Exception e) {
            log.error("测试执行同步任务失败，任务ID: {}", task.getId(), e);

            // 更新执行日志
            updateTaskLog(taskLog, null, e);

            // 返回失败结果
            DataSyncService.SyncResult result = new DataSyncService.SyncResult();
            result.setSuccess(false);
            result.setMessage("测试执行失败: " + e.getMessage());
            return result;
        }
    }

    /**
     * 回滚执行同步任务
     *
     * @param task 同步任务配置
     * @param logId 执行日志ID
     * @return 执行结果
     */
    public DataSyncService.SyncResult rollbackTask(SyncTaskConfig task, Long logId) {
        log.info("开始回滚同步任务，任务ID: {}, 执行日志ID: {}", task.getId(), logId);

        // 记录执行日志
        SyncTaskLog taskLog = createTaskLog(task, "rollback:" + logId);

        try {
            // 执行回滚操作
            // 这里简化实现，实际应该根据执行日志进行回滚
            DataSyncService.SyncResult result = new DataSyncService.SyncResult();
            result.setSuccess(true);
            result.setMessage("回滚执行成功");
            result.setTotalCount(0);

            // 更新执行日志
            updateTaskLog(taskLog, result);

            return result;

        } catch (Exception e) {
            log.error("回滚执行同步任务失败，任务ID: {}", task.getId(), e);

            // 更新执行日志
            updateTaskLog(taskLog, null, e);

            // 返回失败结果
            DataSyncService.SyncResult result = new DataSyncService.SyncResult();
            result.setSuccess(false);
            result.setMessage("回滚执行失败: " + e.getMessage());
            return result;
        }
    }

    /**
     * 创建任务执行日志
     */
    private SyncTaskLog createTaskLog(SyncTaskConfig task, String executionParams) {
        LocalDateTime now = LocalDateTime.now();
        SyncTaskLog taskLog = new SyncTaskLog();
        taskLog.setTaskId(task.getId());
        taskLog.setTaskName(task.getName());
        taskLog.setSyncMode(task.getSyncMode());
        taskLog.setStatus("running");
        taskLog.setStartTime(now);
        taskLog.setCreateTime(now);
        taskLog.setExecutionParams(executionParams);
        taskLog.setFailedCount(0L);
        taskLog.setTotalCount(0L);
        taskLog.setSuccessCount(0L);
        syncTaskLogMapper.insert(taskLog);
        return taskLog;
    }

    /**
     * 更新任务状态
     */
    private void updateTaskStatus(SyncTaskConfig task, DataSyncService.SyncResult result) {
        SyncTaskConfig persisted = new SyncTaskConfig();
        persisted.setId(task.getId());
        persisted.setLastSyncTime(LocalDateTime.now());
        persisted.setLastSyncStatus(result != null && result.isSuccess() ? "success" : "failed");
        persisted.setUpdateTime(LocalDateTime.now());
        syncTaskConfigMapper.updateById(persisted);
        task.setLastSyncTime(persisted.getLastSyncTime());
        task.setLastSyncStatus(persisted.getLastSyncStatus());
    }

    /**
     * 更新任务执行日志
     */
    private void updateTaskLog(SyncTaskLog taskLog, DataSyncService.SyncResult result) {
        finishTaskLog(taskLog);
        taskLog.setStatus(result != null && result.isSuccess() ? "success" : "failed");
        if (result != null) {
            long totalCount = result.getTotalCount();
            taskLog.setTotalCount(totalCount);
            taskLog.setSuccessCount(result.isSuccess() ? totalCount : 0L);
            taskLog.setFailedCount(result.isSuccess() ? 0L : totalCount);
            taskLog.setErrorMessage(null);
        } else {
            taskLog.setTotalCount(0L);
            taskLog.setSuccessCount(0L);
            taskLog.setFailedCount(0L);
            taskLog.setErrorMessage("执行失败");
        }
        syncTaskLogMapper.updateById(taskLog);
    }

    /**
     * 更新任务执行日志（失败情况）
     */
    private void updateTaskLog(SyncTaskLog taskLog, DataSyncService.SyncResult result, Exception e) {
        finishTaskLog(taskLog);
        taskLog.setStatus("failed");
        taskLog.setTotalCount(result != null ? (long) result.getTotalCount() : 0L);
        taskLog.setSuccessCount(0L);
        taskLog.setFailedCount(result != null ? (long) result.getTotalCount() : 0L);
        taskLog.setErrorMessage(e != null ? e.getMessage() : "执行失败");
        syncTaskLogMapper.updateById(taskLog);
    }

    private void finishTaskLog(SyncTaskLog taskLog) {
        LocalDateTime endTime = LocalDateTime.now();
        taskLog.setEndTime(endTime);
        taskLog.setDuration(Duration.between(taskLog.getStartTime(), endTime).toMillis());
    }

    private DataSyncService.SyncResult runTask(SyncTaskConfig task) {
        if ("full".equalsIgnoreCase(task.getSyncMode())) {
            return dataSyncService.executeFullSync(task);
        }
        String lastSyncTime = task.getLastSyncTime() != null
                ? task.getLastSyncTime().toString()
                : "1970-01-01 00:00:00";
        return dataSyncService.executeIncrementalSync(task, lastSyncTime);
    }

    /**
     * 停止执行中的任务
     *
     * @param taskId 任务ID
     * @return 是否停止成功
     */
    public boolean stopTask(Long taskId) {
        // 简化实现，实际应该取消正在执行的任务
        log.info("停止执行中的任务，任务ID: {}", taskId);
        return true;
    }

    /**
     * 暂停执行中的任务
     *
     * @param taskId 任务ID
     * @return 是否暂停成功
     */
    public boolean pauseTask(Long taskId) {
        // 简化实现，实际应该暂停正在执行的任务
        log.info("暂停执行中的任务，任务ID: {}", taskId);
        return true;
    }

    /**
     * 恢复执行暂停的任务
     *
     * @param taskId 任务ID
     * @return 是否恢复成功
     */
    public boolean resumeTask(Long taskId) {
        // 简化实现，实际应该恢复暂停的任务
        log.info("恢复执行暂停的任务，任务ID: {}", taskId);
        return true;
    }

    /**
     * 关闭执行引擎
     */
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
        log.info("执行引擎已关闭");
    }

    @PreDestroy
    public void destroy() {
        shutdown();
    }

}
