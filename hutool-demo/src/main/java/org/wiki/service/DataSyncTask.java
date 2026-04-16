package org.wiki.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.thread.ThreadUtil;
import org.springframework.stereotype.Component;

// 数据同步任务
@Component
public class DataSyncTask implements ScheduledTask {
    
    @Override
    public void execute() {
        System.out.println("[" + DateUtil.now() + "] 执行数据同步任务");
        // 模拟同步操作
        ThreadUtil.sleep(1000);
        System.out.println("数据同步完成");
    }
    
    @Override
    public String getTaskName() {
        return "数据同步";
    }
}