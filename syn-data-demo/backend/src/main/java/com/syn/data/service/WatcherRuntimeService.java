package com.syn.data.service;

import com.syn.data.entity.SyncTaskConfig;

import java.util.Map;

/**
 * watcher 运行时服务统一接口。
 */
public interface WatcherRuntimeService {

    /**
     * 当前运行时支持的数据源类型。
     */
    String getSourceType();

    /**
     * 启动 watcher。
     */
    void startListener(Long watcherId);

    /**
     * 停止 watcher。
     */
    void stopListener(Long watcherId);

    /**
     * 获取 watcher 运行状态。
     */
    Map<String, Object> getListenerStatus(Long watcherId);

    /**
     * 注册与 watcher 绑定的同步任务。
     */
    void registerTask(SyncTaskConfig task);

    /**
     * 取消注册与 watcher 绑定的同步任务。
     */
    void unregisterTask(SyncTaskConfig task);
}
