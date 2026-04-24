package com.syn.data.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * watcher 配置实体。
 */
@Data
@TableName("watcher_config")
public class WatcherConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String sourceType;

    private Long sourceId;

    private String hostName;

    @TableField("database_name")
    private String database;

    @TableField("table_name")
    private String table;

    private String targetIndex;

    private String incrementalField;

    @TableField("event_types")
    private String eventTypesText;

    private String description;

    private String status;

    private Long queueSize;

    private Long syncedCount;

    private LocalDateTime lastSyncTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableField(exist = false)
    private List<String> eventTypes;
}
