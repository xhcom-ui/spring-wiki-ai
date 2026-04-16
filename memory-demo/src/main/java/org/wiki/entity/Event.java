package org.wiki.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDateTime;

@Entity
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String scope; // 作用域：用户、会话、任务等
    private LocalDateTime timestamp;
    private String inputObservation; // 输入观测：messages / 环境状态片段
    private String systemAction; // 系统动作：对外输出或Memory Tool的动作
    private String memoryChange; // 记忆变更：ADD/UPDATE/DELETE/NONE
    private String feedbackSignal; // 反馈信号：reward / 用户评分 / 任务成败等
    private String decisionMetadata; // 决策元数据：候选集合、命中证据、early stop阈值等
    private Long memoryId; // 关联的Memory ID

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getInputObservation() {
        return inputObservation;
    }

    public void setInputObservation(String inputObservation) {
        this.inputObservation = inputObservation;
    }

    public String getSystemAction() {
        return systemAction;
    }

    public void setSystemAction(String systemAction) {
        this.systemAction = systemAction;
    }

    public String getMemoryChange() {
        return memoryChange;
    }

    public void setMemoryChange(String memoryChange) {
        this.memoryChange = memoryChange;
    }

    public String getFeedbackSignal() {
        return feedbackSignal;
    }

    public void setFeedbackSignal(String feedbackSignal) {
        this.feedbackSignal = feedbackSignal;
    }

    public String getDecisionMetadata() {
        return decisionMetadata;
    }

    public void setDecisionMetadata(String decisionMetadata) {
        this.decisionMetadata = decisionMetadata;
    }

    public Long getMemoryId() {
        return memoryId;
    }

    public void setMemoryId(Long memoryId) {
        this.memoryId = memoryId;
    }
}
