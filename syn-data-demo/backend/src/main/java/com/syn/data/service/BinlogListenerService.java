package com.syn.data.service;


import com.syn.data.entity.DataSourceConfig;
import com.syn.data.entity.SyncTaskConfig;
import com.syn.data.mapper.DataSourceConfigMapper;
import com.syn.data.mapper.SyncTaskConfigMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * MySQL Binlog监听服务
 * 负责监听MySQL的binlog变化，实现实时增量同步
 */
@Slf4j
@Service
public class BinlogListenerService {

    @Resource
    private DataSourceConfigMapper dataSourceConfigMapper;

    @Resource
    private SyncTaskConfigMapper syncTaskConfigMapper;

    @Resource
    private DataSyncService dataSyncService;

    // 存储每个数据源的binlog客户端
    private final Map<Long, MySQLBinlogListener> binlogClients = new ConcurrentHashMap<>();
    
    // 存储每个数据源对应的同步任务
    private final Map<Long, List<SyncTaskConfig>> dataSourceTasks = new ConcurrentHashMap<>();
    
    // 线程池用于处理binlog事件
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    /**
     * 初始化时启动所有增量同步任务
     */
    @PostConstruct
    public void init() {
        log.info("初始化Binlog监听服务...");
        // 这里可以加载所有启用的增量同步任务
        // 实际项目中可能需要更复杂的启动逻辑
    }

    /**
     * 销毁时停止所有binlog客户端
     */
    @PreDestroy
    public void destroy() {
        log.info("停止Binlog监听服务...");
        binlogClients.forEach((id, client) -> {
            try {
                client.stop();
                log.info("停止数据源 {} 的binlog监听", id);
            } catch (Exception e) {
                log.error("停止数据源 {} 的binlog监听失败", id, e);
            }
        });
        executorService.shutdown();
    }

    /**
     * 启动指定数据源的binlog监听
     *
     * @param dataSourceId 数据源ID
     */
    public void startListener(Long dataSourceId) {
        if (binlogClients.containsKey(dataSourceId)) {
            log.warn("数据源 {} 的binlog监听已在运行", dataSourceId);
            return;
        }

        DataSourceConfig dataSource = dataSourceConfigMapper.selectById(dataSourceId);
        if (dataSource == null) {
            throw new RuntimeException("数据源不存在");
        }

        if (!"mysql".equalsIgnoreCase(dataSource.getType())) {
            throw new RuntimeException("只有MySQL数据源支持binlog监听");
        }

        try {
            MySQLBinlogListener client = new MySQLBinlogListener();
            binlogClients.put(dataSourceId, client);
            
            // 在后台线程中启动监听
            executorService.submit(() -> {
                try {
                    log.info("启动数据源 {} 的binlog监听", dataSourceId);
                    client.start(dataSource);
                } catch (Exception e) {
                    log.error("启动数据源 {} 的binlog监听失败", dataSourceId, e);
                    binlogClients.remove(dataSourceId);
                }
            });
            
        } catch (Exception e) {
            log.error("创建binlog客户端失败", e);
            throw new RuntimeException("创建binlog客户端失败: " + e.getMessage());
        }
    }

    /**
     * 停止指定数据源的binlog监听
     *
     * @param dataSourceId 数据源ID
     */
    public void stopListener(Long dataSourceId) {
        MySQLBinlogListener client = binlogClients.remove(dataSourceId);
        if (client != null) {
            try {
                client.stop();
                log.info("停止数据源 {} 的binlog监听", dataSourceId);
            } catch (Exception e) {
                log.error("停止数据源 {} 的binlog监听失败", dataSourceId, e);
            }
        }
    }



    /**
     * 获取数据源下的所有增量同步任务
     */
    private List<SyncTaskConfig> getDataSourceTasks(Long dataSourceId) {
        return dataSourceTasks.computeIfAbsent(dataSourceId, id -> {
            // 从数据库加载该数据源下的所有增量同步任务
            // 这里简化实现，实际应该查询数据库
            return new ArrayList<>();
        });
    }

    /**
     * 注册同步任务
     */
    public void registerTask(SyncTaskConfig task) {
        if (!"incremental".equals(task.getSyncMode())) {
            return;
        }

        Long dataSourceId = task.getSourceId();
        dataSourceTasks.computeIfAbsent(dataSourceId, k -> new ArrayList<>()).add(task);
        
        // 如果该数据源的binlog监听未启动，则启动
        if (!binlogClients.containsKey(dataSourceId)) {
            startListener(dataSourceId);
        }
        
        log.info("注册增量同步任务: {}", task.getName());
    }

    /**
     * 取消注册同步任务
     */
    public void unregisterTask(SyncTaskConfig task) {
        Long dataSourceId = task.getSourceId();
        List<SyncTaskConfig> tasks = dataSourceTasks.get(dataSourceId);
        if (tasks != null) {
            tasks.remove(task);
            
            // 如果该数据源下没有任务了，停止binlog监听
            if (tasks.isEmpty()) {
                stopListener(dataSourceId);
            }
        }
        
        log.info("取消注册增量同步任务: {}", task.getName());
    }
}
