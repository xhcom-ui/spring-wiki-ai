package com.example.deepresearch.repository;

import com.example.deepresearch.entity.ResearchTopic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResearchTopicRepository extends JpaRepository<ResearchTopic, Long> {
}
