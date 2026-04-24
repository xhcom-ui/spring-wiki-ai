package com.syn.data.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Result;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.IndexOperation;
import co.elastic.clients.elasticsearch.core.bulk.DeleteOperation;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.DeleteIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Elasticsearch服务
 * 负责与Elasticsearch集群的交互，提供索引、查询、删除等功能
 */
@Slf4j
@Service
public class ElasticsearchService {

    @Value("${elasticsearch.hosts:localhost:9200}")
    private String elasticsearchHosts;

    @Value("${elasticsearch.username:}")
    private String elasticsearchUsername;

    @Value("${elasticsearch.password:}")
    private String elasticsearchPassword;

    private ElasticsearchClient client;
    private RestClient restClient;

    /**
     * 初始化Elasticsearch客户端
     */
    @PostConstruct
    public void init() {
        try {
            // 解析hosts配置，支持多个节点
            String[] hostsArray = elasticsearchHosts.split(",");
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

            // 如果配置了用户名和密码，添加认证
            if (elasticsearchUsername != null && !elasticsearchUsername.isEmpty()) {
                final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                credentialsProvider.setCredentials(AuthScope.ANY,
                        new UsernamePasswordCredentials(elasticsearchUsername, elasticsearchPassword));
                builder.setHttpClientConfigCallback(httpClientBuilder ->
                        httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider));
            }

            restClient = builder.build();
            ElasticsearchTransport transport = new RestClientTransport(
                    restClient, new JacksonJsonpMapper());
            client = new ElasticsearchClient(transport);

            log.info("Elasticsearch客户端初始化成功，连接到 {}", elasticsearchHosts);
        } catch (Exception e) {
            log.error("Elasticsearch客户端初始化失败", e);
            throw new RuntimeException("Elasticsearch客户端初始化失败: " + e.getMessage());
        }
    }

    /**
     * 销毁Elasticsearch客户端
     */
    @PreDestroy
    public void destroy() {
        try {
            if (restClient != null) {
                restClient.close();
                log.info("Elasticsearch客户端已关闭");
            }
        } catch (IOException e) {
            log.error("关闭Elasticsearch客户端失败", e);
        }
    }

    /**
     * 检查索引是否存在
     */
    public boolean indexExists(String indexName) {
        try {
            ExistsRequest request = ExistsRequest.of(e -> e.index(indexName));
            return client.indices().exists(request).value();
        } catch (IOException e) {
            log.error("检查索引存在性失败: {}", indexName, e);
            return false;
        }
    }

    /**
     * 创建索引
     */
    public boolean createIndex(String indexName, String mapping) {
        try {
            CreateIndexRequest request = CreateIndexRequest.of(c -> c
                    .index(indexName)
                    .withJson(new java.io.StringReader(mapping))
            );
            return client.indices().create(request).acknowledged();
        } catch (IOException e) {
            log.error("创建索引失败: {}", indexName, e);
            return false;
        }
    }

    /**
     * 删除索引
     */
    public boolean deleteIndex(String indexName) {
        try {
            DeleteIndexRequest request = DeleteIndexRequest.of(d -> d.index(indexName));
            return client.indices().delete(request).acknowledged();
        } catch (IOException e) {
            log.error("删除索引失败: {}", indexName, e);
            return false;
        }
    }

    /**
     * 索引单个文档
     */
    public boolean indexDocument(String indexName, String documentId, Map<String, Object> document) {
        try {
            IndexRequest<Map<String, Object>> request = IndexRequest.of(i -> i
                    .index(indexName)
                    .id(documentId)
                    .document(document)
            );
            Result result = client.index(request).result();
            return result == Result.Created || result == Result.Updated;
        } catch (IOException e) {
            log.error("索引文档失败: {}", indexName, e);
            return false;
        }
    }

    /**
     * 批量索引文档
     */
    public BulkResponse bulkIndex(String indexName, List<Map<String, Object>> documents) {
        try {
            List<BulkOperation> operations = new ArrayList<>();
            
            for (Map<String, Object> document : documents) {
                // 提取文档ID（假设第一列是ID）
                String documentId = extractDocumentId(document);
                
                IndexOperation<Map<String, Object>> indexOp = IndexOperation.of(i -> i
                        .index(indexName)
                        .id(documentId)
                        .document(document)
                );
                
                operations.add(BulkOperation.of(b -> b.index(indexOp)));
            }

            BulkRequest request = BulkRequest.of(b -> b.operations(operations));
            return client.bulk(request);
        } catch (IOException e) {
            log.error("批量索引文档失败: {}", indexName, e);
            throw new RuntimeException("批量索引失败: " + e.getMessage());
        }
    }

    /**
     * 删除文档
     */
    public boolean deleteDocument(String indexName, String documentId) {
        try {
            DeleteRequest request = DeleteRequest.of(d -> d
                    .index(indexName)
                    .id(documentId)
            );
            return client.delete(request).result() == Result.Deleted;
        } catch (IOException e) {
            log.error("删除文档失败: {}", indexName, e);
            return false;
        }
    }

    /**
     * 批量删除文档
     */
    public BulkResponse bulkDelete(String indexName, List<String> documentIds) {
        try {
            List<BulkOperation> operations = new ArrayList<>();
            
            for (String documentId : documentIds) {
                DeleteOperation deleteOp = DeleteOperation.of(d -> d
                        .index(indexName)
                        .id(documentId)
                );
                
                operations.add(BulkOperation.of(b -> b.delete(deleteOp)));
            }

            BulkRequest request = BulkRequest.of(b -> b.operations(operations));
            return client.bulk(request);
        } catch (IOException e) {
            log.error("批量删除文档失败: {}", indexName, e);
            throw new RuntimeException("批量删除失败: " + e.getMessage());
        }
    }

    /**
     * 搜索文档
     */
    public SearchResponse<Map> search(String indexName, String query, int from, int size) {
        try {
            SearchRequest request = SearchRequest.of(s -> s
                    .index(indexName)
                    .query(q -> q
                            .queryString(qs -> qs.query(query))
                    )
                    .from(from)
                    .size(size)
            );
            return client.search(request, Map.class);
        } catch (IOException e) {
            log.error("搜索文档失败: {}", indexName, e);
            throw new RuntimeException("搜索失败: " + e.getMessage());
        }
    }

    /**
     * 获取文档
     */
    public Map<String, Object> getDocument(String indexName, String documentId) {
        try {
            GetRequest request = GetRequest.of(g -> g
                    .index(indexName)
                    .id(documentId)
            );
            GetResponse<Map> response = client.get(request, Map.class);
            return response.found() ? response.source() : null;
        } catch (IOException e) {
            log.error("获取文档失败: {}", indexName, e);
            return null;
        }
    }

    /**
     * 统计文档数量
     */
    public long countDocuments(String indexName) {
        try {
            CountRequest request = CountRequest.of(c -> c.index(indexName));
            return client.count(request).count();
        } catch (IOException e) {
            log.error("统计文档数量失败: {}", indexName, e);
            return 0;
        }
    }

    /**
     * 提取文档ID
     */
    private String extractDocumentId(Map<String, Object> document) {
        // 优先使用 id 字段
        if (document.containsKey("id")) {
            return String.valueOf(document.get("id"));
        }
        
        // 其次使用 _id 字段
        if (document.containsKey("_id")) {
            return String.valueOf(document.get("_id"));
        }
        
        // 尝试使用第一个列作为ID
        for (String key : document.keySet()) {
            if (key.startsWith("column_0")) {
                return String.valueOf(document.get(key));
            }
        }
        
        // 如果没有ID字段，生成一个UUID
        return java.util.UUID.randomUUID().toString();
    }

    /**
     * 检查Elasticsearch连接状态
     */
    public boolean isConnected() {
        try {
            return client.ping().value();
        } catch (IOException e) {
            log.error("检查Elasticsearch连接失败", e);
            return false;
        }
    }

    /**
     * 获取集群健康状态
     */
    public Map<String, Object> getClusterHealth() {
        try {
            co.elastic.clients.elasticsearch.cluster.HealthResponse response = 
                    client.cluster().health();
            
            Map<String, Object> health = new java.util.HashMap<>();
            health.put("status", response.status().jsonValue());
            health.put("clusterName", response.clusterName());
            health.put("numberOfNodes", response.numberOfNodes());
            health.put("numberOfDataNodes", response.numberOfDataNodes());
            health.put("activeShards", response.activeShards());
            health.put("relocatingShards", response.relocatingShards());
            health.put("initializingShards", response.initializingShards());
            health.put("unassignedShards", response.unassignedShards());
            
            return health;
        } catch (IOException e) {
            log.error("获取集群健康状态失败", e);
            return null;
        }
    }

}
