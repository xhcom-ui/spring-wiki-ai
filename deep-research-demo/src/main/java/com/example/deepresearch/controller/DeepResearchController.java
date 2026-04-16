package com.example.deepresearch.controller;

import com.example.deepresearch.entity.ResearchResult;
import com.example.deepresearch.entity.ResearchTopic;
import com.example.deepresearch.service.DeepResearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/research")
public class DeepResearchController {

    @Autowired
    private DeepResearchService deepResearchService;

    @PostMapping("/conduct")
    public ResponseEntity<ResearchResult> conductResearch(
            @RequestParam("topic") String topic,
            @RequestParam(value = "description", required = false) String description) {
        try {
            ResearchResult result = deepResearchService.conductResearch(topic, description);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/result/{topicId}")
    public ResponseEntity<ResearchResult> getResearchResult(@PathVariable Long topicId) {
        ResearchResult result = deepResearchService.getResearchResult(topicId);
        if (result == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/topics")
    public ResponseEntity<List<ResearchTopic>> getAllTopics() {
        List<ResearchTopic> topics = deepResearchService.getAllTopics();
        return ResponseEntity.ok(topics);
    }
}
