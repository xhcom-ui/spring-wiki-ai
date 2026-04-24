package com.syn.data.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.syn.data.controller.dto.WatcherConfigRequest;
import com.syn.data.service.WatcherConfigRecord;
import com.syn.data.service.WatcherConfigService;
import com.syn.data.service.WatcherRuntimeManager;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * watcher 配置控制器。
 */
@RestController
@RequestMapping("/api/watchers")
@SaCheckRole("admin")
public class WatcherConfigController {

    @Resource
    private WatcherConfigService watcherConfigService;

    @Resource
    private WatcherRuntimeManager watcherRuntimeManager;

    @GetMapping
    public List<WatcherConfigRecord> list() {
        return watcherConfigService.list();
    }

    @GetMapping("/{id}")
    public WatcherConfigRecord get(@PathVariable Long id) {
        return watcherConfigService.get(id);
    }

    @PostMapping("/{sourceType}")
    public WatcherConfigRecord create(@PathVariable String sourceType, @RequestBody WatcherConfigRequest request) {
        return watcherConfigService.create(sourceType, request);
    }

    @PutMapping("/{id}")
    public WatcherConfigRecord update(@PathVariable Long id, @RequestBody WatcherConfigRequest request) {
        return watcherConfigService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        if ("running".equalsIgnoreCase(watcherRuntimeManager.requireWatcher(id).getStatus())) {
            watcherRuntimeManager.stop(id);
        }
        watcherConfigService.delete(id);
    }

    @PostMapping("/{id}/start")
    public WatcherConfigRecord start(@PathVariable Long id) {
        watcherRuntimeManager.start(id);
        return watcherConfigService.get(id);
    }

    @PostMapping("/{id}/stop")
    public WatcherConfigRecord stop(@PathVariable Long id) {
        watcherRuntimeManager.stop(id);
        return watcherConfigService.get(id);
    }

    @GetMapping("/{id}/status")
    public Map<String, Object> status(@PathVariable Long id) {
        return watcherRuntimeManager.status(id);
    }
}
