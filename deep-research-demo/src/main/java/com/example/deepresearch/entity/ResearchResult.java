package com.example.deepresearch.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "research_result")
public class ResearchResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String summary;
    private String conclusion;
    private String references;
    private LocalDateTime createdAt;
    
    @ManyToOne
    @JoinColumn(name = "topic_id")
    private ResearchTopic topic;
}
