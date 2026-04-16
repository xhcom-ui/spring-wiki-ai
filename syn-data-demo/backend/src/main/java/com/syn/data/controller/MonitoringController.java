package com.syn.data.controller;

import com.syn.data.service.MonitoringService;
import com.syn.data.service.AlertService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Map;
import java.util.List;

/**
 * 监控与告警控制器
 */
@RestController
@RequestMapping("/api/monitoring")
public class MonitoringController {

    @Resource
    private MonitoringService monitoringService;

    @Resource
    private AlertService alertService;

    /**
     * 获取实时监控数据
     */
    @GetMapping("/realtime")
    public Map<String, Object> getRealTimeData() {
        return monitoringService.getRealTimeMonitoringData();
    }

    /**
     * 获取历史监控数据
     */
    @GetMapping("/history")
    public Map<String, Object> getHistoryData(@RequestParam String timeRange) {
        return monitoringService.getHistoryMonitoringData(timeRange);
    }

    /**
     * 获取任务执行详情
     */
    @GetMapping("/task/detail")
    public Map<String, Object> getTaskDetail(@RequestParam Long taskId, @RequestParam Long logId) {
        return monitoringService.getTaskExecutionDetail(taskId, logId);
    }

    /**
     * 导出监控数据
     */
    @PostMapping("/export")
    public Map<String, Object> exportData(@RequestParam String timeRange, @RequestParam String format) {
        return monitoringService.exportMonitoringData(timeRange, format);
    }

    /**
     * 获取所有告警规则
     */
    @GetMapping("/alerts/rules")
    public List<AlertService.AlertRule> getAlertRules() {
        return alertService.getAlertRules();
    }

    /**
     * 根据ID获取告警规则
     */
    @GetMapping("/alerts/rules/{id}")
    public AlertService.AlertRule getAlertRuleById(@PathVariable Long id) {
        return alertService.getAlertRuleById(id);
    }

    /**
     * 创建告警规则
     */
    @PostMapping("/alerts/rules")
    public AlertService.AlertRule createAlertRule(@RequestBody AlertService.AlertRule rule) {
        return alertService.createAlertRule(rule);
    }

    /**
     * 更新告警规则
     */
    @PutMapping("/alerts/rules")
    public AlertService.AlertRule updateAlertRule(@RequestBody AlertService.AlertRule rule) {
        return alertService.updateAlertRule(rule);
    }

    /**
     * 删除告警规则
     */
    @DeleteMapping("/alerts/rules/{id}")
    public boolean deleteAlertRule(@PathVariable Long id) {
        return alertService.deleteAlertRule(id);
    }

    /**
     * 启用/禁用告警规则
     */
    @PostMapping("/alerts/rules/{id}/toggle")
    public boolean toggleAlertRule(@PathVariable Long id, @RequestParam boolean enabled) {
        return alertService.toggleAlertRule(id, enabled);
    }

    /**
     * 获取告警记录
     */
    @GetMapping("/alerts/records")
    public List<AlertService.AlertRecord> getAlertRecords(
            @RequestParam int limit,
            @RequestParam(required = false) String status) {
        return alertService.getAlertRecords(limit, status);
    }

    /**
     * 触发告警
     */
    @PostMapping("/alerts/trigger")
    public AlertService.AlertRecord triggerAlert(
            @RequestParam String type,
            @RequestParam String severity,
            @RequestParam String message,
            @RequestParam(required = false) Long ruleId) {
        return alertService.triggerAlert(type, severity, message, ruleId);
    }

    /**
     * 解决告警
     */
    @PostMapping("/alerts/resolve/{id}")
    public boolean resolveAlert(@PathVariable Long id) {
        return alertService.resolveAlert(id);
    }

    /**
     * 获取告警统计数据
     */
    @GetMapping("/alerts/stats")
    public Map<String, Object> getAlertStats(@RequestParam String timeRange) {
        return alertService.getAlertStats(timeRange);
    }

    /**
     * 测试告警通知
     */
    @PostMapping("/alerts/test-notification")
    public Map<String, Object> testAlertNotification(
            @RequestParam String method,
            @RequestParam List<String> recipients) {
        return alertService.testAlertNotification(method, recipients);
    }

}
