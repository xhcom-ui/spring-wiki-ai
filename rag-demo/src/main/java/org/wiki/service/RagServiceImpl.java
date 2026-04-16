package org.wiki.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RagServiceImpl implements RagService {

    private final VectorStore vectorStore;
    private final ChatClient chatClient;

    // 构造注入
    public RagServiceImpl(VectorStore vectorStore, ChatClient.Builder chatClientBuilder) {
        this.vectorStore = vectorStore;
        this.chatClient = chatClientBuilder.build();
    }

    /**
     * 文档上传 → 切分 → 向量化 → 存入向量库
     */
    @Override
    public String uploadDocument(String filePath) {
        // 读取文档（支持 TXT/PDF/Word）
        TikaDocumentReader reader = new TikaDocumentReader(new FileSystemResource(filePath));
        List<Document> documents = reader.get();

        // 文本切分（默认按 token 切分）
        TokenTextSplitter splitter = new TokenTextSplitter();
        List<Document> splitDocs = splitter.apply(documents);

        // 添加PageIndex信息
        for (int i = 0; i < splitDocs.size(); i++) {
            Document doc = splitDocs.get(i);
            doc.getMetadata().put("pageIndex", String.valueOf(i + 1));
            doc.getMetadata().put("chunkId", String.valueOf(i + 1));
        }

        // 存入向量数据库
        vectorStore.add(splitDocs);

        return "文档向量化完成，共 " + splitDocs.size() + " 条片段";
    }

    /**
     * 用户意图识别和Query改写
     */
    private String rewriteQuery(String question) {
        String systemPrompt = """
            你是一个专业的查询改写助手，负责将用户的自然语言问题改写成更适合向量检索的查询语句。
            你的任务是：
            1. 分析用户的意图和核心需求
            2. 识别关键实体和概念
            3. 生成一个更精确、更具信息量的查询语句
            4. 保持查询语句的自然语言风格
            5. 不要添加任何解释或额外信息，只返回改写后的查询语句
            """;

        return chatClient.prompt()
                .system(systemPrompt)
                .user(question)
                .call()
                .content();
    }

    /**
     * 向量检索
     */
    private List<Document> vectorSearch(String query) {
        SearchRequest searchRequest = SearchRequest.builder()
                .query(query)
                .topK(10)
                .build();
        return vectorStore.similaritySearch(searchRequest);
    }

    /**
     * 关键词检索（基于简单的文本匹配）
     */
    private List<Document> keywordSearch(String query, List<Document> allDocs) {
        List<Document> results = new ArrayList<>();
        String[] keywords = query.toLowerCase().split("\\s+");
        
        for (Document doc : allDocs) {
            String text = doc.getText().toLowerCase();
            int matchCount = 0;
            for (String keyword : keywords) {
                if (text.contains(keyword)) {
                    matchCount++;
                }
            }
            if (matchCount > 0) {
                results.add(doc);
            }
        }
        return results.subList(0, Math.min(5, results.size()));
    }

    /**
     * 元数据检索
     */
    private List<Document> metadataSearch(String query, List<Document> allDocs) {
        List<Document> results = new ArrayList<>();
        for (Document doc : allDocs) {
            Map<String, Object> metadata = doc.getMetadata();
            for (Map.Entry<String, Object> entry : metadata.entrySet()) {
                if (entry.getValue().toString().toLowerCase().contains(query.toLowerCase())) {
                    results.add(doc);
                    break;
                }
            }
        }
        return results.subList(0, Math.min(5, results.size()));
    }

    /**
     * 融合层：对多路召回结果进行加权融合
     */
    private List<Document> fuseResults(List<Document> vectorResults, List<Document> keywordResults, List<Document> metadataResults) {
        // 权重配置（可手动调整）
        double vectorWeight = 0.6;
        double keywordWeight = 0.3;
        double metadataWeight = 0.1;
        
        // 存储文档及其得分
        Map<Document, Double> docScores = new HashMap<>();
        
        // 处理向量检索结果
        for (int i = 0; i < vectorResults.size(); i++) {
            Document doc = vectorResults.get(i);
            double score = vectorWeight * (1.0 / (i + 1));
            docScores.put(doc, docScores.getOrDefault(doc, 0.0) + score);
        }
        
        // 处理关键词检索结果
        for (int i = 0; i < keywordResults.size(); i++) {
            Document doc = keywordResults.get(i);
            double score = keywordWeight * (1.0 / (i + 1));
            docScores.put(doc, docScores.getOrDefault(doc, 0.0) + score);
        }
        
        // 处理元数据检索结果
        for (int i = 0; i < metadataResults.size(); i++) {
            Document doc = metadataResults.get(i);
            double score = metadataWeight * (1.0 / (i + 1));
            docScores.put(doc, docScores.getOrDefault(doc, 0.0) + score);
        }
        
        // 按得分排序
        List<Map.Entry<Document, Double>> sortedEntries = new ArrayList<>(docScores.entrySet());
        sortedEntries.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));
        
        // 提取排序后的文档
        List<Document> fusedResults = new ArrayList<>();
        for (Map.Entry<Document, Double> entry : sortedEntries) {
            fusedResults.add(entry.getKey());
        }
        
        // 返回前5-10条结果
        return fusedResults.subList(0, Math.min(10, fusedResults.size()));
    }

    /**
     * 精排层：对融合后的结果进行精排
     */
    private List<Document> rerankResults(String question, List<Document> fusedResults) {
        // 构建精排提示词
        StringBuilder documentsStr = new StringBuilder();
        for (int i = 0; i < fusedResults.size(); i++) {
            Document doc = fusedResults.get(i);
            documentsStr.append("文档 " + (i + 1) + ": " + doc.getText() + "\n");
        }
        
        String systemPrompt = """
            你是一个专业的文档排序助手，负责根据用户问题对文档进行相关性排序。
            你的任务是：
            1. 分析用户问题和每个文档的内容
            2. 评估每个文档与问题的相关性
            3. 按相关性从高到低排序文档
            4. 只返回排序后的文档编号，用逗号分隔，例如：1,3,2,4,5
            5. 不要添加任何解释或额外信息
            """;
        
        String userPrompt = "用户问题：" + question + "\n\n文档列表：\n" + documentsStr.toString();
        
        // 调用LLM进行精排
        String rankingResult = chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .content();
        
        // 解析排序结果
        List<Document> rerankedResults = new ArrayList<>();
        try {
            String[] ranks = rankingResult.split(",");
            for (String rank : ranks) {
                int index = Integer.parseInt(rank.trim()) - 1;
                if (index >= 0 && index < fusedResults.size()) {
                    rerankedResults.add(fusedResults.get(index));
                }
            }
        } catch (Exception e) {
            // 如果解析失败，返回原始融合结果
            return fusedResults;
        }
        
        // 如果解析结果为空，返回原始融合结果
        return rerankedResults.isEmpty() ? fusedResults : rerankedResults;
    }

    /**
     * 观测层：监控检索效果
     */
    private void observeSearch(String question, String rewrittenQuery, List<Document> vectorResults, 
                              List<Document> keywordResults, List<Document> metadataResults, 
                              List<Document> fusedResults, List<Document> rerankedResults, 
                              long startTime, long endTime) {
        // 计算检索时间
        long searchTime = endTime - startTime;
        
        // 记录观测数据
        System.out.println("=== 检索观测信息 ===");
        System.out.println("原始查询: " + question);
        System.out.println("改写后查询: " + rewrittenQuery);
        System.out.println("检索时间: " + searchTime + "ms");
        System.out.println("向量检索结果数: " + vectorResults.size());
        System.out.println("关键词检索结果数: " + keywordResults.size());
        System.out.println("元数据检索结果数: " + metadataResults.size());
        System.out.println("融合后结果数: " + fusedResults.size());
        System.out.println("精排后结果数: " + rerankedResults.size());
        System.out.println("===================");
        
        // 这里可以添加更多观测指标，例如：
        // 1. 召回率
        // 2. 精确率
        // 3. 相关性评分
        // 4. 用户满意度
        // 5. 存储到日志或监控系统
    }

    @Override
    public String askQuestion(String question) {
        // 记录开始时间
        long startTime = System.currentTimeMillis();
        
        // 1. 用户意图识别和Query改写
        String rewrittenQuery = rewriteQuery(question);

        // 2. 多路召回
        // 2.1 向量检索
        List<Document> vectorResults = vectorSearch(rewrittenQuery);
        
        // 2.2 关键词检索
        List<Document> keywordResults = keywordSearch(rewrittenQuery, vectorResults);
        
        // 2.3 元数据检索
        List<Document> metadataResults = metadataSearch(rewrittenQuery, vectorResults);

        // 3. 融合层：对多路召回结果进行加权融合
        List<Document> fusedResults = fuseResults(vectorResults, keywordResults, metadataResults);

        // 4. 精排层：对融合后的结果进行精排
        List<Document> rerankedResults = rerankResults(question, fusedResults);

        // 5. 拼接上下文
        StringBuilder context = new StringBuilder();
        for (Document doc : rerankedResults) {
            context.append(doc.getText()).append("\n");
        }

        // 6. M6 正确写法：把上下文直接拼进 system 提示词里
        String systemPrompt = """
            你是一个专业的问答助手，请根据提供的上下文回答问题。
            只使用上下文信息，不要编造答案。
            如果无法回答，请回复：根据提供的资料无法回答该问题。

            上下文：
            {context}
            """.replace("{context}", context.toString()); // 直接替换！

        // 7. M6 标准调用方式（无 variable，无报错）
        String answer = chatClient.prompt()
                .system(systemPrompt)
                .user(question)
                .call()
                .content();
        
        // 记录结束时间
        long endTime = System.currentTimeMillis();
        
        // 8. 观测层：监控检索效果
        observeSearch(question, rewrittenQuery, vectorResults, keywordResults, metadataResults, 
                     fusedResults, rerankedResults, startTime, endTime);
        
        return answer;
    }
}