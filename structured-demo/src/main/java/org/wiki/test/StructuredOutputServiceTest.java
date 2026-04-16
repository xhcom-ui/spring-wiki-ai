package org.wiki.test;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.wiki.entity.ActorFilms;
import org.wiki.entity.Address;
import org.wiki.entity.CodeReview;
import org.wiki.entity.MovieRecommendation;
import org.wiki.service.StructuredOutputService;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class StructuredOutputServiceTest {
    
    @Autowired
    private StructuredOutputService structuredOutputService;
    
    @Test
    void testExtractAddress() {
        String text = "收货人：李四，电话：13800138000，地址：北京市海淀区中关村大街1号，邮编：100080";
        
        Address address = structuredOutputService.extractAddress(text);
        
        assertThat(address).isNotNull();
        assertThat(address.getName()).isEqualTo("李四");
        assertThat(address.getPhone()).isEqualTo("13800138000");
        assertThat(address.getCity()).isEqualTo("北京市");
        assertThat(address.getZipCode()).isEqualTo("100080");
        
        System.out.println("提取的地址信息：" + address);
    }
    
    @Test
    void testGetActorFilmography() {
        String actor = "Leonardo DiCaprio";
        
        ActorFilms actorFilms = structuredOutputService.getActorFilmography(actor);
        
        assertThat(actorFilms).isNotNull();
        assertThat(actorFilms.getActor()).isEqualTo(actor);
        assertThat(actorFilms.getMovies()).isNotEmpty();
        assertThat(actorFilms.getMovies().size()).isGreaterThanOrEqualTo(5);
        
        System.out.println("演员电影作品：" + actorFilms);
    }
    
    @Test
    void testRecommendMovie() {
        String genre = "科幻";
        String language = "中文";
        
        MovieRecommendation recommendation =
            structuredOutputService.recommendMovie(genre, language);
        
        assertThat(recommendation).isNotNull();
        assertThat(recommendation.getTitle()).isNotBlank();
        assertThat(recommendation.getGenres()).contains(genre);
        assertThat(recommendation.getRating()).isBetween(0.0, 10.0);
        
        System.out.println("电影推荐：" + recommendation);
    }
    
    @Test
    void testReviewCode() {
        String code = """
            public class Example {
                public void badMethod() {
                    String s = "";
                    for (int i = 0; i < 100; i++) {
                        s += i;
                    }
                }
            }
            """;
        
        CodeReview review = structuredOutputService.reviewCode(code);
        
        assertThat(review).isNotNull();
        assertThat(review.getSummary()).isNotBlank();
        assertThat(review.getScore()).isBetween(0, 10);
        assertThat(review.getIssues()).isNotEmpty();
        
        System.out.println("代码审查结果：" + review);
    }
    
    @Test
    void testExtractPersonInfo() {
        String description = "张三，30岁，是一名软件工程师，住在上海。他喜欢编程、阅读和旅行。";
        
        Map<String, Object> personInfo = 
            structuredOutputService.extractPersonInfo(description);
        
        assertThat(personInfo).isNotNull();
        assertThat(personInfo.get("name")).isEqualTo("张三");
        assertThat(personInfo.get("age")).isEqualTo(30);
        assertThat(personInfo.get("occupation")).isEqualTo("软件工程师");
        
        System.out.println("提取的个人信息：" + personInfo);
    }
    
    @Test
    void testExtractKeywords() {
        String text = "人工智能是当今科技领域最热门的话题之一。机器学习、深度学习、神经网络等技术正在快速发展。";
        
        List<String> keywords = structuredOutputService.extractKeywords(text);
        
        assertThat(keywords).isNotNull();
        assertThat(keywords).isNotEmpty();
        assertThat(keywords.size()).isBetween(5, 10);
        
        System.out.println("提取的关键词：" + keywords);
    }
    
    @Test
    void testCategorizeBooks() {
        List<String> books = List.of(
            "三体", "时间简史", "百年孤独", 
            "人类简史", "小王子", "物种起源"
        );
        
        Map<String, List<String>> categories = 
            structuredOutputService.categorizeBooks(books);
        
        assertThat(categories).isNotNull();
        assertThat(categories.keySet()).contains("fiction", "nonfiction", "science");
        
        System.out.println("书籍分类：" + categories);
    }
}