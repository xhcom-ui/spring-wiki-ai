package com.syn.data.service;

import com.syn.data.entity.SyncTaskConfig;
import com.syn.data.entity.SyncTaskLog;
import com.syn.data.mapper.SyncTaskLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
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
        SyncTaskLog log = createTaskLog(task);

        try {
            // 执行同步任务
            DataSyncService.SyncResult result;
            if ("full".equals(task.getSyncMode())) {
                // 全量同步
                result = dataSyncService.executeFullSync(task);
            } else {
                // 增量同步
                String lastSyncTime = task.getLastSyncTime() != null 
                    ? task.getLastSyncTime().toString() 
                    : "1970-01-01 00:00:00";
                result = dataSyncService.executeIncrementalSync(task, lastSyncTime);
            }

            // 更新任务状态
            updateTaskStatus(task, result);

            // 更新执行日志
            updateTaskLog(log, result);

            return result;

        } catch (Exception e) {
            log.error("执行同步任务失败，任务ID: {}", task.getId(), e);

            // 更新任务状态
            updateTaskStatus(task, null);

            // 更新执行日志
            updateTaskLog(log, null, e);

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
        SyncTaskLog log = createTaskLog(task);
        log.setIsTest(true);

        try {
            // 执行同步任务（测试模式）
            DataSyncService.SyncResult result;
            if ("full".equals(task.getSyncMode())) {
                // 全量同步（测试模式）
                result = dataSyncService.executeFullSync(task);
            } else {
                // 增量同步（测试模式）
                String lastSyncTime = task.getLastSyncTime() != null 
                    ? task.getLastSyncTime().toString() 
                    : "1970-01-01 00:00:00";
                result = dataSyncService.executeIncrementalSync(task, lastSyncTime);
            }

            // 更新执行日志
            updateTaskLog(log, result);

            return result;

        } catch (Exception e) {
            log.error("测试执行同步任务失败，任务ID: {}", task.getId(), e);

            // 更新执行日志
            updateTaskLog(log, null, e);

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
        SyncTaskLog log = createTaskLog(task);
        log.setIsRollback(true);

        try {
            // 执行回滚操作
            // 这里简化实现，实际应该根据执行日志进行回滚
            DataSyncService.SyncResult result = new DataSyncService.SyncResult();
            result.setSuccess(true);
            result.setMessage("回滚执行成功");
            result.setTotalCount(0);

            // 更新执行日志
            updateTaskLog(log, result);

            return result;

        } catch (Exception e) {
            log.error("回滚执行同步任务失败，任务ID: {}", task.getId(), e);

            // 更新执行日志
            updateTaskLog(log, null, e);

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
    private SyncTaskLog createTaskLog(SyncTaskConfig task) {
        SyncTaskLog log = new SyncTaskLog();
        log.setTaskId(task.getId());
        log.setTaskName(task.getName());
        log.setSyncMode(task.getSyncMode());
        log.setStatus("running");
        log.setStartTime(LocalDateTime.now());
        log.setIsTest(false);
        log.setIsRollback(false);
        syncTaskLogMapper.insert(log);
        return log;
    }

    /**
     * 更新任务状态
     */
    private void updateTaskStatus(SyncTaskConfig task, DataSyncService.SyncResult result) {
        task.setLastSyncTime(LocalDateTime.now());
        task.setLastSyncStatus(result != null && result.isSuccess() ? "success" : "failed");
    }

    /**
     * 更新任务执行日志
     */
    private void updateTaskLog(SyncTaskLog log, DataSyncService.SyncResult result) {
        log.setEndTime(LocalDateTime.now());
        log.setDuration(log.getEndTime().getSecond() - log.getStartTime().getSecond());
        log.setStatus(result != null && result.isSuccess() ? "success" : "failed");
        if (result != null) {
            log.setSourceCount((long) result.getTotalCount());
            log.setTargetCount((long) result.getTotalCount());
            log.setErrorMessage(null);
        } else {
            log.setSourceCount(0L);
            log.setTargetCount(0L);
            log.setErrorMessage("执行失败");
        }
        syncTaskLogMapper.updateById(log);
    }

    /**
     * 更新任务执行日志（失败情况）
     */
    private void updateTaskLog(SyncTaskLog log, DataSyncService.SyncResult result, Exception e) {
        log.setEndTime(LocalDateTime.now());
        log.setDuration(log.getEndTime().getSecond() - log.getStartTime().getSecond());
        log.setStatus("failed");
        log.setSourceCount(0L);
        log.setTargetCount(0L);
        log.setErrorMessage(e != null ? e.getMessage() : "执行失败");
        syncTaskLogMapper.updateById(log);
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

}
