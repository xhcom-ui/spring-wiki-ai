package org.wiki.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.thread.ThreadUtil;
import org.springframework.stereotype.Component;

// 日志清理任务
@Component
public class LogCleanTask implements ScheduledTask {
    
    @Override
    public void execute() {
        System.out.println("[" + DateUtil.now() + "] 执行日志清理任务");
        // 模拟清理操作
        ThreadUtil.sleep(500);
        System.out.println("日志清理完成");
    }
    
    @Override
    public String getTaskName() {
        return "日志清理";
    }
}