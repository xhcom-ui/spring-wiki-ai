package com.syn.data.service;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 内存态 watcher 配置记录。
 */
@Data
public class WatcherConfigRecord {

    private Long id;

    private String sourceType;

    private Long sourceId;

    private String hostName;

    private String database;

    private String table;

    private String targetIndex;

    private String incrementalField;

    private List<String> eventTypes;

    private String description;

    private String status;

    private long queueSize;

    private long syncedCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime lastSyncTime;
}
