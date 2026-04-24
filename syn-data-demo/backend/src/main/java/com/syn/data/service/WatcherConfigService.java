package com.syn.data.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.syn.data.controller.dto.WatcherConfigRequest;
import com.syn.data.entity.DataSourceConfig;
import com.syn.data.entity.WatcherConfig;
import com.syn.data.mapper.DataSourceConfigMapper;
import com.syn.data.mapper.WatcherConfigMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * watcher 配置管理服务。
 */
@Service
public class WatcherConfigService {

    @Resource
    private WatcherConfigMapper watcherConfigMapper;

    @Resource
    private DataSourceConfigMapper dataSourceConfigMapper;

    public List<WatcherConfigRecord> list() {
        return watcherConfigMapper.selectList(
                        new QueryWrapper<WatcherConfig>()
                                .orderByDesc("update_time")
                                .orderByDesc("id")
                ).stream()
                .map(this::toRecord)
                .toList();
    }

    public List<WatcherConfig> listEntitiesBySourceType(String sourceType) {
        return watcherConfigMapper.selectList(
                new QueryWrapper<WatcherConfig>()
                        .eq("source_type", sourceType)
                        .orderByDesc("update_time")
                        .orderByDesc("id")
        ).stream().peek(entity -> entity.setEventTypes(parseEventTypes(entity.getEventTypesText()))).toList();
    }

    public WatcherConfigRecord get(Long id) {
        return toRecord(requireEntity(id));
    }

    public WatcherConfigRecord create(String sourceType, WatcherConfigRequest request) {
        DataSourceConfig dataSource = requireDataSource(request.getSourceId(), sourceType);
        LocalDateTime now = LocalDateTime.now();
        WatcherConfig entity = new WatcherConfig();
        entity.setSourceType(sourceType);
        entity.setSourceId(dataSource.getId());
        entity.setHostName(defaultText(request.getHostName(), dataSource.getHost()));
        entity.setDatabase(defaultText(request.getDatabase(), dataSource.getDatabaseName()));
        entity.setTable(defaultText(request.getTable(), ""));
        entity.setTargetIndex(request.getTargetIndex());
        entity.setIncrementalField(request.getIncrementalField());
        entity.setEventTypesText(String.join(",", normalizeEventTypes(request.getEventTypes())));
        entity.setDescription(request.getDescription());
        entity.setStatus("stopped");
        entity.setQueueSize(0L);
        entity.setSyncedCount(0L);
        entity.setCreateTime(now);
        entity.setUpdateTime(now);
        watcherConfigMapper.insert(entity);
        return toRecord(entity);
    }

    public WatcherConfigRecord update(Long id, WatcherConfigRequest request) {
        WatcherConfig entity = requireEntity(id);
        DataSourceConfig dataSource = requireDataSource(request.getSourceId(), entity.getSourceType());
        entity.setSourceId(dataSource.getId());
        entity.setHostName(defaultText(request.getHostName(), dataSource.getHost()));
        entity.setDatabase(defaultText(request.getDatabase(), dataSource.getDatabaseName()));
        entity.setTable(defaultText(request.getTable(), entity.getTable()));
        entity.setTargetIndex(request.getTargetIndex());
        entity.setIncrementalField(request.getIncrementalField());
        entity.setEventTypesText(String.join(",", normalizeEventTypes(request.getEventTypes())));
        entity.setDescription(request.getDescription());
        entity.setUpdateTime(LocalDateTime.now());
        watcherConfigMapper.updateById(entity);
        return toRecord(entity);
    }

    public void delete(Long id) {
        if (watcherConfigMapper.deleteById(id) == 0) {
            throw new RuntimeException("watcher 配置不存在");
        }
    }

    public WatcherConfig requireEntity(Long id) {
        WatcherConfig entity = watcherConfigMapper.selectById(id);
        if (entity == null) {
            throw new RuntimeException("watcher 配置不存在");
        }
        entity.setEventTypes(parseEventTypes(entity.getEventTypesText()));
        return entity;
    }

    public void updateRuntimeState(Long id, String status, Long queueSize, Long syncedCount, LocalDateTime lastSyncTime) {
        WatcherConfig entity = requireEntity(id);
        entity.setStatus(status);
        entity.setQueueSize(queueSize);
        entity.setSyncedCount(syncedCount);
        entity.setLastSyncTime(lastSyncTime);
        entity.setUpdateTime(LocalDateTime.now());
        watcherConfigMapper.updateById(entity);
    }

    private WatcherConfigRecord toRecord(WatcherConfig entity) {
        entity.setEventTypes(parseEventTypes(entity.getEventTypesText()));
        WatcherConfigRecord record = new WatcherConfigRecord();
        record.setId(entity.getId());
        record.setSourceType(entity.getSourceType());
        record.setSourceId(entity.getSourceId());
        record.setHostName(entity.getHostName());
        record.setDatabase(entity.getDatabase());
        record.setTable(entity.getTable());
        record.setTargetIndex(entity.getTargetIndex());
        record.setIncrementalField(entity.getIncrementalField());
        record.setEventTypes(entity.getEventTypes());
        record.setDescription(entity.getDescription());
        record.setStatus(entity.getStatus());
        record.setQueueSize(entity.getQueueSize() == null ? 0L : entity.getQueueSize());
        record.setSyncedCount(entity.getSyncedCount() == null ? 0L : entity.getSyncedCount());
        record.setCreatedAt(entity.getCreateTime());
        record.setUpdatedAt(entity.getUpdateTime());
        record.setLastSyncTime(entity.getLastSyncTime());
        return record;
    }

    private DataSourceConfig requireDataSource(Long sourceId, String expectedType) {
        if (sourceId == null) {
            throw new RuntimeException("sourceId 不能为空");
        }
        DataSourceConfig dataSource = dataSourceConfigMapper.selectById(sourceId);
        if (dataSource == null) {
            throw new RuntimeException("关联数据源不存在");
        }
        if (!Objects.equals(expectedType.toLowerCase(), dataSource.getType().toLowerCase())) {
            throw new RuntimeException("watcher 类型与数据源类型不匹配");
        }
        return dataSource;
    }

    private List<String> normalizeEventTypes(List<String> eventTypes) {
        if (eventTypes == null || eventTypes.isEmpty()) {
            return List.of("insert", "update", "delete");
        }
        List<String> normalized = new ArrayList<>();
        for (String eventType : eventTypes) {
            if (eventType == null) {
                continue;
            }
            String value = eventType.trim().toLowerCase();
            if (!value.isEmpty() && !normalized.contains(value)) {
                normalized.add(value);
            }
        }
        return normalized.isEmpty() ? List.of("insert", "update", "delete") : normalized;
    }

    private List<String> parseEventTypes(String text) {
        if (text == null || text.isBlank()) {
            return List.of("insert", "update", "delete");
        }
        List<String> result = new ArrayList<>();
        for (String part : text.split(",")) {
            String value = part.trim();
            if (!value.isEmpty() && !result.contains(value)) {
                result.add(value);
            }
        }
        return result.isEmpty() ? List.of("insert", "update", "delete") : result;
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
