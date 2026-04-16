package org.wiki.service;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.embedding.huggingface.HuggingFaceEmbeddingModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
public class EmbeddingService {

    private final EmbeddingModel huggingFaceEmbeddingModel;
    private final EmbeddingModel localEmbeddingModel;
    private final Map<String, Embedding> embeddingCache;
    private final int maxCacheSize;
    private final AtomicInteger cacheSize = new AtomicInteger(0);
    private final ExecutorService executorService = Executors.newFixedThreadPool(5);

    @Autowired
    public EmbeddingService(org.wiki.config.HuggingFaceConfig huggingFaceConfig, 
                          @Value("${huggingface.cache-size:1000}") int maxCacheSize) {
        this.maxCacheSize = maxCacheSize;
        
        // 初始化 Hugging Face Embedding 模型
        if (!huggingFaceConfig.getApiKey().isEmpty()) {
            try {
                this.huggingFaceEmbeddingModel = HuggingFaceEmbeddingModel.builder()
                        .apiKey(huggingFaceConfig.getApiKey())
                        .modelId(huggingFaceConfig.getEmbeddingModel())
                        .build();
                log.info("Hugging Face Embedding 模型初始化成功");
            } catch (Exception e) {
                log.error("Hugging Face Embedding 模型初始化失败: {}", e.getMessage());
                this.huggingFaceEmbeddingModel = null;
            }
        } else {
            // 如果没有 API 密钥，使用本地模型
            this.huggingFaceEmbeddingModel = null;
            log.info("未配置 Hugging Face API 密钥，使用本地模型");
        }

        // 初始化本地 Embedding 模型
        try {
            this.localEmbeddingModel = new AllMiniLmL6V2EmbeddingModel();
            log.info("本地 Embedding 模型初始化成功");
        } catch (Exception e) {
            log.error("本地 Embedding 模型初始化失败: {}", e.getMessage());
            throw new RuntimeException("本地 Embedding 模型初始化失败", e);
        }

        // 初始化嵌入缓存
        this.embeddingCache = new ConcurrentHashMap<>();
        log.info("嵌入服务初始化完成，缓存大小限制: {}", maxCacheSize);
    }

    // 获取文本嵌入
    public Embedding getEmbedding(String text) {
        if (text == null || text.isEmpty()) {
            log.warn("尝试获取空文本的嵌入");
            return createZeroEmbedding();
        }

        // 检查缓存
        if (embeddingCache.containsKey(text)) {
            log.debug("从缓存获取嵌入: {}", text.substring(0, Math.min(50, text.length())) + (text.length() > 50 ? "..." : ""));
            return embeddingCache.get(text);
        }

        // 优先使用 Hugging Face 模型
        Embedding embedding;
        if (huggingFaceEmbeddingModel != null) {
            try {
                log.debug("使用 Hugging Face 模型获取嵌入");
                embedding = huggingFaceEmbeddingModel.embed(text).content();
            } catch (Exception e) {
                log.error("Hugging Face 模型获取嵌入失败: {}", e.getMessage());
                // 如果 Hugging Face 模型失败，使用本地模型
                log.debug("使用本地模型获取嵌入");
                embedding = localEmbeddingModel.embed(text).content();
            }
        } else {
            // 使用本地模型
            log.debug("使用本地模型获取嵌入");
            embedding = localEmbeddingModel.embed(text).content();
        }

        // 缓存结果
        if (cacheSize.get() >= maxCacheSize) {
            log.debug("缓存达到上限，清理缓存");
            clearCache();
        }
        embeddingCache.put(text, embedding);
        cacheSize.incrementAndGet();
        log.debug("缓存嵌入，当前缓存大小: {}", cacheSize.get());

        return embedding;
    }

    // 批量获取文本嵌入（并行处理）
    public List<Embedding> getEmbeddings(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return Collections.emptyList();
        }

        List<CompletableFuture<Embedding>> futures = new ArrayList<>();
        for (String text : texts) {
            futures.add(CompletableFuture.supplyAsync(() -> getEmbedding(text), executorService));
        }

        // 等待所有任务完成
        CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        allOf.join();

        // 收集结果
        List<Embedding> embeddings = new ArrayList<>();
        for (CompletableFuture<Embedding> future : futures) {
            try {
                embeddings.add(future.get());
            } catch (Exception e) {
                log.error("获取嵌入失败: {}", e.getMessage());
                embeddings.add(createZeroEmbedding());
            }
        }

        return embeddings;
    }

    // 计算两个嵌入的余弦相似度
    public double calculateSimilarity(Embedding embedding1, Embedding embedding2) {
        if (embedding1 == null || embedding2 == null) {
            return 0.0;
        }

        List<Double> vector1 = embedding1.vector();
        List<Double> vector2 = embedding2.vector();

        if (vector1.size() != vector2.size()) {
            log.warn("嵌入向量维度不匹配: {} vs {}", vector1.size(), vector2.size());
            return 0.0;
        }

        // 计算点积
        double dotProduct = 0.0;
        for (int i = 0; i < vector1.size(); i++) {
            dotProduct += vector1.get(i) * vector2.get(i);
        }

        // 计算范数
        double norm1 = 0.0;
        for (Double value : vector1) {
            norm1 += value * value;
        }
        norm1 = Math.sqrt(norm1);

        double norm2 = 0.0;
        for (Double value : vector2) {
            norm2 += value * value;
        }
        norm2 = Math.sqrt(norm2);

        // 计算余弦相似度
        if (norm1 == 0 || norm2 == 0) {
            return 0.0;
        }
        return dotProduct / (norm1 * norm2);
    }

    // 计算两个文本的相似度
    public double calculateSimilarity(String text1, String text2) {
        if (text1 == null || text2 == null) {
            return 0.0;
        }
        Embedding embedding1 = getEmbedding(text1);
        Embedding embedding2 = getEmbedding(text2);
        return calculateSimilarity(embedding1, embedding2);
    }

    // 查找最相似的文本
    public String findMostSimilar(String query, List<String> texts) {
        if (query == null || texts == null || texts.isEmpty()) {
            return null;
        }

        double maxSimilarity = -1.0;
        String mostSimilarText = null;

        for (String text : texts) {
            double similarity = calculateSimilarity(query, text);
            if (similarity > maxSimilarity) {
                maxSimilarity = similarity;
                mostSimilarText = text;
            }
        }

        log.debug("最相似文本相似度: {}", maxSimilarity);
        return mostSimilarText;
    }

    // 查找相似度高于阈值的文本
    public List<Map.Entry<String, Double>> findSimilar(String query, List<String> texts, double threshold) {
        if (query == null || texts == null || texts.isEmpty()) {
            return Collections.emptyList();
        }

        return texts.stream()
                .map(text -> Map.entry(text, calculateSimilarity(query, text)))
                .filter(entry -> entry.getValue() >= threshold)
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .toList();
    }

    // 模型健康检查
    public boolean isHealthy() {
        try {
            // 测试本地模型
            Embedding testEmbedding = localEmbeddingModel.embed("test").content();
            if (testEmbedding == null || testEmbedding.vector().isEmpty()) {
                log.error("本地模型健康检查失败");
                return false;
            }

            // 测试 Hugging Face 模型（如果配置了）
            if (huggingFaceEmbeddingModel != null) {
                Embedding hfEmbedding = huggingFaceEmbeddingModel.embed("test").content();
                if (hfEmbedding == null || hfEmbedding.vector().isEmpty()) {
                    log.error("Hugging Face 模型健康检查失败");
                    // 即使 Hugging Face 模型失败，本地模型可用也视为健康
                }
            }

            log.info("嵌入服务健康检查通过");
            return true;
        } catch (Exception e) {
            log.error("嵌入服务健康检查失败: {}", e.getMessage());
            return false;
        }
    }

    // 清理缓存
    private void clearCache() {
        // 简单的清理策略：移除一半的缓存项
        int toRemove = Math.max(1, cacheSize.get() / 2);
        Iterator<Map.Entry<String, Embedding>> iterator = embeddingCache.entrySet().iterator();
        while (iterator.hasNext() && toRemove > 0) {
            iterator.next();
            iterator.remove();
            cacheSize.decrementAndGet();
            toRemove--;
        }
        log.debug("缓存清理完成，当前缓存大小: {}", cacheSize.get());
    }

    // 创建零向量嵌入
    private Embedding createZeroEmbedding() {
        // 使用本地模型的维度创建零向量
        Embedding testEmbedding = localEmbeddingModel.embed("test").content();
        List<Double> zeroVector = new ArrayList<>();
        for (int i = 0; i < testEmbedding.vector().size(); i++) {
            zeroVector.add(0.0);
        }
        return Embedding.from(zeroVector);
    }

    // 获取缓存统计信息
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("cacheSize", cacheSize.get());
        stats.put("maxCacheSize", maxCacheSize);
        stats.put("huggingFaceModelAvailable", huggingFaceEmbeddingModel != null);
        return stats;
    }

    // 关闭线程池
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("嵌入服务线程池已关闭");
    }
}
