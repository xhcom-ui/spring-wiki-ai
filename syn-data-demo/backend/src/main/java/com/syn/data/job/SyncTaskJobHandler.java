package com.syn.data.job;

import com.syn.data.entity.SyncTaskConfig;
import com.syn.data.mapper.SyncTaskConfigMapper;
import com.syn.data.service.DataSyncService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * XXL-Job同步任务处理器
 * 负责执行定时同步任务
 */
@Slf4j
@Component
public class SyncTaskJobHandler {

    @Resource
    private SyncTaskConfigMapper syncTaskConfigMapper;

    @Resource
    private DataSyncService dataSyncService;

    /**
     * 执行全量同步任务
     * 
     * 使用方式：在XXL-Job管理后台配置任务，JobHandler填写 "fullSyncJob"
     * 参数填写任务ID，如：1
     */
    @XxlJob("fullSyncJob")
    public void fullSyncJob() {
        // 获取任务参数
        String param = XxlJobHelper.getJobParam();
        if (param == null || param.isEmpty()) {
            XxlJobHelper.log("任务参数为空，请填写任务ID");
            XxlJobHelper.handleFail("任务参数为空");
            return;
        }

        Long taskId;
        try {
            taskId = Long.parseLong(param);
        } catch (NumberFormatException e) {
            XxlJobHelper.log("任务参数格式错误: " + param);
            XxlJobHelper.handleFail("任务参数格式错误");
            return;
        }

        // 获取任务配置
        SyncTaskConfig task = syncTaskConfigMapper.selectById(taskId);
        if (task == null) {
            XxlJobHelper.log("任务不存在，ID: " + taskId);
            XxlJobHelper.handleFail("任务不存在");
            return;
        }

        XxlJobHelper.log("开始执行全量同步任务，任务ID: {}, 任务名称: {}", taskId, task.getName());

        try {
            // 执行全量同步
            DataSyncService.SyncResult result = dataSyncService.executeFullSync(task);

            // 更新任务状态
            task.setLastSyncTime(LocalDateTime.now());
            task.setLastSyncStatus(result.isSuccess() ? "success" : "failed");
            syncTaskConfigMapper.updateById(task);

            if (result.isSuccess()) {
                XxlJobHelper.log("全量同步成功，处理数据量: {}, 耗时: {} ms", 
                    result.getTotalCount(), result.getDuration());
                XxlJobHelper.handleSuccess();
            } else {
                XxlJobHelper.log("全量同步失败: " + result.getMessage());
                XxlJobHelper.handleFail(result.getMessage());
            }

        } catch (Exception e) {
            log.error("执行全量同步任务失败，任务ID: {}", taskId, e);
            XxlJobHelper.log("执行全量同步任务失败: " + e.getMessage());
            XxlJobHelper.handleFail(e.getMessage());
        }
    }

    /**
     * 执行增量同步任务
     * 
     * 使用方式：在XXL-Job管理后台配置任务，JobHandler填写 "incrementalSyncJob"
     * 参数填写任务ID，如：1
     */
    @XxlJob("incrementalSyncJob")
    public void incrementalSyncJob() {
        // 获取任务参数
        String param = XxlJobHelper.getJobParam();
        if (param == null || param.isEmpty()) {
            XxlJobHelper.log("任务参数为空，请填写任务ID");
            XxlJobHelper.handleFail("任务参数为空");
            return;
        }

        Long taskId;
        try {
            taskId = Long.parseLong(param);
        } catch (NumberFormatException e) {
            XxlJobHelper.log("任务参数格式错误: " + param);
            XxlJobHelper.handleFail("任务参数格式错误");
            return;
        }

        // 获取任务配置
        SyncTaskConfig task = syncTaskConfigMapper.selectById(taskId);
        if (task == null) {
            XxlJobHelper.log("任务不存在，ID: " + taskId);
            XxlJobHelper.handleFail("任务不存在");
            return;
        }

        XxlJobHelper.log("开始执行增量同步任务，任务ID: {}, 任务名称: {}", taskId, task.getName());

        try {
            // 获取上次同步时间
            String lastSyncTime = task.getLastSyncTime() != null 
                ? task.getLastSyncTime().toString() 
                : "1970-01-01 00:00:00";

            // 执行增量同步
            DataSyncService.SyncResult result = dataSyncService.executeIncrementalSync(task, lastSyncTime);

            // 更新任务状态
            task.setLastSyncTime(LocalDateTime.now());
            task.setLastSyncStatus(result.isSuccess() ? "success" : "failed");
            syncTaskConfigMapper.updateById(task);

            if (result.isSuccess()) {
                XxlJobHelper.log("增量同步成功，处理数据量: {}, 耗时: {} ms", 
                    result.getTotalCount(), result.getDuration());
                XxlJobHelper.handleSuccess();
            } else {
                XxlJobHelper.log("增量同步失败: " + result.getMessage());
                XxlJobHelper.handleFail(result.getMessage());
            }

        } catch (Exception e) {
            log.error("执行增量同步任务失败，任务ID: {}", taskId, e);
            XxlJobHelper.log("执行增量同步任务失败: " + e.getMessage());
            XxlJobHelper.handleFail(e.getMessage());
        }
    }

    /**
     * 批量执行同步任务
     * 
     * 使用方式：在XXL-Job管理后台配置任务，JobHandler填写 "batchSyncJob"
     * 参数填写任务ID列表，多个ID用逗号分隔，如：1,2,3
     */
    @XxlJob("batchSyncJob")
    public void batchSyncJob() {
        // 获取任务参数
        String param = XxlJobHelper.getJobParam();
        if (param == null || param.isEmpty()) {
            XxlJobHelper.log("任务参数为空，请填写任务ID列表");
            XxlJobHelper.handleFail("任务参数为空");
            return;
        }

        String[] taskIds = param.split(",");
        int successCount = 0;
        int failCount = 0;

        for (String taskIdStr : taskIds) {
            Long taskId;
            try {
                taskId = Long.parseLong(taskIdStr.trim());
            } catch (NumberFormatException e) {
                XxlJobHelper.log("任务参数格式错误: " + taskIdStr);
                failCount++;
                continue;
            }

            // 获取任务配置
            SyncTaskConfig task = syncTaskConfigMapper.selectById(taskId);
            if (task == null) {
                XxlJobHelper.log("任务不存在，ID: " + taskId);
                failCount++;
                continue;
            }

            XxlJobHelper.log("开始执行任务，任务ID: {}, 任务名称: {}", taskId, task.getName());

            try {
                DataSyncService.SyncResult result;
                
                // 根据同步模式选择执行方式
                if ("full".equals(task.getSyncMode())) {
                    result = dataSyncService.executeFullSync(task);
                } else {
                    String lastSyncTime = task.getLastSyncTime() != null 
                        ? task.getLastSyncTime().toString() 
                        : "1970-01-01 00:00:00";
                    result = dataSyncService.executeIncrementalSync(task, lastSyncTime);
                }

                // 更新任务状态
                task.setLastSyncTime(LocalDateTime.now());
                task.setLastSyncStatus(result.isSuccess() ? "success" : "failed");
                syncTaskConfigMapper.updateById(task);

                if (result.isSuccess()) {
                    XxlJobHelper.log("任务执行成功，任务ID: {}, 处理数据量: {}, 耗时: {} ms", 
                        taskId, result.getTotalCount(), result.getDuration());
                    successCount++;
                } else {
                    XxlJobHelper.log("任务执行失败，任务ID: {}, 错误: {}", taskId, result.getMessage());
                    failCount++;
                }

            } catch (Exception e) {
                log.error("执行任务失败，任务ID: {}", taskId, e);
                XxlJobHelper.log("执行任务失败，任务ID: {}, 错误: {}", taskId, e.getMessage());
                failCount++;
            }
        }

        XxlJobHelper.log("批量任务执行完成，成功: {}, 失败: {}", successCount, failCount);
        
        if (failCount == 0) {
            XxlJobHelper.handleSuccess();
        } else {
            XxlJobHelper.handleFail("部分任务执行失败，成功: " + successCount + ", 失败: " + failCount);
        }
    }
}
