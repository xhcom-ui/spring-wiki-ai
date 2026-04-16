package com.syn.data.service;

import com.syn.data.entity.DataSourceConfig;
import com.syn.data.entity.SyncTaskConfig;
import com.syn.data.mapper.DataSourceConfigMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.*;

/**
 * MySQL Binlog监听服务
 * 负责监听MySQL的Binlog日志变化，实现实时增量同步
 * 
 * MySQL Binlog监听原理：
 * 1. 连接MySQL服务器，伪装为从节点
 * 2. 接收主节点的Binlog事件
 * 3. 解析Binlog事件（INSERT、UPDATE、DELETE等）
 * 4. 将变更同步到Elasticsearch
 */
@Slf4j
@Service
public class MySQLBinlogListenerService {

    @Resource
    private DataSourceConfigMapper dataSourceConfigMapper;

    @Resource
    private DataSyncService dataSyncService;

    @Resource
    private ElasticsearchService elasticsearchService;

    @Resource
    private AlertService alertService;

    // 存储每个数据源的Binlog监听器
    private final Map<Long, MySQLBinlogListener> binlogListeners = new ConcurrentHashMap<>();

    // 存储每个数据源对应的同步任务
    private final Map<Long, List<SyncTaskConfig>> dataSourceTasks = new ConcurrentHashMap<>();

    // 线程池用于处理Binlog事件
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    // 批量处理队列
    private final Map<Long, BlockingQueue<Map<String, Object>>> batchQueues = new ConcurrentHashMap<>();

    // 批量处理定时器
    private final Map<Long, ScheduledFuture<?>> batchTimers = new ConcurrentHashMap<>();

    // 批量大小
    private static final int BATCH_SIZE = 100;

    // 批量处理间隔（毫秒）
    private static final int BATCH_INTERVAL = 1000;

    // 表名到列信息的映射
    private final Map<Long, Map<String, String[]>> tableColumns = new ConcurrentHashMap<>();

    /**
     * 初始化时启动所有增量同步任务
     */
    @PostConstruct
    public void init() {
        log.info("初始化MySQL Binlog监听服务...");
        // 从数据库加载所有启用的MySQL数据源并启动监听
        loadAndStartListeners();
    }

    /**
     * 加载并启动所有MySQL数据源的Binlog监听
     */
    private void loadAndStartListeners() {
        try {
            // 查询所有MySQL数据源
            List<DataSourceConfig> dataSources = dataSourceConfigMapper.selectList(null);
            for (DataSourceConfig dataSource : dataSources) {
                if ("mysql".equalsIgnoreCase(dataSource.getType())) {
                    // 检查是否有对应的增量同步任务
                    List<SyncTaskConfig> tasks = getDataSourceTasks(dataSource.getId());
                    if (!tasks.isEmpty()) {
                        startListener(dataSource.getId());
                    }
                }
            }
        } catch (Exception e) {
            log.error("加载MySQL数据源失败", e);
        }
    }

    /**
     * 销毁时停止所有Binlog监听
     */
    @PreDestroy
    public void destroy() {
        log.info("停止MySQL Binlog监听服务...");
        
        // 停止所有批量处理定时器
        batchTimers.forEach((id, timer) -> {
            timer.cancel(true);
            log.info("停止数据源 {} 的批量处理定时器", id);
        });
        
        // 停止所有Binlog监听
        binlogListeners.forEach((id, listener) -> {
            try {
                listener.stop();
                log.info("停止数据源 {} 的Binlog监听", id);
            } catch (Exception e) {
                log.error("停止数据源 {} 的Binlog监听失败", id, e);
            }
        });
        
        // 处理剩余的批量数据
        batchQueues.forEach((id, queue) -> {
            processBatchQueue(id);
        });
        
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
    }

    /**
     * 启动指定数据源的Binlog监听
     *
     * @param dataSourceId 数据源ID
     */
    public void startListener(Long dataSourceId) {
        if (binlogListeners.containsKey(dataSourceId)) {
            log.warn("数据源 {} 的Binlog监听已在运行", dataSourceId);
            return;
        }

        DataSourceConfig dataSource = dataSourceConfigMapper.selectById(dataSourceId);
        if (dataSource == null) {
            throw new RuntimeException("数据源不存在");
        }

        if (!"mysql".equalsIgnoreCase(dataSource.getType())) {
            throw new RuntimeException("只有MySQL数据源支持Binlog监听");
        }

        try {
            // 初始化批量处理队列
            batchQueues.put(dataSourceId, new LinkedBlockingQueue<>());

            // 创建Binlog监听器
            MySQLBinlogListener listener = new MySQLBinlogListener();

            // 启动Binlog监听
            executorService.submit(() -> {
                try {
                    log.info("启动数据源 {} 的Binlog监听", dataSourceId);
                    listener.start(dataSource);
                } catch (Exception e) {
                    log.error("启动数据源 {} 的Binlog监听失败", dataSourceId, e);
                    
                    // 触发告警
                    alertService.triggerAlert("failure", "high", 
                            String.format("MySQL Binlog监听启动失败，数据源: %d，错误: %s", 
                                    dataSourceId, e.getMessage()), null);
                    
                    binlogListeners.remove(dataSourceId);
                }
            });

            binlogListeners.put(dataSourceId, listener);

            // 启动批量处理定时器
            ScheduledFuture<?> timer = Executors.newSingleThreadScheduledExecutor()
                    .scheduleAtFixedRate(() -> processBatchQueue(dataSourceId), 
                            BATCH_INTERVAL, BATCH_INTERVAL, TimeUnit.MILLISECONDS);
            batchTimers.put(dataSourceId, timer);

        } catch (Exception e) {
            log.error("创建Binlog监听失败", e);
            throw new RuntimeException("创建Binlog监听失败: " + e.getMessage());
        }
    }

    /**
     * 停止指定数据源的Binlog监听
     *
     * @param dataSourceId 数据源ID
     */
    public void stopListener(Long dataSourceId) {
        // 停止批量处理定时器
        ScheduledFuture<?> timer = batchTimers.remove(dataSourceId);
        if (timer != null) {
            timer.cancel(true);
        }

        // 处理剩余的批量数据
        processBatchQueue(dataSourceId);

        // 停止Binlog监听
        MySQLBinlogListener listener = binlogListeners.remove(dataSourceId);
        if (listener != null) {
            try {
                listener.stop();
                log.info("停止数据源 {} 的Binlog监听", dataSourceId);
            } catch (Exception e) {
                log.error("停止数据源 {} 的Binlog监听失败", dataSourceId, e);
            }
        }

        // 清理批量队列
        batchQueues.remove(dataSourceId);
    }

    // 表名到列信息的映射
    private final Map<Long, Map<String, String[]>> tableColumns = new ConcurrentHashMap<>();

    /**
     * 处理批量队列中的数据
     */
    private void processBatchQueue(Long dataSourceId) {
        BlockingQueue<Map<String, Object>> queue = batchQueues.get(dataSourceId);
        if (queue == null || queue.isEmpty()) {
            return;
        }

        List<Map<String, Object>> batch = new ArrayList<>();
        queue.drainTo(batch, BATCH_SIZE);

        if (!batch.isEmpty()) {
            List<SyncTaskConfig> tasks = getDataSourceTasks(dataSourceId);
            for (SyncTaskConfig task : tasks) {
                try {
                    elasticsearchService.bulkIndex(task.getTargetIndex(), batch);
                    log.debug("批量同步 {} 条数据到ES，任务: {}", batch.size(), task.getName());
                } catch (Exception e) {
                    log.error("批量同步数据到ES失败，任务: {}", task.getName(), e);
                    
                    // 触发告警
                    alertService.triggerAlert("failure", "medium", 
                            String.format("MySQL Binlog批量同步失败，任务: %s，错误: %s", 
                                    task.getName(), e.getMessage()), null);
                }
            }
        }
    }

    /**
     * 同步数据到Elasticsearch
     */
    private void syncToElasticsearch(SyncTaskConfig task, Map<String, Object> data) {
        executorService.submit(() -> {
            try {
                log.info("同步数据到ES，任务: {}, 数据: {}", task.getName(), data);
                // 提取文档ID
                String documentId = extractDocumentId(data);
                if (documentId != null) {
                    elasticsearchService.indexDocument(task.getTargetIndex(), documentId, data);
                }
            } catch (Exception e) {
                log.error("同步数据到ES失败，任务: {}", task.getName(), e);
            }
        });
    }

    /**
     * 提取文档ID
     */
    private String extractDocumentId(Map<String, Object> data) {
        // 优先使用 id 字段
        if (data.containsKey("id")) {
            return String.valueOf(data.get("id"));
        }
        
        // 其次使用 _id 字段
        if (data.containsKey("_id")) {
            return String.valueOf(data.get("_id"));
        }
        
        // 尝试使用第一个列作为ID
        for (String key : data.keySet()) {
            if (key.startsWith("column_0")) {
                return String.valueOf(data.get(key));
            }
        }
        
        // 如果没有ID字段，生成一个UUID
        return java.util.UUID.randomUUID().toString();
    }

    /**
     * 从Elasticsearch删除数据
     */
    private void deleteFromElasticsearch(SyncTaskConfig task, String documentId) {
        executorService.submit(() -> {
            try {
                log.info("从ES删除数据，任务: {}, 文档ID: {}", task.getName(), documentId);
                elasticsearchService.deleteDocument(task.getTargetIndex(), documentId);
            } catch (Exception e) {
                log.error("从ES删除数据失败，任务: {}", task.getName(), e);
            }
        });
    }

    /**
     * 获取数据源下的所有增量同步任务
     */
    private List<SyncTaskConfig> getDataSourceTasks(Long dataSourceId) {
        return dataSourceTasks.computeIfAbsent(dataSourceId, id -> new ArrayList<>());
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

        // 如果该数据源的Binlog监听未启动，则启动
        if (!binlogListeners.containsKey(dataSourceId)) {
            startListener(dataSourceId);
        }

        log.info("注册MySQL增量同步任务: {}", task.getName());
    }

    /**
     * 取消注册同步任务
     */
    public void unregisterTask(SyncTaskConfig task) {
        Long dataSourceId = task.getSourceId();
        List<SyncTaskConfig> tasks = dataSourceTasks.get(dataSourceId);
        if (tasks != null) {
            tasks.remove(task);

            // 如果该数据源下没有任务了，停止Binlog监听
            if (tasks.isEmpty()) {
                stopListener(dataSourceId);
            }
        }

        log.info("取消注册MySQL增量同步任务: {}", task.getName());
    }

    /**
     * 获取监听状态
     */
    public Map<String, Object> getListenerStatus(Long dataSourceId) {
        Map<String, Object> status = new HashMap<>();
        
        MySQLBinlogListener listener = binlogListeners.get(dataSourceId);
        boolean isRunning = listener != null && listener.isRunning();
        status.put("isRunning", isRunning);
        status.put("dataSourceId", dataSourceId);
        
        if (isRunning) {
            BlockingQueue<Map<String, Object>> queue = batchQueues.get(dataSourceId);
            status.put("queueSize", queue != null ? queue.size() : 0);
            
            List<SyncTaskConfig> tasks = getDataSourceTasks(dataSourceId);
            status.put("taskCount", tasks.size());
        }
        
        return status;
    }

}
