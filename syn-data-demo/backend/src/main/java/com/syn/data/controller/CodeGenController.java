package com.syn.data.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.syn.data.controller.dto.WatcherConfigRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Watcher 配置控制器
 * 老版本会动态生成 Java 监听器源码；当前版本改为前端直接传 watcher 配置。
 */
@RestController
@RequestMapping("/api/codegen")
@SaCheckRole("admin")
public class CodeGenController {

    /**
     * 生成 PostgreSQL watcher 前端配置
     */
    @PostMapping("/pg-listener")
    public Map<String, Object> generatePgListener(@RequestBody WatcherConfigRequest config) {
        return buildWatcherPayload("postgresql", config);
    }

    /**
     * 生成 MySQL watcher 前端配置
     */
    @PostMapping("/mysql-listener")
    public Map<String, Object> generateMysqlListener(@RequestBody WatcherConfigRequest config) {
        return buildWatcherPayload("mysql", config);
    }

    private Map<String, Object> buildWatcherPayload(
            String sourceType,
            WatcherConfigRequest config
    ) {
        Map<String, Object> watcher = new LinkedHashMap<>();
        watcher.put("sourceType", sourceType);
        watcher.put("sourceId", config.getSourceId());
        watcher.put("hostName", config.getHostName());
        watcher.put("database", config.getDatabase());
        watcher.put("table", config.getTable());
        watcher.put("targetIndex", config.getTargetIndex());
        watcher.put("incrementalField", config.getIncrementalField());
        watcher.put("eventTypes", normalizeEventTypes(config.getEventTypes()));
        watcher.put("description", config.getDescription());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("mode", "frontend-config");
        response.put("message", "已切换为前端传入 watcher 配置，不再动态生成 Java 监听器代码");
        response.put("saveEndpoint", String.format("/api/watchers/%s", sourceType));
        response.put("bindTaskEndpoint", "/api/task");
        response.put("startEndpoint", "/api/watchers/{id}/start");
        response.put("watcher", watcher);
        response.put("usage", List.of(
                "前端可直接调用 saveEndpoint 保存 watcher 配置",
                "创建或更新 SyncTask 时传入 watcherId 绑定 watcher",
                "保存后可在实时监控页直接启动或停止该 watcher"
        ));
        return response;
    }

    private List<String> normalizeEventTypes(List<String> eventTypes) {
        if (eventTypes == null || eventTypes.isEmpty()) {
            return List.of("insert", "update", "delete");
        }
        return eventTypes.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .map(String::toLowerCase)
                .distinct()
                .toList();
    }
}
