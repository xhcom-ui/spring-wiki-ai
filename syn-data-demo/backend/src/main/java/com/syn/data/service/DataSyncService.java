package com.syn.data.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.PutMappingRequest;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.syn.data.entity.DataSourceConfig;
import com.syn.data.entity.SyncTaskConfig;
import com.syn.data.mapper.DataSourceConfigMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringReader;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * 数据同步服务
 * 负责执行MySQL/PostgreSQL到Elasticsearch的数据同步
 */
@Slf4j
@Service
public class DataSyncService {

    @Resource
    private DataSourceConfigMapper dataSourceConfigMapper;

    @Resource
    private DatabaseConnectionService databaseConnectionService;

    @Value("${elasticsearch.hosts:localhost:9200}")
    private String esHosts;

    @Value("${elasticsearch.username:}")
    private String esUsername;

    @Value("${elasticsearch.password:}")
    private String esPassword;

    @Value("${elasticsearch.connection-timeout:30000}")
    private int esConnectionTimeout;

    @Value("${elasticsearch.socket-timeout:60000}")
    private int esSocketTimeout;

    @Value("${database.pool.size:10}")
    private int databasePoolSize;

    private ElasticsearchClient esClient;
    private RestClient esRestClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ExecutorService executorService = Executors.newFixedThreadPool(5);
    private final Map<String, HikariDataSource> dataSourcePool = new ConcurrentHashMap<>();

    /**
     * 初始化Elasticsearch客户端
     */
    @PostConstruct
    public void init() {
        try {
            esRestClient = createRestClient();
            ElasticsearchTransport transport = new RestClientTransport(esRestClient, new JacksonJsonpMapper());
            esClient = new ElasticsearchClient(transport);
            log.info("Elasticsearch客户端初始化成功");
        } catch (Exception e) {
            log.error("Elasticsearch客户端初始化失败", e);
        }
    }

    /**
     * 销毁资源
     */
    @PreDestroy
    public void destroy() {
        // 关闭Elasticsearch客户端
        if (esRestClient != null) {
            try {
                esRestClient.close();
                log.info("Elasticsearch客户端已关闭");
            } catch (IOException e) {
                log.error("关闭Elasticsearch客户端失败", e);
            }
        }
        
        // 关闭线程池
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        // 关闭数据源连接池
        dataSourcePool.values().forEach(dataSource -> {
            try {
                dataSource.close();
            } catch (Exception e) {
                log.error("关闭数据源失败", e);
            }
        });
    }

    /**
     * 创建RestClient
     */
    private RestClient createRestClient() {
        // 解析hosts配置，支持多个节点
        String[] hostsArray = esHosts.split(",");
        HttpHost[] httpHosts = new HttpHost[hostsArray.length];
        
        for (int i = 0; i < hostsArray.length; i++) {
            String hostStr = hostsArray[i].trim();
            if (hostStr.contains(":")) {
                String[] parts = hostStr.split(":");
                httpHosts[i] = new HttpHost(parts[0], Integer.parseInt(parts[1]));
            } else {
                httpHosts[i] = new HttpHost(hostStr, 9200);
            }
        }

        RestClientBuilder builder = RestClient.builder(httpHosts);

        // 配置连接超时
        builder.setRequestConfigCallback(requestConfigBuilder -> {
            return requestConfigBuilder
                    .setConnectTimeout(esConnectionTimeout)
                    .setSocketTimeout(esSocketTimeout);
        });

        // 配置连接池
        builder.setHttpClientConfigCallback((HttpAsyncClientBuilder httpClientBuilder) -> {
            httpClientBuilder.setMaxConnTotal(100);
            httpClientBuilder.setMaxConnPerRoute(10);
            
            // 配置认证
            if (esUsername != null && !esUsername.isEmpty()) {
                final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                credentialsProvider.setCredentials(AuthScope.ANY,
                        new UsernamePasswordCredentials(esUsername, esPassword));
                httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
            }
            
            return httpClientBuilder;
        });

        return builder.build();
    }

    /**
     * 执行全量同步
     *
     * @param task 同步任务配置
     * @return 同步结果
     */
    public SyncResult executeFullSync(SyncTaskConfig task) {
        log.info("开始执行全量同步，任务ID: {}, 任务名称: {}", task.getId(), task.getName());
        SyncResult result = new SyncResult();
        result.setStartTime(System.currentTimeMillis());

        try {
            DataSourceConfig dataSource = requireDataSource(task.getSourceId());
            Map<String, String> fieldMapping = parseFieldMapping(task.getFieldMapping());
            createIndexIfNotExists(task.getTargetIndex(), fieldMapping);

            try (Connection conn = getConnection(dataSource);
                 PreparedStatement stmt = prepareQueryStatement(conn, task.getSql_(), task.getBatchSize(), task.getQueryTimeout(), null);
                 ResultSet rs = stmt.executeQuery()) {

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            int batchSize = task.getBatchSize() != null ? task.getBatchSize() : 1000;
            int totalCount = 0;
            List<Map<String, Object>> batch = new ArrayList<>(batchSize);

            while (rs.next()) {
                Map<String, Object> document = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnLabel(i);
                    Object value = rs.getObject(i);
                    // 处理NULL值
                    if (value != null) {
                        document.put(columnName, value);
                    }
                }
                batch.add(document);

                if (batch.size() >= batchSize) {
                    bulkIndexDocuments(task.getTargetIndex(), batch);
                    totalCount += batch.size();
                    batch.clear();
                    log.info("已同步 {} 条数据", totalCount);
                }
            }

            // 处理剩余数据
            if (!batch.isEmpty()) {
                bulkIndexDocuments(task.getTargetIndex(), batch);
                totalCount += batch.size();
            }

            result.setSuccess(true);
            result.setTotalCount(totalCount);
            result.setMessage("全量同步成功，共处理 " + totalCount + " 条数据");
            }

        } catch (Exception e) {
            log.error("全量同步失败", e);
            result.setSuccess(false);
            result.setMessage("全量同步失败: " + e.getMessage());
        } finally {
            result.setEndTime(System.currentTimeMillis());
        }

        return result;
    }

    /**
     * 执行增量同步
     *
     * @param task 同步任务配置
     * @param lastSyncTime 上次同步时间
     * @return 同步结果
     */
    public SyncResult executeIncrementalSync(SyncTaskConfig task, String lastSyncTime) {
        log.info("开始执行增量同步，任务ID: {}, 任务名称: {}, 上次同步时间: {}",
                task.getId(), task.getName(), lastSyncTime);
        SyncResult result = new SyncResult();
        result.setStartTime(System.currentTimeMillis());

        try {
            DataSourceConfig dataSource = requireDataSource(task.getSourceId());
            String sql = buildIncrementalSql(task.getSql_(), task.getIncrementalField());

            try (Connection conn = getConnection(dataSource);
                 PreparedStatement stmt = prepareQueryStatement(conn, sql, task.getBatchSize(), task.getQueryTimeout(), lastSyncTime);
                 ResultSet rs = stmt.executeQuery()) {

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            int batchSize = task.getBatchSize() != null ? task.getBatchSize() : 1000;
            int totalCount = 0;
            List<Map<String, Object>> batch = new ArrayList<>(batchSize);

            while (rs.next()) {
                Map<String, Object> document = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnLabel(i);
                    Object value = rs.getObject(i);
                    // 处理NULL值
                    if (value != null) {
                        document.put(columnName, value);
                    }
                }
                batch.add(document);

                if (batch.size() >= batchSize) {
                    bulkIndexDocuments(task.getTargetIndex(), batch);
                    totalCount += batch.size();
                    batch.clear();
                }
            }

            // 处理剩余数据
            if (!batch.isEmpty()) {
                bulkIndexDocuments(task.getTargetIndex(), batch);
                totalCount += batch.size();
            }

            result.setSuccess(true);
            result.setTotalCount(totalCount);
            result.setMessage("增量同步成功，共处理 " + totalCount + " 条数据");
            }

        } catch (Exception e) {
            log.error("增量同步失败", e);
            result.setSuccess(false);
            result.setMessage("增量同步失败: " + e.getMessage());
        } finally {
            result.setEndTime(System.currentTimeMillis());
        }

        return result;
    }

    /**
     * 执行并行同步（多线程）
     *
     * @param tasks 同步任务配置列表
     * @return 同步结果列表
     */
    public List<SyncResult> executeParallelSync(List<SyncTaskConfig> tasks) {
        List<SyncResult> results = new ArrayList<>(tasks.size());
        List<CompletableFuture<SyncResult>> futures = new ArrayList<>(tasks.size());

        for (SyncTaskConfig task : tasks) {
            CompletableFuture<SyncResult> future = CompletableFuture.supplyAsync(() -> {
                return executeFullSync(task);
            }, executorService);
            futures.add(future);
        }

        // 等待所有任务完成
        CompletableFuture<Void> allOf = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0])
        );

        try {
            allOf.get();
            // 收集结果
            for (CompletableFuture<SyncResult> future : futures) {
                results.add(future.get());
            }
        } catch (Exception e) {
            log.error("并行同步失败", e);
        }

        return results;
    }

    /**
     * 获取数据库连接
     */
    private Connection getConnection(DataSourceConfig dataSource) throws SQLException {
        String key = dataSource.getType() + "_" + dataSource.getHost() + "_" + dataSource.getPort() + "_" + dataSource.getDatabaseName();
        HikariDataSource ds = dataSourcePool.computeIfAbsent(key, k -> createDataSource(dataSource));
        return ds.getConnection();
    }

    /**
     * 创建数据源
     */
    private HikariDataSource createDataSource(DataSourceConfig dataSource) {
        try {
            String url = databaseConnectionService.buildJdbcUrl(dataSource);
            String driverClass = databaseConnectionService.resolveDriverClassName(dataSource.getType());
            int maxPoolSize = Optional.ofNullable(dataSource.getMaxConnections())
                    .filter(value -> value > 0)
                    .orElse(databasePoolSize);
            int minIdle = Optional.ofNullable(dataSource.getMinConnections())
                    .filter(value -> value >= 0)
                    .orElse(Math.min(5, maxPoolSize));
            int timeoutMs = normalizeTimeoutMs(dataSource.getConnectionTimeout(), 30000);

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(url);
            config.setUsername(dataSource.getUsername());
            config.setPassword(dataSource.getPassword());
            config.setDriverClassName(driverClass);
            config.setMaximumPoolSize(maxPoolSize);
            config.setMinimumIdle(Math.min(minIdle, maxPoolSize));
            config.setIdleTimeout(30000);
            config.setMaxLifetime(1800000);
            config.setConnectionTimeout(timeoutMs);

            return new HikariDataSource(config);
        } catch (Exception e) {
            log.error("创建数据源失败", e);
            throw new RuntimeException("创建数据源失败", e);
        }
    }

    /**
     * 解析字段映射配置
     */
    private Map<String, String> parseFieldMapping(String fieldMappingJson) throws IOException {
        if (fieldMappingJson == null || fieldMappingJson.isEmpty()) {
            return new HashMap<>();
        }
        JsonNode rootNode = objectMapper.readTree(fieldMappingJson);
        Map<String, String> mapping = new HashMap<>();
        rootNode.fields().forEachRemaining(entry -> {
            mapping.put(entry.getKey(), entry.getValue().asText());
        });
        return mapping;
    }

    /**
     * 创建ES索引（如果不存在）
     */
    private void createIndexIfNotExists(String indexName, Map<String, String> fieldMapping) throws IOException {
        boolean exists = esClient.indices().exists(e -> e.index(indexName)).value();
        if (!exists) {
            // 创建索引
            CreateIndexRequest.Builder builder = new CreateIndexRequest.Builder()
                    .index(indexName)
                    .settings(s -> s
                            .numberOfShards("3")
                            .numberOfReplicas("1")
                    );

            esClient.indices().create(builder.build());
            log.info("创建ES索引: {}", indexName);

            // 添加字段映射
            if (!fieldMapping.isEmpty()) {
                Map<String, Object> properties = new HashMap<>();
                for (Map.Entry<String, String> entry : fieldMapping.entrySet()) {
                    Map<String, Object> fieldProps = new HashMap<>();
                    fieldProps.put("type", entry.getValue());
                    properties.put(entry.getKey(), fieldProps);
                }

                Map<String, Object> mapping = new HashMap<>();
                mapping.put("properties", properties);
                String mappingJson = objectMapper.writeValueAsString(mapping);

                PutMappingRequest putMappingRequest = PutMappingRequest.of(p -> p
                        .index(indexName)
                        .withJson(new StringReader(mappingJson))
                );

                esClient.indices().putMapping(putMappingRequest);
                log.info("添加ES字段映射: {}", indexName);
            }
        }
    }

    /**
     * 批量索引文档
     */
    private void bulkIndexDocuments(String indexName, List<Map<String, Object>> documents) throws IOException {
        if (documents.isEmpty()) {
            return;
        }

        int retries = 3;
        int delay = 1000;
        
        while (retries > 0) {
            try {
                BulkRequest.Builder bulkBuilder = new BulkRequest.Builder();

                for (Map<String, Object> doc : documents) {
                    String docId = generateDocId(doc);
                    bulkBuilder.operations(op -> op
                            .index(idx -> idx
                                    .index(indexName)
                                    .id(docId)
                                    .document(doc)
                            )
                    );
                }

                BulkResponse response = esClient.bulk(bulkBuilder.build());
                if (response.errors()) {
                    int errorCount = 0;
                    StringBuilder errorMessages = new StringBuilder();
                    
                    for (BulkResponseItem item : response.items()) {
                        if (item.error() != null) {
                            errorCount++;
                            errorMessages.append(item.error().reason()).append("\n");
                        }
                    }
                    
                    log.error("批量索引文档失败，{}个文档出错: {}", errorCount, errorMessages.toString());
                    
                    // 如果是临时错误，重试
                    if (retries > 1) {
                        log.warn("重试批量索引，剩余次数: {}", retries - 1);
                        try {
                            Thread.sleep(delay);
                        } catch (InterruptedException interruptedException) {
                            Thread.currentThread().interrupt();
                            throw new IOException("批量索引重试被中断", interruptedException);
                        }
                        delay *= 2; // 指数退避
                        retries--;
                        continue;
                    }
                }
                
                // 成功或达到最大重试次数
                break;
                
            } catch (Exception e) {
                log.error("批量索引文档异常", e);
                if (retries > 1) {
                    log.warn("重试批量索引，剩余次数: {}", retries - 1);
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    delay *= 2; // 指数退避
                    retries--;
                } else {
                    if (e instanceof IOException ioException) {
                        throw ioException;
                    }
                    throw new IOException("批量索引失败", e);
                }
            }
        }
    }

    /**
     * 生成文档ID
     */
    private String generateDocId(Map<String, Object> doc) {
        // 优先使用id字段，否则使用UUID
        if (doc.containsKey("id")) {
            return String.valueOf(doc.get("id"));
        }
        // 尝试使用其他唯一字段
        for (String field : Arrays.asList("uuid", "guid", "code", "key", "identifier")) {
            if (doc.containsKey(field)) {
                return String.valueOf(doc.get(field));
            }
        }
        // 最后使用UUID
        return UUID.randomUUID().toString();
    }

    /**
     * 构建增量SQL
     */
    private String buildIncrementalSql(String baseSql, String incrementalField) {
        String normalizedSql = baseSql.trim();
        if (normalizedSql.toLowerCase().endsWith(";")) {
            normalizedSql = normalizedSql.substring(0, normalizedSql.length() - 1);
        }

        if (normalizedSql.toLowerCase().contains("where")) {
            return normalizedSql + " AND " + incrementalField + " > ?";
        } else {
            return normalizedSql + " WHERE " + incrementalField + " > ?";
        }
    }

    private DataSourceConfig requireDataSource(Long sourceId) {
        DataSourceConfig dataSource = dataSourceConfigMapper.selectById(sourceId);
        if (dataSource == null) {
            throw new RuntimeException("数据源不存在");
        }
        return dataSource;
    }

    private PreparedStatement prepareQueryStatement(
            Connection conn,
            String sql,
            Integer batchSize,
            Integer queryTimeout,
            String incrementalValue
    ) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        stmt.setFetchSize(batchSize != null && batchSize > 0 ? batchSize : 1000);
        if (queryTimeout != null && queryTimeout > 0) {
            stmt.setQueryTimeout(queryTimeout);
        }
        if (incrementalValue != null) {
            stmt.setString(1, incrementalValue);
        }
        return stmt;
    }

    private int normalizeTimeoutMs(Integer value, int defaultValue) {
        if (value == null || value <= 0) {
            return defaultValue;
        }
        return value < 1000 ? value * 1000 : value;
    }

    /**
     * 同步结果类
     */
    public static class SyncResult {
        private boolean success;
        private int totalCount;
        private String message;
        private long startTime;
        private long endTime;

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public int getTotalCount() {
            return totalCount;
        }

        public void setTotalCount(int totalCount) {
            this.totalCount = totalCount;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public long getStartTime() {
            return startTime;
        }

        public void setStartTime(long startTime) {
            this.startTime = startTime;
        }

        public long getEndTime() {
            return endTime;
        }

        public void setEndTime(long endTime) {
            this.endTime = endTime;
        }

        public long getDuration() {
            return endTime - startTime;
        }

        @Override
        public String toString() {
            return "SyncResult{" +
                    "success=" + success +
                    ", totalCount=" + totalCount +
                    ", message='" + message + '\'' +
                    ", duration=" + getDuration() + "ms" +
                    '}';
        }
    }
}
