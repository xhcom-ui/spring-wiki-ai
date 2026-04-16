package com.gateway.controller;

import com.gateway.manager.PluginManager;
import com.gateway.plugin.GatewayPlugin;
import com.gateway.plugin.impl.GrayscalePlugin;
import com.gateway.plugin.impl.TimeoutPlugin;
import com.gateway.plugin.impl.TrafficRecordingPlugin;
import com.gateway.plugin.impl.TrafficReplayPlugin;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 插件管理控制器
 */
@RestController
@RequestMapping("/api/plugins")
public class PluginController {

    @Resource
    private PluginManager pluginManager;

    /**
     * 获取所有插件
     */
    @GetMapping
    public Map<String, Object> getAllPlugins() {
        return pluginManager.getPlugins().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> Map.of(
                                "name", entry.getKey(),
                                "enabled", entry.getValue().isEnabled()
                        )
                ));
    }

    /**
     * 启动插件
     */
    @PostMapping("/{pluginName}/start")
    public Map<String, String> startPlugin(@PathVariable String pluginName) {
        pluginManager.startPlugin(pluginName);
        return Map.of("message", "插件启动成功: " + pluginName);
    }

    /**
     * 停止插件
     */
    @PostMapping("/{pluginName}/stop")
    public Map<String, String> stopPlugin(@PathVariable String pluginName) {
        pluginManager.stopPlugin(pluginName);
        return Map.of("message", "插件停止成功: " + pluginName);
    }

    /**
     * 重新加载插件
     */
    @PostMapping("/reload")
    public Map<String, String> reloadPlugins() {
        pluginManager.reloadPlugins();
        return Map.of("message", "插件重新加载完成");
    }

    /**
     * 获取插件状态
     */
    @GetMapping("/{pluginName}")
    public Map<String, Object> getPluginStatus(@PathVariable String pluginName) {
        GatewayPlugin plugin = pluginManager.getPlugin(pluginName);
        if (plugin == null) {
            return Map.of("error", "插件不存在: " + pluginName);
        }
        
        Map<String, Object> result = Map.of(
                "name", plugin.getName(),
                "enabled", plugin.isEnabled()
        );
        
        // 如果是超时插件，返回超时配置
        if (plugin instanceof TimeoutPlugin) {
            TimeoutPlugin timeoutPlugin = (TimeoutPlugin) plugin;
            result = Map.of(
                    "name", plugin.getName(),
                    "enabled", plugin.isEnabled(),
                    "timeoutConfig", timeoutPlugin.getTimeoutConfig()
            );
        }
        // 如果是灰度发布插件，返回灰度配置
        else if (plugin instanceof GrayscalePlugin) {
            GrayscalePlugin grayscalePlugin = (GrayscalePlugin) plugin;
            result = Map.of(
                    "name", plugin.getName(),
                    "enabled", plugin.isEnabled(),
                    "grayscaleConfig", grayscalePlugin.getGrayscaleConfig()
            );
        }
        // 如果是流量录制插件，返回录制配置
        else if (plugin instanceof TrafficRecordingPlugin) {
            TrafficRecordingPlugin recordingPlugin = (TrafficRecordingPlugin) plugin;
            result = Map.of(
                    "name", plugin.getName(),
                    "enabled", plugin.isEnabled(),
                    "recordingConfig", recordingPlugin.getRecordingConfig()
            );
        }
        // 如果是流量回放插件，返回回放配置
        else if (plugin instanceof TrafficReplayPlugin) {
            TrafficReplayPlugin replayPlugin = (TrafficReplayPlugin) plugin;
            result = Map.of(
                    "name", plugin.getName(),
                    "enabled", plugin.isEnabled(),
                    "replayConfig", replayPlugin.getReplayConfig()
            );
        }
        
        return result;
    }

    /**
     * 更新超时配置
     */
    @PostMapping("/timeout/config")
    public Map<String, String> updateTimeoutConfig(@RequestBody Map<String, Integer> config) {
        GatewayPlugin plugin = pluginManager.getPlugin("timeout");
        if (plugin == null || !(plugin instanceof TimeoutPlugin)) {
            return Map.of("error", "超时插件不存在");
        }
        
        TimeoutPlugin timeoutPlugin = (TimeoutPlugin) plugin;
        for (Map.Entry<String, Integer> entry : config.entrySet()) {
            timeoutPlugin.setTimeoutConfig(entry.getKey(), entry.getValue());
        }
        
        return Map.of("message", "超时配置更新成功");
    }

    /**
     * 更新灰度配置
     */
    @PostMapping("/grayscale/config")
    public Map<String, String> updateGrayscaleConfig(@RequestBody Map<String, Integer> config) {
        GatewayPlugin plugin = pluginManager.getPlugin("grayscale");
        if (plugin == null || !(plugin instanceof GrayscalePlugin)) {
            return Map.of("error", "灰度发布插件不存在");
        }
        
        GrayscalePlugin grayscalePlugin = (GrayscalePlugin) plugin;
        for (Map.Entry<String, Integer> entry : config.entrySet()) {
            grayscalePlugin.setGrayscaleConfig(entry.getKey(), entry.getValue());
        }
        
        return Map.of("message", "灰度配置更新成功");
    }

    /**
     * 更新流量录制配置
     */
    @PostMapping("/trafficRecording/config")
    public Map<String, String> updateTrafficRecordingConfig(@RequestBody Map<String, Object> config) {
        GatewayPlugin plugin = pluginManager.getPlugin("trafficRecording");
        if (plugin == null || !(plugin instanceof TrafficRecordingPlugin)) {
            return Map.of("error", "流量录制插件不存在");
        }
        
        TrafficRecordingPlugin recordingPlugin = (TrafficRecordingPlugin) plugin;
        
        if (config.containsKey("recordingPath")) {
            recordingPlugin.setRecordingPath((String) config.get("recordingPath"));
        }
        
        if (config.containsKey("includePaths")) {
            recordingPlugin.setIncludePaths((List<String>) config.get("includePaths"));
        }
        
        if (config.containsKey("excludePaths")) {
            recordingPlugin.setExcludePaths((List<String>) config.get("excludePaths"));
        }
        
        return Map.of("message", "流量录制配置更新成功");
    }

    /**
     * 更新流量回放配置
     */
    @PostMapping("/trafficReplay/config")
    public Map<String, String> updateTrafficReplayConfig(@RequestBody Map<String, String> config) {
        GatewayPlugin plugin = pluginManager.getPlugin("trafficReplay");
        if (plugin == null || !(plugin instanceof TrafficReplayPlugin)) {
            return Map.of("error", "流量回放插件不存在");
        }
        
        TrafficReplayPlugin replayPlugin = (TrafficReplayPlugin) plugin;
        
        if (config.containsKey("recordingPath")) {
            replayPlugin.setRecordingPath(config.get("recordingPath"));
        }
        
        if (config.containsKey("targetUrl")) {
            replayPlugin.setTargetUrl(config.get("targetUrl"));
        }
        
        return Map.of("message", "流量回放配置更新成功");
    }

    /**
     * 开始流量回放
     */
    @PostMapping("/trafficReplay/start")
    public Map<String, String> startTrafficReplay() {
        GatewayPlugin plugin = pluginManager.getPlugin("trafficReplay");
        if (plugin == null || !(plugin instanceof TrafficReplayPlugin)) {
            return Map.of("error", "流量回放插件不存在");
        }
        
        TrafficReplayPlugin replayPlugin = (TrafficReplayPlugin) plugin;
        replayPlugin.startReplay();
        
        return Map.of("message", "流量回放开始");
    }
}
