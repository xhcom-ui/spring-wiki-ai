package com.syn.data.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.syn.data.entity.AlertConfigEntity;
import com.syn.data.entity.AlertLogEntity;
import com.syn.data.mapper.AlertConfigMapper;
import com.syn.data.mapper.AlertLogMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AlertService {

    public static class AlertRule {
        private Long id;
        private String name;
        private String type;
        private String condition;
        private String severity;
        private boolean enabled;
        private List<String> notificationMethods;
        private List<String> recipients;
        private String description;

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

    public static class AlertRecord {
        private Long id;
        private Long ruleId;
        private String ruleName;
        private String type;
        private String severity;
        private String message;
        private Date createTime;
        private Date resolveTime;
        private String status;

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

    @Resource
    private AlertConfigMapper alertConfigMapper;

    @Resource
    private AlertLogMapper alertLogMapper;

    public List<AlertRule> getAlertRules() {
        return alertConfigMapper.selectList(
                new QueryWrapper<AlertConfigEntity>()
                        .orderByDesc("updated_at")
                        .orderByDesc("id")
        ).stream().map(this::toRule).toList();
    }

    public AlertRule getAlertRuleById(Long id) {
        AlertConfigEntity entity = alertConfigMapper.selectById(id);
        return entity == null ? null : toRule(entity);
    }

    public AlertRule createAlertRule(AlertRule rule) {
        AlertConfigEntity entity = toEntity(rule);
        entity.setEnabled(rule.isEnabled() ? 1 : 0);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        alertConfigMapper.insert(entity);
        return toRule(entity);
    }

    public AlertRule updateAlertRule(AlertRule rule) {
        AlertConfigEntity existing = alertConfigMapper.selectById(rule.getId());
        if (existing == null) {
            return null;
        }
        existing.setName(rule.getName());
        existing.setType(rule.getType());
        existing.setThreshold(rule.getCondition());
        existing.setLevel(rule.getSeverity());
        existing.setEnabled(rule.isEnabled() ? 1 : 0);
        existing.setNotificationMethods(join(rule.getNotificationMethods()));
        existing.setRecipients(join(rule.getRecipients()));
        existing.setDescription(rule.getDescription());
        existing.setUpdatedAt(LocalDateTime.now());
        alertConfigMapper.updateById(existing);
        return toRule(existing);
    }

    public boolean deleteAlertRule(Long id) {
        return alertConfigMapper.deleteById(id) > 0;
    }

    public boolean toggleAlertRule(Long id, boolean enabled) {
        AlertConfigEntity entity = alertConfigMapper.selectById(id);
        if (entity == null) {
            return false;
        }
        entity.setEnabled(enabled ? 1 : 0);
        entity.setUpdatedAt(LocalDateTime.now());
        return alertConfigMapper.updateById(entity) > 0;
    }

    public List<AlertRecord> getAlertRecords(int limit, String status) {
        QueryWrapper<AlertLogEntity> wrapper = new QueryWrapper<AlertLogEntity>()
                .orderByDesc("created_at")
                .orderByDesc("id");
        if (status != null && !status.isBlank()) {
            wrapper.eq("status", status);
        }
        if (limit > 0) {
            wrapper.last("limit " + limit);
        }
        return alertLogMapper.selectList(wrapper).stream().map(this::toRecord).toList();
    }

    public AlertRecord triggerAlert(String type, String severity, String message, Long ruleId) {
        AlertConfigEntity rule = ruleId == null ? null : alertConfigMapper.selectById(ruleId);
        AlertLogEntity entity = new AlertLogEntity();
        entity.setAlertId(ruleId);
        entity.setAlertName(rule != null ? rule.getName() : "手动告警");
        entity.setType(type);
        entity.setLevel(severity);
        entity.setMessage(message);
        entity.setStatus("pending");
        entity.setRecipients(rule != null ? rule.getRecipients() : "");
        entity.setCreatedAt(LocalDateTime.now());

        boolean sent = sendNotification(entity, rule);
        entity.setStatus(sent ? "sent" : "failed");
        entity.setSendTime(LocalDateTime.now());
        alertLogMapper.insert(entity);
        return toRecord(entity);
    }

    public boolean resolveAlert(Long id) {
        AlertLogEntity entity = alertLogMapper.selectById(id);
        if (entity == null) {
            return false;
        }
        entity.setStatus("resolved");
        entity.setSendTime(LocalDateTime.now());
        return alertLogMapper.updateById(entity) > 0;
    }

    public Map<String, Object> getAlertStats(String timeRange) {
        List<AlertLogEntity> records = listRecordsWithinRange(timeRange);
        Map<String, Object> stats = new HashMap<>();
        stats.put("typeStats", records.stream().collect(Collectors.groupingBy(AlertLogEntity::getType, Collectors.counting())));
        stats.put("severityStats", records.stream().collect(Collectors.groupingBy(AlertLogEntity::getLevel, Collectors.counting())));
        stats.put("statusStats", records.stream().collect(Collectors.groupingBy(AlertLogEntity::getStatus, Collectors.counting())));
        stats.put("trendData", getAlertTrend(records, resolveStartTime(timeRange)));
        return stats;
    }

    public Map<String, Object> testAlertNotification(String method, List<String> recipients) {
        Map<String, Object> result = new HashMap<>();
        try {
            AlertLogEntity record = new AlertLogEntity();
            record.setLevel("info");
            record.setMessage("测试告警通知");
            switch (method.toLowerCase(Locale.ROOT)) {
                case "email" -> sendEmailNotification(record, recipients);
                case "wechat" -> sendWechatNotification(record, recipients);
                case "dingtalk" -> sendDingtalkNotification(record, recipients);
                case "sms" -> sendSmsNotification(record, recipients);
                case "phone" -> sendPhoneNotification(record, recipients);
                default -> log.info("测试通知仅记录日志: {}", method);
            }
            result.put("success", true);
            result.put("message", "测试通知发送成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "测试通知发送失败: " + e.getMessage());
        }
        return result;
    }

    private boolean sendNotification(AlertLogEntity record, AlertConfigEntity rule) {
        List<String> methods = rule != null ? split(rule.getNotificationMethods()) : List.of("log");
        List<String> recipients = rule != null ? split(rule.getRecipients()) : List.of();
        try {
            for (String method : methods) {
                switch (method.toLowerCase(Locale.ROOT)) {
                    case "email" -> sendEmailNotification(record, recipients);
                    case "wechat" -> sendWechatNotification(record, recipients);
                    case "dingtalk" -> sendDingtalkNotification(record, recipients);
                    case "sms" -> sendSmsNotification(record, recipients);
                    case "phone" -> sendPhoneNotification(record, recipients);
                    default -> log.info("记录告警通知: {} - {}", record.getLevel(), record.getMessage());
                }
            }
            return true;
        } catch (Exception e) {
            log.error("发送告警通知失败", e);
            return false;
        }
    }

    private void sendEmailNotification(AlertLogEntity record, List<String> recipients) {
        log.info("发送邮件通知给 {}: {} - {}", recipients, record.getLevel(), record.getMessage());
    }

    private void sendWechatNotification(AlertLogEntity record, List<String> recipients) {
        log.info("发送企业微信通知给 {}: {} - {}", recipients, record.getLevel(), record.getMessage());
    }

    private void sendDingtalkNotification(AlertLogEntity record, List<String> recipients) {
        log.info("发送钉钉通知给 {}: {} - {}", recipients, record.getLevel(), record.getMessage());
    }

    private void sendSmsNotification(AlertLogEntity record, List<String> recipients) {
        log.info("发送短信通知给 {}: {} - {}", recipients, record.getLevel(), record.getMessage());
    }

    private void sendPhoneNotification(AlertLogEntity record, List<String> recipients) {
        log.info("发送电话通知给 {}: {} - {}", recipients, record.getLevel(), record.getMessage());
    }

    private List<AlertLogEntity> listRecordsWithinRange(String timeRange) {
        return alertLogMapper.selectList(
                new QueryWrapper<AlertLogEntity>()
                        .ge("created_at", resolveStartTime(timeRange))
                        .orderByAsc("created_at")
                        .orderByAsc("id")
        );
    }

    private List<Map<String, Object>> getAlertTrend(List<AlertLogEntity> records, LocalDateTime startTime) {
        Map<LocalDate, Long> grouped = records.stream()
                .collect(Collectors.groupingBy(item -> item.getCreatedAt().toLocalDate(), Collectors.counting()));
        List<Map<String, Object>> trendData = new ArrayList<>();
        for (LocalDate cursor = startTime.toLocalDate(); !cursor.isAfter(LocalDate.now()); cursor = cursor.plusDays(1)) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", cursor.toString());
            item.put("alertCount", grouped.getOrDefault(cursor, 0L));
            trendData.add(item);
        }
        return trendData;
    }

    private LocalDateTime resolveStartTime(String timeRange) {
        if (timeRange == null || timeRange.isBlank()) {
            return LocalDateTime.now().minusDays(7);
        }
        String normalized = timeRange.trim().toLowerCase(Locale.ROOT);
        if (normalized.endsWith("d")) {
            return LocalDateTime.now().minusDays(Long.parseLong(normalized.substring(0, normalized.length() - 1)));
        }
        if (normalized.endsWith("h")) {
            return LocalDateTime.now().minusHours(Long.parseLong(normalized.substring(0, normalized.length() - 1)));
        }
        return LocalDateTime.now().minusDays(7);
    }

    private AlertRule toRule(AlertConfigEntity entity) {
        AlertRule rule = new AlertRule();
        rule.setId(entity.getId());
        rule.setName(entity.getName());
        rule.setType(entity.getType());
        rule.setCondition(entity.getThreshold());
        rule.setSeverity(entity.getLevel());
        rule.setEnabled(entity.getEnabled() != null && entity.getEnabled() == 1);
        rule.setNotificationMethods(split(entity.getNotificationMethods()));
        rule.setRecipients(split(entity.getRecipients()));
        rule.setDescription(entity.getDescription());
        return rule;
    }

    private AlertConfigEntity toEntity(AlertRule rule) {
        AlertConfigEntity entity = new AlertConfigEntity();
        entity.setId(rule.getId());
        entity.setName(rule.getName());
        entity.setType(rule.getType());
        entity.setThreshold(rule.getCondition());
        entity.setLevel(rule.getSeverity());
        entity.setNotificationMethods(join(rule.getNotificationMethods()));
        entity.setRecipients(join(rule.getRecipients()));
        entity.setDescription(rule.getDescription());
        return entity;
    }

    private AlertRecord toRecord(AlertLogEntity entity) {
        AlertRecord record = new AlertRecord();
        record.setId(entity.getId());
        record.setRuleId(entity.getAlertId());
        record.setRuleName(entity.getAlertName());
        record.setType(entity.getType());
        record.setSeverity(entity.getLevel());
        record.setMessage(entity.getMessage());
        record.setCreateTime(toDate(entity.getCreatedAt()));
        record.setResolveTime(toDate(entity.getSendTime()));
        record.setStatus(entity.getStatus());
        return record;
    }

    private Date toDate(LocalDateTime value) {
        if (value == null) {
            return null;
        }
        Instant instant = value.atZone(ZoneId.systemDefault()).toInstant();
        return Date.from(instant);
    }

    private List<String> split(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .distinct()
                .toList();
    }

    private String join(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .distinct()
                .collect(Collectors.joining(","));
    }
}
