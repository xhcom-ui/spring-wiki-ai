package com.example.deepresearch.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "research_source")
public class ResearchSource {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private String url;
    private String content;
    private String type;
    private LocalDateTime createdAt;
    
    @ManyToOne
    @JoinColumn(name = "topic_id")
    private ResearchTopic topic;
}
