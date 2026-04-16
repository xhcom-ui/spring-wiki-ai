package org.wiki.controller;


import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.wiki.entity.ActorFilms;
import org.wiki.entity.Address;
import org.wiki.entity.CodeReview;
import org.wiki.entity.MovieRecommendation;
import org.wiki.service.StructuredOutputService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/structured")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class StructuredOutputController {
    
    private final StructuredOutputService structuredOutputService;
    
    // 1. 地址提取接口
    @PostMapping("/extract-address")
    public ResponseEntity<Address> extractAddress(@RequestBody Map<String, String> request) {
        String text = request.get("text");
        Address address = structuredOutputService.extractAddress(text);
        return ResponseEntity.ok(address);
    }
    
    // 2. 获取演员电影作品
    @GetMapping("/actor-films/{actor}")
    public ResponseEntity<ActorFilms> getActorFilms(@PathVariable String actor) {
        ActorFilms actorFilms = structuredOutputService.getActorFilmography(actor);
        return ResponseEntity.ok(actorFilms);
    }
    
    // 3. 电影推荐
    @GetMapping("/movie-recommendation")
    public ResponseEntity<MovieRecommendation> recommendMovie(
            @RequestParam String genre,
            @RequestParam(defaultValue = "中文") String language) {
        MovieRecommendation recommendation = 
            structuredOutputService.recommendMovie(genre, language);
        return ResponseEntity.ok(recommendation);
    }
    
    // 4. 代码审查
    @PostMapping("/code-review")
    public ResponseEntity<CodeReview> reviewCode(@RequestBody Map<String, String> request) {
        String code = request.get("code");
        CodeReview review = structuredOutputService.reviewCode(code);
        return ResponseEntity.ok(review);
    }
    
    // 5. 提取个人信息
    @PostMapping("/extract-person-info")
    public ResponseEntity<Map<String, Object>> extractPersonInfo(
            @RequestBody Map<String, String> request) {
        String description = request.get("description");
        Map<String, Object> personInfo = 
            structuredOutputService.extractPersonInfo(description);
        return ResponseEntity.ok(personInfo);
    }
    
    // 6. 提取关键词
    @PostMapping("/extract-keywords")
    public ResponseEntity<List<String>> extractKeywords(@RequestBody Map<String, String> request) {
        String text = request.get("text");
        List<String> keywords = structuredOutputService.extractKeywords(text);
        return ResponseEntity.ok(keywords);
    }
    
    // 7. 书籍分类
    @PostMapping("/categorize-books")
    public ResponseEntity<Map<String, List<String>>> categorizeBooks(
            @RequestBody Map<String, List<String>> request) {
        List<String> bookTitles = request.get("books");
        Map<String, List<String>> categories = 
            structuredOutputService.categorizeBooks(bookTitles);
        return ResponseEntity.ok(categories);
    }
    
    // 8. 流式响应
    @PostMapping("/stream")
    public ResponseEntity<String> streamResponse(@RequestBody Map<String, String> request) {
        String query = request.get("query");
        String response = structuredOutputService.streamStructuredResponse(query);
        return ResponseEntity.ok(response);
    }
    
    // 9. 健康检查
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Spring AI Structured Output Service is running");
    }
}