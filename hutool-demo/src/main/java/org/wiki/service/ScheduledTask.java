package org.wiki.service;

// 任务接口
public interface ScheduledTask {
    void execute();
    String getTaskName();
}