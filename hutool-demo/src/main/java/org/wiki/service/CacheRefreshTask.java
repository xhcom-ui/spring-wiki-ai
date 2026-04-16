package org.wiki.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.thread.ThreadUtil;

// 缓存刷新任务（动态注册）
public class CacheRefreshTask implements ScheduledTask {
    private String cacheName;
    
    public CacheRefreshTask(String cacheName) {
        this.cacheName = cacheName;
    }
    
    @Override
    public void execute() {
        System.out.println("[" + DateUtil.now() + "] 刷新缓存：" + cacheName);
        ThreadUtil.sleep(300);
        System.out.println("缓存刷新完成");
    }
    
    @Override
    public String getTaskName() {
        return "缓存刷新-" + cacheName;
    }
}