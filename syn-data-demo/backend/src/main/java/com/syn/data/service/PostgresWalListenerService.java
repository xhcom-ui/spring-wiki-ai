package com.syn.data.service;

import com.syn.data.entity.DataSourceConfig;
import com.syn.data.entity.SyncTaskConfig;
import com.syn.data.mapper.DataSourceConfigMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.nio.ByteBuffer;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * PostgreSQL WAL逻辑解码服务
 * 负责监听PostgreSQL的WAL日志变化，实现实时增量同步
 * 
 * PostgreSQL逻辑解码原理：
 * 1. 使用pgoutput插件捕获数据库变更
 * 2. 通过复制协议连接到PostgreSQL
 * 3. 解析WAL日志中的变更事件
 * 4. 将变更同步到Elasticsearch
 */
@Slf4j
@Service
public class PostgresWalListenerService {

    @Resource
    private DataSourceConfigMapper dataSourceConfigMapper;

    @Resource
    private DataSyncService dataSyncService;

    @Resource
    private ElasticsearchService elasticsearchService;

    @Resource
    private AlertService alertService;

    // 存储每个数据源的WAL监听任务
    private final Map<Long, Future<?>> walListeners = new ConcurrentHashMap<>();

    // 存储每个数据源对应的同步任务
    private final Map<Long, List<SyncTaskConfig>> dataSourceTasks = new ConcurrentHashMap<>();

    // 线程池用于处理WAL事件
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    // 存储每个数据源的复制槽名称
    private final Map<Long, String> replicationSlots = new ConcurrentHashMap<>();

    // 批量处理队列
    private final Map<Long, BlockingQueue<Map<String, Object>>> batchQueues = new ConcurrentHashMap<>();

    // 批量处理定时器
    private final Map<Long, ScheduledFuture<?>> batchTimers = new ConcurrentHashMap<>();

    // 批量大小
    private static final int BATCH_SIZE = 100;

    // 批量处理间隔（毫秒）
    private static final int BATCH_INTERVAL = 1000;

    /**
     * 初始化时启动所有增量同步任务
     */
    @PostConstruct
    public void init() {
        log.info("初始化PostgreSQL WAL监听服务...");
        // 从数据库加载所有启用的PostgreSQL数据源并启动监听
        loadAndStartListeners();
    }

    /**
     * 加载并启动所有PostgreSQL数据源的WAL监听
     */
    private void loadAndStartListeners() {
        try {
            // 查询所有PostgreSQL数据源
            List<DataSourceConfig> dataSources = dataSourceConfigMapper.selectList(null);
            for (DataSourceConfig dataSource : dataSources) {
                if ("postgresql".equalsIgnoreCase(dataSource.getType())) {
                    // 检查是否有对应的增量同步任务
                    List<SyncTaskConfig> tasks = getDataSourceTasks(dataSource.getId());
                    if (!tasks.isEmpty()) {
                        startListener(dataSource.getId());
                    }
                }
            }
        } catch (Exception e) {
            log.error("加载PostgreSQL数据源失败", e);
        }
    }

    /**
     * 销毁时停止所有WAL监听
     */
    @PreDestroy
    public void destroy() {
        log.info("停止PostgreSQL WAL监听服务...");
        
        // 停止所有批量处理定时器
        batchTimers.forEach((id, timer) -> {
            timer.cancel(true);
            log.info("停止数据源 {} 的批量处理定时器", id);
        });
        
        // 停止所有WAL监听
        walListeners.forEach((id, future) -> {
            future.cancel(true);
            log.info("停止数据源 {} 的WAL监听", id);
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
     * 启动指定数据源的WAL监听
     *
     * @param dataSourceId 数据源ID
     */
    public void startListener(Long dataSourceId) {
        if (walListeners.containsKey(dataSourceId)) {
            log.warn("数据源 {} 的WAL监听已在运行", dataSourceId);
            return;
        }

        DataSourceConfig dataSource = dataSourceConfigMapper.selectById(dataSourceId);
        if (dataSource == null) {
            throw new RuntimeException("数据源不存在");
        }

        if (!"postgresql".equalsIgnoreCase(dataSource.getType())) {
            throw new RuntimeException("只有PostgreSQL数据源支持WAL监听");
        }

        try {
            // 初始化批量处理队列
            batchQueues.put(dataSourceId, new LinkedBlockingQueue<>());

            // 创建复制槽
            String slotName = createReplicationSlot(dataSource);
            replicationSlots.put(dataSourceId, slotName);

            // 启动批量处理定时器
            ScheduledFuture<?> timer = Executors.newSingleThreadScheduledExecutor()
                    .scheduleAtFixedRate(() -> processBatchQueue(dataSourceId), 
                            BATCH_INTERVAL, BATCH_INTERVAL, TimeUnit.MILLISECONDS);
            batchTimers.put(dataSourceId, timer);

            // 启动WAL监听
            Future<?> future = executorService.submit(() -> {
                try {
                    log.info("启动数据源 {} 的WAL监听，复制槽: {}", dataSourceId, slotName);
                    startLogicalReplication(dataSource, slotName);
                } catch (Exception e) {
                    log.error("启动数据源 {} 的WAL监听失败", dataSourceId, e);
                    
                    // 触发告警
                    alertService.triggerAlert("failure", "high", 
                            String.format("PostgreSQL WAL监听启动失败，数据源: %d，错误: %s", 
                                    dataSourceId, e.getMessage()), null);
                    
                    walListeners.remove(dataSourceId);
                }
            });

            walListeners.put(dataSourceId, future);

        } catch (Exception e) {
            log.error("创建WAL监听失败", e);
            throw new RuntimeException("创建WAL监听失败: " + e.getMessage());
        }
    }

    /**
     * 停止指定数据源的WAL监听
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

        // 停止WAL监听
        Future<?> future = walListeners.remove(dataSourceId);
        if (future != null) {
            future.cancel(true);
            
            // 删除复制槽
            String slotName = replicationSlots.remove(dataSourceId);
            if (slotName != null) {
                try {
                    DataSourceConfig dataSource = dataSourceConfigMapper.selectById(dataSourceId);
                    if (dataSource != null) {
                        dropReplicationSlot(dataSource, slotName);
                    }
                } catch (Exception e) {
                    log.error("删除复制槽失败", e);
                }
            }
            
            // 清理批量队列
            batchQueues.remove(dataSourceId);
            
            log.info("停止数据源 {} 的WAL监听", dataSourceId);
        }
    }

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
                            String.format("PostgreSQL WAL批量同步失败，任务: %s，错误: %s", 
                                    task.getName(), e.getMessage()), null);
                }
            }
        }
    }

    /**
     * 创建逻辑复制槽
     */
    private String createReplicationSlot(DataSourceConfig dataSource) throws SQLException {
        String slotName = "syn_data_slot_" + System.currentTimeMillis();
        String url = buildJdbcUrl(dataSource);

        try (Connection conn = DriverManager.getConnection(url, dataSource.getUsername(), dataSource.getPassword())) {
            // 检查pgoutput插件是否可用
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM pg_available_extensions WHERE name = 'pgoutput'")
            ) {
                if (!rs.next()) {
                    throw new RuntimeException("pgoutput插件不可用，请先安装");
                }
            }

            // 创建逻辑复制槽
            String sql = String.format("SELECT pg_create_logical_replication_slot('%s', 'pgoutput')", slotName);
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
                log.info("创建逻辑复制槽: {}", slotName);
            }
        }

        return slotName;
    }

    /**
     * 删除逻辑复制槽
     */
    private void dropReplicationSlot(DataSourceConfig dataSource, String slotName) throws SQLException {
        String url = buildJdbcUrl(dataSource);

        try (Connection conn = DriverManager.getConnection(url, dataSource.getUsername(), dataSource.getPassword())) {
            String sql = String.format("SELECT pg_drop_replication_slot('%s')", slotName);
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
                log.info("删除逻辑复制槽: {}", slotName);
            }
        }
    }

    /**
     * 启动逻辑复制
     */
    private void startLogicalReplication(DataSourceConfig dataSource, String slotName) throws SQLException {
        String url = buildJdbcUrl(dataSource) + "?replication=database";
        
        try (Connection conn = DriverManager.getConnection(url, dataSource.getUsername(), dataSource.getPassword())) {
            // 创建复制流
            String sql = String.format("START_REPLICATION SLOT %s LOGICAL 0/0", slotName);
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
            }

            // 持续监听WAL变化
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // 读取WAL数据
                    processWalChanges(conn, dataSource.getId());
                    Thread.sleep(1000); // 每秒检查一次
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    /**
     * 处理WAL变更
     */
    private void processWalChanges(Connection conn, Long dataSourceId) {
        List<SyncTaskConfig> tasks = getDataSourceTasks(dataSourceId);
        if (tasks.isEmpty()) {
            return;
        }

        // 这里简化实现，实际应该解析pgoutput格式的WAL数据
        // pgoutput格式包含：BEGIN、INSERT、UPDATE、DELETE、COMMIT等消息
        // 需要解析这些消息并转换为相应的ES操作

        // 模拟处理变更
        log.debug("处理数据源 {} 的WAL变更", dataSourceId);
    }

    /**
     * 解析pgoutput消息
     * 
     * pgoutput消息格式：
     * - 'B' - Begin
     * - 'C' - Commit
     * - 'I' - Insert
     * - 'U' - Update
     * - 'D' - Delete
     * - 'T' - Truncate
     * - 'R' - Relation (表结构信息)
     * - 'Y' - Type
     */
    private void parsePgoutputMessage(ByteBuffer buffer, Long dataSourceId) {
        char messageType = (char) buffer.get();
        
        switch (messageType) {
            case 'B':
                // 事务开始
                log.debug("事务开始");
                break;
            case 'C':
                // 事务提交
                log.debug("事务提交");
                break;
            case 'I':
                // 插入操作
                handleInsertMessage(buffer, dataSourceId);
                break;
            case 'U':
                // 更新操作
                handleUpdateMessage(buffer, dataSourceId);
                break;
            case 'D':
                // 删除操作
                handleDeleteMessage(buffer, dataSourceId);
                break;
            case 'R':
                // 表结构信息
                handleRelationMessage(buffer);
                break;
            default:
                log.debug("未知消息类型: {}", messageType);
        }
    }

    /**
     * 处理插入消息
     */
    private void handleInsertMessage(ByteBuffer buffer, Long dataSourceId) {
        // 解析插入消息，获取表ID和元组数据
        int relationId = buffer.getInt();
        
        // 解析元组数据
        Map<String, Object> data = parseTupleData(buffer);
        log.debug("插入操作，表ID: {}, 数据: {}", relationId, data);
        
        // 添加到批量队列
        BlockingQueue<Map<String, Object>> queue = batchQueues.get(dataSourceId);
        if (queue != null) {
            queue.offer(data);
        }
    }

    /**
     * 处理更新消息
     */
    private void handleUpdateMessage(ByteBuffer buffer, Long dataSourceId) {
        // 解析更新消息
        int relationId = buffer.getInt();
        
        // 解析旧元组数据（可选）
        Map<String, Object> oldData = parseTupleData(buffer);
        
        // 解析新元组数据
        Map<String, Object> newData = parseTupleData(buffer);
        
        log.debug("更新操作，表ID: {}, 旧数据: {}, 新数据: {}", relationId, oldData, newData);
        
        // 添加到批量队列
        BlockingQueue<Map<String, Object>> queue = batchQueues.get(dataSourceId);
        if (queue != null) {
            queue.offer(newData);
        }
    }

    /**
     * 处理删除消息
     */
    private void handleDeleteMessage(ByteBuffer buffer, Long dataSourceId) {
        // 解析删除消息
        int relationId = buffer.getInt();
        
        // 解析元组数据
        Map<String, Object> data = parseTupleData(buffer);
        log.debug("删除操作，表ID: {}, 数据: {}", relationId, data);
        
        // 从ES删除
        List<SyncTaskConfig> tasks = getDataSourceTasks(dataSourceId);
        for (SyncTaskConfig task : tasks) {
            if (shouldSync(task, data)) {
                deleteFromElasticsearch(task, data);
            }
        }
    }

    /**
     * 处理表结构消息
     */
    private void handleRelationMessage(ByteBuffer buffer) {
        int relationId = buffer.getInt();
        String schemaName = readString(buffer);
        String tableName = readString(buffer);
        
        log.debug("表结构信息，ID: {}, 模式: {}, 表名: {}", relationId, schemaName, tableName);
    }

    /**
     * 解析元组数据
     */
    private Map<String, Object> parseTupleData(ByteBuffer buffer) {
        Map<String, Object> data = new HashMap<>();
        
        // 解析列数
        short columnCount = buffer.getShort();
        
        for (int i = 0; i < columnCount; i++) {
            char flag = (char) buffer.get();
            
            if (flag == 'n') {
                // NULL值
                data.put("column_" + i, null);
            } else if (flag == 'u') {
                // 未更改（用于UPDATE的旧值）
                data.put("column_" + i, "UNCHANGED");
            } else if (flag == 't') {
                // 文本数据
                int length = buffer.getInt();
                byte[] bytes = new byte[length];
                buffer.get(bytes);
                data.put("column_" + i, new String(bytes));
            } else if (flag == 'b') {
                // 二进制数据
                int length = buffer.getInt();
                byte[] bytes = new byte[length];
                buffer.get(bytes);
                data.put("column_" + i, bytes);
            }
        }
        
        return data;
    }

    /**
     * 从缓冲区读取字符串
     */
    private String readString(ByteBuffer buffer) {
        StringBuilder sb = new StringBuilder();
        byte b;
        while ((b = buffer.get()) != 0) {
            sb.append((char) b);
        }
        return sb.toString();
    }

    /**
     * 构建JDBC URL
     */
    private String buildJdbcUrl(DataSourceConfig dataSource) {
        return String.format("jdbc:postgresql://%s:%d/%s",
                dataSource.getHost(), dataSource.getPort(), dataSource.getDatabaseName());
    }

    /**
     * 判断是否应该同步该数据
     */
    private boolean shouldSync(SyncTaskConfig task, Map<String, Object> data) {
        // 这里可以添加更复杂的过滤逻辑
        return true;
    }

    /**
     * 从Elasticsearch删除数据
     */
    private void deleteFromElasticsearch(SyncTaskConfig task, Map<String, Object> data) {
        executorService.submit(() -> {
            try {
                // 提取文档ID
                String documentId = extractDocumentId(data);
                if (documentId != null) {
                    elasticsearchService.deleteDocument(task.getTargetIndex(), documentId);
                    log.info("从ES删除数据，任务: {}, 文档ID: {}", task.getName(), documentId);
                }
            } catch (Exception e) {
                log.error("从ES删除数据失败，任务: {}", task.getName(), e);
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

        // 如果该数据源的WAL监听未启动，则启动
        if (!walListeners.containsKey(dataSourceId)) {
            startListener(dataSourceId);
        }

        log.info("注册PostgreSQL增量同步任务: {}", task.getName());
    }

    /**
     * 取消注册同步任务
     */
    public void unregisterTask(SyncTaskConfig task) {
        Long dataSourceId = task.getSourceId();
        List<SyncTaskConfig> tasks = dataSourceTasks.get(dataSourceId);
        if (tasks != null) {
            tasks.remove(task);

            // 如果该数据源下没有任务了，停止WAL监听
            if (tasks.isEmpty()) {
                stopListener(dataSourceId);
            }
        }

        log.info("取消注册PostgreSQL增量同步任务: {}", task.getName());
    }

    /**
     * 获取监听状态
     */
    public Map<String, Object> getListenerStatus(Long dataSourceId) {
        Map<String, Object> status = new HashMap<>();
        
        boolean isRunning = walListeners.containsKey(dataSourceId);
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
