package com.syn.data.controller.dto;

import lombok.Data;

import java.util.List;

/**
 * 前端 watcher 配置请求体。
 * 不再生成 Java 监听器源码，改为前端直接传配置。
 */
@Data
public class WatcherConfigRequest {

    private Long sourceId;

    private String hostName;

    private String database;

    private String table;

    private String targetIndex;

    private String incrementalField;

    private List<String> eventTypes;

    private String description;
}
