package org.wiki.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDateTime;

@Entity
public class Policy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name; // 策略名称
    private String scope; // 适用范围
    private int readLimit; // 读取限制：每次最多读取多少条
    private int writeThreshold; // 写入阈值：什么条件下写入
    private int updateInterval; // 更新间隔：多久更新一次
    private int retentionDays; // 保留天数：记忆保留多久
    private String forgettingStrategy; // 遗忘策略：如何遗忘旧记忆
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public int getReadLimit() {
        return readLimit;
    }

    public void setReadLimit(int readLimit) {
        this.readLimit = readLimit;
    }

    public int getWriteThreshold() {
        return writeThreshold;
    }

    public void setWriteThreshold(int writeThreshold) {
        this.writeThreshold = writeThreshold;
    }

    public int getUpdateInterval() {
        return updateInterval;
    }

    public void setUpdateInterval(int updateInterval) {
        this.updateInterval = updateInterval;
    }

    public int getRetentionDays() {
        return retentionDays;
    }

    public void setRetentionDays(int retentionDays) {
        this.retentionDays = retentionDays;
    }

    public String getForgettingStrategy() {
        return forgettingStrategy;
    }

    public void setForgettingStrategy(String forgettingStrategy) {
        this.forgettingStrategy = forgettingStrategy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
