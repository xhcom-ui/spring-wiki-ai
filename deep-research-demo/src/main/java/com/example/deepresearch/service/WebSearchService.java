package com.example.deepresearch.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class WebSearchService {

    public List<SearchResult> search(String query, int maxResults) {
        // 这里应该调用网络搜索 API 获取相关信息
        // 由于是示例，这里返回模拟的搜索结果
        log.info("搜索网络信息: {}, 最大结果数: {}", query, maxResults);
        // 模拟搜索结果，实际应用中应该调用真实的搜索 API
        return List.of(
            new SearchResult("结果 1", "https://example.com/1", "这是搜索结果 1 的内容"),
            new SearchResult("结果 2", "https://example.com/2", "这是搜索结果 2 的内容"),
            new SearchResult("结果 3", "https://example.com/3", "这是搜索结果 3 的内容")
        );
    }

    public static class SearchResult {
        private String title;
        private String url;
        private String content;

        public SearchResult(String title, String url, String content) {
            this.title = title;
            this.url = url;
            this.content = content;
        }

        public String getTitle() {
            return title;
        }

        public String getUrl() {
            return url;
        }

        public String getContent() {
            return content;
        }
    }
}
