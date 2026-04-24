package com.syn.data.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 同步任务配置实体类
 */
@Data
@TableName("sync_task_config")
public class SyncTaskConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 任务名称
     */
    private String name;

    /**
     * 任务描述
     */
    private String description;

    /**
     * 所属项目
     */
    private String project;

    /**
     * 负责人
     */
    private String owner;

    /**
     * 优先级
     */
    private String priority;

    /**
     * 标签
     */
    private String tags;

    /**
     * 数据源ID
     */
    private Long sourceId;

    /**
     * 关联 watcher 配置ID
     */
    private Long watcherId;

    /**
     * ES集群
     */
    private String esCluster;

    /**
     * SQL语句
     */
    private String sql_;

    /**
     * 字段映射配置
     */
    private String fieldMapping;

    /**
     * 目标ES索引
     */
    private String targetIndex;

    /**
     * 同步模式：full, incremental, full+incremental
     */
    private String syncMode;

    /**
     * 执行策略：immediate, scheduled, triggered
     */
    private String executionStrategy;

    /**
     * 增量字段
     */
    private String incrementalField;

    /**
     * 增量策略：timestamp, autoIncrement, timeWindow, businessLogic
     */
    private String incrementalStrategy;

    /**
     * 是否启用断点续传
     */
    private Boolean enableCheckpoint;

    /**
     * 调度类型：manual, cron
     */
    private String scheduleType;

    /**
     * CRON表达式
     */
    private String cronExpression;

    /**
     * 批次大小
     */
    private Integer batchSize;

    /**
     * 并发线程数
     */
    private Integer concurrentThreads;

    /**
     * 连接超时时间（秒）
     */
    private Integer connectionTimeout;

    /**
     * 查询超时时间（秒）
     */
    private Integer queryTimeout;

    /**
     * 写入超时时间（秒）
     */
    private Integer writeTimeout;

    /**
     * 失败重试次数
     */
    private Integer retryCount;

    /**
     * 重试间隔（秒）
     */
    private Integer retryInterval;

    /**
     * 是否跳过错误记录
     */
    private Boolean skipErrorRecords;

    /**
     * 错误处理策略：continue, pause, terminate
     */
    private String errorHandlingStrategy;

    /**
     * 状态：0-禁用，1-启用
     */
    private Integer status;

    /**
     * 最后同步时间
     */
    private LocalDateTime lastSyncTime;

    /**
     * 最后同步状态
     */
    private String lastSyncStatus;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

}
