package com.syn.data.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 同步任务执行记录实体类
 */
@Data
@TableName("sync_task_log")
public class SyncTaskLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 任务ID
     */
    private Long taskId;

    /**
     * 执行状态：running-运行中，success-成功，failed-失败
     */
    private String status;

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 耗时（毫秒）
     */
    private Long duration;

    /**
     * 处理总量
     */
    private Long totalCount;

    /**
     * 成功数量
     */
    private Long successCount;

    /**
     * 失败数据量
     */
    private Long failedCount;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 执行参数
     */
    private String executionParams;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 仅用于页面展示的同步模式，不落库。
     */
    @TableField(exist = false)
    private String syncMode;

}
