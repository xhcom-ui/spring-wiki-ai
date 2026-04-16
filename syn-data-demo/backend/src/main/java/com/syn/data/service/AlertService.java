package com.syn.data.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 告警服务
 * 负责告警规则配置和通知
 */
@Slf4j
@Service
public class AlertService {

    /**
     * 告警规则配置
     */
    public static class AlertRule {
        private Long id;
        private String name;
        private String type; // failure, timeout, dataVolume, delay, quality
        private String condition;
        private String severity; // low, medium, high
        private boolean enabled;
        private List<String> notificationMethods; // email, wechat, dingtalk, sms, phone
        private List<String> recipients;
        private String description;

        // Getters and setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getCondition() { return condition; }
        public void setCondition(String condition) { this.condition = condition; }
        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public List<String> getNotificationMethods() { return notificationMethods; }
        public void setNotificationMethods(List<String> notificationMethods) { this.notificationMethods = notificationMethods; }
        public List<String> getRecipients() { return recipients; }
        public void setRecipients(List<String> recipients) { this.recipients = recipients; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    /**
     * 告警记录
     */
    public static class AlertRecord {
        private Long id;
        private Long ruleId;
        private String ruleName;
        private String type;
        private String severity;
        private String message;
        private Date createTime;
        private Date resolveTime;
        private String status; // active, resolved

        // Getters and setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getRuleId() { return ruleId; }
        public void setRuleId(Long ruleId) { this.ruleId = ruleId; }
        public String getRuleName() { return ruleName; }
        public void setRuleName(String ruleName) { this.ruleName = ruleName; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public Date getCreateTime() { return createTime; }
        public void setCreateTime(Date createTime) { this.createTime = createTime; }
        public Date getResolveTime() { return resolveTime; }
        public void setResolveTime(Date resolveTime) { this.resolveTime = resolveTime; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    // 模拟告警规则存储
    private final List<AlertRule> alertRules = new ArrayList<>();
    // 模拟告警记录存储
    private final List<AlertRecord> alertRecords = new ArrayList<>();

    public AlertService() {
        // 初始化默认告警规则
        initDefaultRules();
    }

    /**
     * 初始化默认告警规则
     */
    private void initDefaultRules() {
        AlertRule rule1 = new AlertRule();
        rule1.setId(1L);
        rule1.setName("任务失败告警");
        rule1.setType("failure");
        rule1.setCondition("任务执行失败");
        rule1.setSeverity("high");
        rule1.setEnabled(true);
        rule1.setNotificationMethods(Arrays.asList("email", "wechat"));
        rule1.setRecipients(Arrays.asList("admin@example.com", "user@example.com"));
        rule1.setDescription("当同步任务执行失败时触发告警");
        alertRules.add(rule1);

        AlertRule rule2 = new AlertRule();
        rule2.setId(2L);
        rule2.setName("任务超时告警");
        rule2.setType("timeout");
        rule2.setCondition("执行时间超过30分钟");
        rule2.setSeverity("medium");
        rule2.setEnabled(true);
        rule2.setNotificationMethods(Arrays.asList("email"));
        rule2.setRecipients(Arrays.asList("admin@example.com"));
        rule2.setDescription("当同步任务执行时间超过30分钟时触发告警");
        alertRules.add(rule2);

        AlertRule rule3 = new AlertRule();
        rule3.setId(3L);
        rule3.setName("数据量异常告警");
        rule3.setType("dataVolume");
        rule3.setCondition("数据量波动超过50%");
        rule3.setSeverity("medium");
        rule3.setEnabled(true);
        rule3.setNotificationMethods(Arrays.asList("email"));
        rule3.setRecipients(Arrays.asList("admin@example.com"));
        rule3.setDescription("当同步数据量波动超过50%时触发告警");
        alertRules.add(rule3);
    }

    /**
     * 获取所有告警规则
     */
    public List<AlertRule> getAlertRules() {
        return alertRules;
    }

    /**
     * 根据ID获取告警规则
     */
    public AlertRule getAlertRuleById(Long id) {
        return alertRules.stream()
                .filter(rule -> rule.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    /**
     * 创建告警规则
     */
    public AlertRule createAlertRule(AlertRule rule) {
        long newId = alertRules.size() + 1;
        rule.setId(newId);
        rule.setEnabled(true);
        alertRules.add(rule);
        return rule;
    }

    /**
     * 更新告警规则
     */
    public AlertRule updateAlertRule(AlertRule rule) {
        AlertRule existingRule = getAlertRuleById(rule.getId());
        if (existingRule != null) {
            existingRule.setName(rule.getName());
            existingRule.setType(rule.getType());
            existingRule.setCondition(rule.getCondition());
            existingRule.setSeverity(rule.getSeverity());
            existingRule.setEnabled(rule.isEnabled());
            existingRule.setNotificationMethods(rule.getNotificationMethods());
            existingRule.setRecipients(rule.getRecipients());
            existingRule.setDescription(rule.getDescription());
        }
        return existingRule;
    }

    /**
     * 删除告警规则
     */
    public boolean deleteAlertRule(Long id) {
        return alertRules.removeIf(rule -> rule.getId().equals(id));
    }

    /**
     * 启用/禁用告警规则
     */
    public boolean toggleAlertRule(Long id, boolean enabled) {
        AlertRule rule = getAlertRuleById(id);
        if (rule != null) {
            rule.setEnabled(enabled);
            return true;
        }
        return false;
    }

    /**
     * 获取告警记录
     */
    public List<AlertRecord> getAlertRecords(int limit, String status) {
        List<AlertRecord> filteredRecords = alertRecords;
        if (status != null) {
            filteredRecords = alertRecords.stream()
                    .filter(record -> status.equals(record.getStatus()))
                    .collect(java.util.stream.Collectors.toList());
        }
        return filteredRecords.stream()
                .sorted((a, b) -> b.getCreateTime().compareTo(a.getCreateTime()))
                .limit(limit)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 触发告警
     */
    public AlertRecord triggerAlert(String type, String severity, String message, Long ruleId) {
        AlertRecord record = new AlertRecord();
        record.setId((long) (alertRecords.size() + 1));
        record.setRuleId(ruleId);
        record.setRuleName(ruleId != null ? getAlertRuleById(ruleId).getName() : "手动告警");
        record.setType(type);
        record.setSeverity(severity);
        record.setMessage(message);
        record.setCreateTime(new Date());
        record.setStatus("active");

        alertRecords.add(record);

        // 发送通知
        sendNotification(record);

        log.info("告警触发: {} - {}", severity, message);
        return record;
    }

    /**
     * 解决告警
     */
    public boolean resolveAlert(Long id) {
        AlertRecord record = alertRecords.stream()
                .filter(r -> r.getId().equals(id))
                .findFirst()
                .orElse(null);
        if (record != null) {
            record.setResolveTime(new Date());
            record.setStatus("resolved");
            return true;
        }
        return false;
    }

    /**
     * 发送告警通知
     */
    private void sendNotification(AlertRecord record) {
        // 简化实现，实际应该调用对应的通知服务
        log.info("发送告警通知: {} - {}", record.getSeverity(), record.getMessage());
        
        // 模拟发送不同类型的通知
        if (record.getRuleId() != null) {
            AlertRule rule = getAlertRuleById(record.getRuleId());
            if (rule != null && rule.isEnabled()) {
                for (String method : rule.getNotificationMethods()) {
                    switch (method) {
                        case "email":
                            sendEmailNotification(record, rule.getRecipients());
                            break;
                        case "wechat":
                            sendWechatNotification(record, rule.getRecipients());
                            break;
                        case "dingtalk":
                            sendDingtalkNotification(record, rule.getRecipients());
                            break;
                        case "sms":
                            sendSmsNotification(record, rule.getRecipients());
                            break;
                        case "phone":
                            sendPhoneNotification(record, rule.getRecipients());
                            break;
                    }
                }
            }
        }
    }

    /**
     * 发送邮件通知
     */
    private void sendEmailNotification(AlertRecord record, List<String> recipients) {
        log.info("发送邮件通知给 {}: {} - {}", recipients, record.getSeverity(), record.getMessage());
        // 实际实现：调用邮件服务发送邮件
    }

    /**
     * 发送企业微信通知
     */
    private void sendWechatNotification(AlertRecord record, List<String> recipients) {
        log.info("发送企业微信通知给 {}: {} - {}", recipients, record.getSeverity(), record.getMessage());
        // 实际实现：调用企业微信API发送通知
    }

    /**
     * 发送钉钉通知
     */
    private void sendDingtalkNotification(AlertRecord record, List<String> recipients) {
        log.info("发送钉钉通知给 {}: {} - {}", recipients, record.getSeverity(), record.getMessage());
        // 实际实现：调用钉钉API发送通知
    }

    /**
     * 发送短信通知
     */
    private void sendSmsNotification(AlertRecord record, List<String> recipients) {
        log.info("发送短信通知给 {}: {} - {}", recipients, record.getSeverity(), record.getMessage());
        // 实际实现：调用短信服务发送短信
    }

    /**
     * 发送电话通知
     */
    private void sendPhoneNotification(AlertRecord record, List<String> recipients) {
        log.info("发送电话通知给 {}: {} - {}", recipients, record.getSeverity(), record.getMessage());
        // 实际实现：调用电话服务发送语音通知
    }

    /**
     * 获取告警统计数据
     */
    public Map<String, Object> getAlertStats(String timeRange) {
        Map<String, Object> stats = new HashMap<>();
        
        // 统计不同类型的告警数量
        Map<String, Long> typeStats = alertRecords.stream()
                .filter(record -> isWithinTimeRange(record.getCreateTime(), timeRange))
                .collect(java.util.stream.Collectors.groupingBy(AlertRecord::getType, java.util.stream.Collectors.counting()));
        stats.put("typeStats", typeStats);

        // 统计不同严重程度的告警数量
        Map<String, Long> severityStats = alertRecords.stream()
                .filter(record -> isWithinTimeRange(record.getCreateTime(), timeRange))
                .collect(java.util.stream.Collectors.groupingBy(AlertRecord::getSeverity, java.util.stream.Collectors.counting()));
        stats.put("severityStats", severityStats);

        // 统计告警状态
        Map<String, Long> statusStats = alertRecords.stream()
                .filter(record -> isWithinTimeRange(record.getCreateTime(), timeRange))
                .collect(java.util.stream.Collectors.groupingBy(AlertRecord::getStatus, java.util.stream.Collectors.counting()));
        stats.put("statusStats", statusStats);

        // 统计告警趋势
        List<Map<String, Object>> trendData = getAlertTrend(timeRange);
        stats.put("trendData", trendData);

        return stats;
    }

    /**
     * 获取告警趋势
     */
    private List<Map<String, Object>> getAlertTrend(String timeRange) {
        List<Map<String, Object>> trendData = new ArrayList<>();
        
        // 模拟趋势数据
        for (int i = 0; i < 7; i++) {
            Map<String, Object> data = new HashMap<>();
            data.put("date", new Date(System.currentTimeMillis() - i * 24 * 60 * 60 * 1000));
            data.put("alertCount", 5 + (i % 3));
            trendData.add(data);
        }
        
        return trendData;
    }

    /**
     * 判断时间是否在指定范围内
     */
    private boolean isWithinTimeRange(Date date, String timeRange) {
        // 简化实现，实际应该根据时间范围进行判断
        return true;
    }

    /**
     * 测试告警通知
     */
    public Map<String, Object> testAlertNotification(String method, List<String> recipients) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 模拟发送测试通知
            log.info("测试告警通知: {} - {}", method, recipients);
            
            switch (method) {
                case "email":
                    sendEmailNotification(null, recipients);
                    break;
                case "wechat":
                    sendWechatNotification(null, recipients);
                    break;
                case "dingtalk":
                    sendDingtalkNotification(null, recipients);
                    break;
                case "sms":
                    sendSmsNotification(null, recipients);
                    break;
                case "phone":
                    sendPhoneNotification(null, recipients);
                    break;
            }
            
            result.put("success", true);
            result.put("message", "测试通知发送成功");
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "测试通知发送失败: " + e.getMessage());
        }
        
        return result;
    }

}
