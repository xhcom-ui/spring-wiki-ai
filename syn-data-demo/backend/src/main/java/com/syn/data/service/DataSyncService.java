package com.syn.data.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.syn.data.entity.DataSourceConfig;
import com.syn.data.entity.SyncTaskConfig;
import com.syn.data.mapper.DataSourceConfigMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.io.IOException;
import java.sql.*;
import java.util.*;

/**
 * 数据同步服务
 * 负责执行MySQL/PostgreSQL到Elasticsearch的数据同步
 */
@Slf4j
@Service
public class DataSyncService {

    @Resource
    private DataSourceConfigMapper dataSourceConfigMapper;

    @Value("${elasticsearch.hosts:localhost:9200}")
    private String esHosts;

    @Value("${elasticsearch.username:}")
    private String esUsername;

    @Value("${elasticsearch.password:}")
    private String esPassword;

    private ElasticsearchClient esClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 初始化Elasticsearch客户端
     */
    @PostConstruct
    public void init() {
        try {
            RestClient restClient = createRestClient();
            ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
            esClient = new ElasticsearchClient(transport);
            log.info("Elasticsearch客户端初始化成功");
        } catch (Exception e) {
            log.error("Elasticsearch客户端初始化失败", e);
        }
    }

    /**
     * 销毁Elasticsearch客户端
     */
    @PreDestroy
    public void destroy() {
        if (esClient != null) {
            try {
                esClient.close();
                log.info("Elasticsearch客户端已关闭");
            } catch (IOException e) {
                log.error("关闭Elasticsearch客户端失败", e);
            }
        }
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

        RestClient.Builder builder = RestClient.builder(httpHosts);

        // 配置认证
        if (esUsername != null && !esUsername.isEmpty()) {
            final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY,
                    new UsernamePasswordCredentials(esUsername, esPassword));
            builder.setHttpClientConfigCallback(httpClientBuilder ->
                    httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider));
        }

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

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            // 1. 获取数据源配置
            DataSourceConfig dataSource = dataSourceConfigMapper.selectById(task.getSourceId());
            if (dataSource == null) {
                throw new RuntimeException("数据源不存在");
            }

            // 2. 建立数据库连接
            conn = getConnection(dataSource);

            // 3. 执行SQL查询
            String sql = task.getSql_();
            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();

            // 4. 获取结果集元数据
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            // 5. 解析字段映射
            Map<String, String> fieldMapping = parseFieldMapping(task.getFieldMapping());

            // 6. 创建ES索引（如果不存在）
            createIndexIfNotExists(task.getTargetIndex(), fieldMapping);

            // 7. 批量写入ES
            int batchSize = task.getBatchSize() != null ? task.getBatchSize() : 1000;
            int totalCount = 0;
            List<Map<String, Object>> batch = new ArrayList<>();

            while (rs.next()) {
                Map<String, Object> document = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnLabel(i);
                    Object value = rs.getObject(i);
                    document.put(columnName, value);
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

        } catch (Exception e) {
            log.error("全量同步失败", e);
            result.setSuccess(false);
            result.setMessage("全量同步失败: " + e.getMessage());
        } finally {
            closeResources(rs, stmt, conn);
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

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            // 1. 获取数据源配置
            DataSourceConfig dataSource = dataSourceConfigMapper.selectById(task.getSourceId());
            if (dataSource == null) {
                throw new RuntimeException("数据源不存在");
            }

            // 2. 建立数据库连接
            conn = getConnection(dataSource);

            // 3. 构建增量SQL
            String sql = buildIncrementalSql(task.getSql_(), task.getIncrementalField(), lastSyncTime);
            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();

            // 4. 获取结果集元数据
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            // 5. 批量写入ES
            int batchSize = task.getBatchSize() != null ? task.getBatchSize() : 1000;
            int totalCount = 0;
            List<Map<String, Object>> batch = new ArrayList<>();

            while (rs.next()) {
                Map<String, Object> document = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnLabel(i);
                    Object value = rs.getObject(i);
                    document.put(columnName, value);
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

        } catch (Exception e) {
            log.error("增量同步失败", e);
            result.setSuccess(false);
            result.setMessage("增量同步失败: " + e.getMessage());
        } finally {
            closeResources(rs, stmt, conn);
            result.setEndTime(System.currentTimeMillis());
        }

        return result;
    }

    /**
     * 获取数据库连接
     */
    private Connection getConnection(DataSourceConfig dataSource) throws SQLException {
        String url = buildJdbcUrl(dataSource);
        return DriverManager.getConnection(url, dataSource.getUsername(), dataSource.getPassword());
    }

    /**
     * 构建JDBC URL
     */
    private String buildJdbcUrl(DataSourceConfig dataSource) {
        String type = dataSource.getType();
        String host = dataSource.getHost();
        int port = dataSource.getPort();
        String database = dataSource.getDatabaseName();

        if ("mysql".equalsIgnoreCase(type)) {
            return String.format("jdbc:mysql://%s:%d/%s?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai",
                    host, port, database);
        } else if ("postgresql".equalsIgnoreCase(type)) {
            return String.format("jdbc:postgresql://%s:%d/%s",
                    host, port, database);
        }
        throw new RuntimeException("不支持的数据库类型: " + type);
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
            CreateIndexRequest.Builder builder = new CreateIndexRequest.Builder()
                    .index(indexName);

            // 添加字段映射
            if (!fieldMapping.isEmpty()) {
                // 这里可以添加更复杂的mapping配置
            }

            esClient.indices().create(builder.build());
            log.info("创建ES索引: {}", indexName);
        }
    }

    /**
     * 批量索引文档
     */
    private void bulkIndexDocuments(String indexName, List<Map<String, Object>> documents) throws IOException {
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
            log.error("批量索引文档失败，存在错误");
            response.items().forEach(item -> {
                if (item.error() != null) {
                    log.error("索引失败: {}", item.error().reason());
                }
            });
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
        return UUID.randomUUID().toString();
    }

    /**
     * 构建增量SQL
     */
    private String buildIncrementalSql(String baseSql, String incrementalField, String lastSyncTime) {
        // 简单的增量SQL构建，实际项目中可能需要更复杂的逻辑
        if (baseSql.toLowerCase().contains("where")) {
            return baseSql + " AND " + incrementalField + " > '" + lastSyncTime + "'";
        } else {
            return baseSql + " WHERE " + incrementalField + " > '" + lastSyncTime + "'";
        }
    }

    /**
     * 关闭资源
     */
    private void closeResources(ResultSet rs, Statement stmt, Connection conn) {
        try {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();
        } catch (SQLException e) {
            log.error("关闭数据库资源失败", e);
        }
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
    }
}
