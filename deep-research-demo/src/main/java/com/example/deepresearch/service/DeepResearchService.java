package com.example.deepresearch.service;

import com.example.deepresearch.entity.ResearchSource;
import com.example.deepresearch.entity.ResearchResult;
import com.example.deepresearch.entity.ResearchTopic;
import com.example.deepresearch.repository.ResearchSourceRepository;
import com.example.deepresearch.repository.ResearchResultRepository;
import com.example.deepresearch.repository.ResearchTopicRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class DeepResearchService {

    @Autowired
    private ResearchTopicRepository topicRepository;

    @Autowired
    private ResearchSourceRepository sourceRepository;

    @Autowired
    private ResearchResultRepository resultRepository;

    @Autowired
    private WebSearchService webSearchService;

    @Autowired
    private LLMService llmService;

    @Autowired
    private EmbeddingService embeddingService;

    @Value("${deep-research.max-iterations}")
    private int maxIterations;

    @Value("${deep-research.max-sources}")
    private int maxSources;

    @Value("${deep-research.chunk-size}")
    private int chunkSize;

    @Value("${deep-research.chunk-overlap}")
    private int chunkOverlap;

    public ResearchResult conductResearch(String topic, String description) {
        try {
            // 1. 创建研究主题
            ResearchTopic researchTopic = new ResearchTopic();
            researchTopic.setTopic(topic);
            researchTopic.setDescription(description);
            researchTopic.setCreatedAt(LocalDateTime.now());
            researchTopic.setUpdatedAt(LocalDateTime.now());
            researchTopic = topicRepository.save(researchTopic);

            log.info("开始深度研究: {}", topic);

            // 2. 多轮迭代研究
            for (int iteration = 1; iteration <= maxIterations; iteration++) {
                log.info("第 {} 轮研究", iteration);

                // 2.1 搜索相关信息
                List<WebSearchService.SearchResult> searchResults = webSearchService.search(topic, maxSources);

                // 2.2 处理搜索结果
                for (WebSearchService.SearchResult result : searchResults) {
                    // 检查是否已存在相同的来源
                    boolean exists = sourceRepository.findByTopic(researchTopic).stream()
                            .anyMatch(source -> source.getUrl().equals(result.getUrl()));
                    if (!exists) {
                        ResearchSource source = new ResearchSource();
                        source.setTitle(result.getTitle());
                        source.setUrl(result.getUrl());
                        source.setContent(result.getContent());
                        source.setType("web");
                        source.setCreatedAt(LocalDateTime.now());
                        source.setTopic(researchTopic);
                        sourceRepository.save(source);
                    }
                }

                // 2.3 生成新的搜索查询（基于当前研究结果）
                if (iteration < maxIterations) {
                    String newQuery = generateNewQuery(researchTopic);
                    topic = newQuery; // 更新搜索主题
                }
            }

            // 3. 处理研究资料
            List<ResearchSource> sources = sourceRepository.findByTopic(researchTopic);
            List<String> chunks = chunkContent(sources);

            // 4. 生成研究摘要和结论
            String summary = generateSummary(chunks);
            String conclusion = generateConclusion(chunks, topic);

            // 5. 构建参考文献
            StringBuilder referencesBuilder = new StringBuilder();
            for (ResearchSource source : sources) {
                referencesBuilder.append("- [").append(source.getTitle()).append("](").append(source.getUrl()).append(")\n");
            }

            // 6. 保存研究结果
            ResearchResult researchResult = new ResearchResult();
            researchResult.setSummary(summary);
            researchResult.setConclusion(conclusion);
            researchResult.setReferences(referencesBuilder.toString());
            researchResult.setCreatedAt(LocalDateTime.now());
            researchResult.setTopic(researchTopic);
            researchResult = resultRepository.save(researchResult);

            log.info("深度研究完成: {}", topic);

            return researchResult;
        } catch (Exception e) {
            log.error("深度研究失败: {}", e.getMessage(), e);
            throw new RuntimeException("深度研究失败: " + e.getMessage());
        }
    }

    private String generateNewQuery(ResearchTopic topic) {
        // 基于当前研究结果生成新的搜索查询
        List<ResearchSource> sources = sourceRepository.findByTopic(topic);
        StringBuilder contentBuilder = new StringBuilder();
        for (ResearchSource source : sources) {
            contentBuilder.append(source.getContent()).append("\n\n");
        }

        String prompt = "基于以下内容，生成一个更具体的搜索查询，用于深入研究'" + topic.getTopic() + "':\n" + contentBuilder.toString();
        return llmService.generateResponse(prompt);
    }

    private List<String> chunkContent(List<ResearchSource> sources) {
        List<String> chunks = new ArrayList<>();
        for (ResearchSource source : sources) {
            String content = source.getContent();
            int start = 0;
            while (start < content.length()) {
                int end = Math.min(start + chunkSize, content.length());
                // 确保切分点在单词边界
                if (end < content.length()) {
                    while (end > start && !Character.isWhitespace(content.charAt(end))) {
                        end--;
                    }
                }
                if (end <= start) {
                    end = start + chunkSize;
                }
                chunks.add(content.substring(start, end));
                start = end - chunkOverlap;
            }
        }
        return chunks;
    }

    private String generateSummary(List<String> chunks) {
        // 对每个分块生成摘要，然后合并
        StringBuilder summaryBuilder = new StringBuilder();
        for (String chunk : chunks) {
            summaryBuilder.append(llmService.summarizeText(chunk)).append("\n\n");
        }
        // 生成最终摘要
        return llmService.summarizeText(summaryBuilder.toString());
    }

    private String generateConclusion(List<String> chunks, String topic) {
        // 合并分块内容
        StringBuilder contentBuilder = new StringBuilder();
        for (String chunk : chunks) {
            contentBuilder.append(chunk).append("\n\n");
        }
        // 生成结论
        String prompt = "基于以下内容，生成一个关于'" + topic + "'的详细结论，包括现状、挑战和未来发展趋势:\n" + contentBuilder.toString();
        return llmService.generateResponse(prompt);
    }

    public ResearchResult getResearchResult(Long topicId) {
        try {
            ResearchTopic topic = topicRepository.findById(topicId).orElse(null);
            if (topic == null) {
                return null;
            }
            return resultRepository.findByTopic(topic);
        } catch (Exception e) {
            log.error("获取研究结果失败: {}", e.getMessage(), e);
            return null;
        }
    }

    public List<ResearchTopic> getAllTopics() {
        try {
            return topicRepository.findAll();
        } catch (Exception e) {
            log.error("获取研究主题失败: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
}
