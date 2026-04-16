package com.example.deepresearch.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "research_topic")
public class ResearchTopic {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String topic;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
