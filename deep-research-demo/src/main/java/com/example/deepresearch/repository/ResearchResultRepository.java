package com.example.deepresearch.repository;

import com.example.deepresearch.entity.ResearchResult;
import com.example.deepresearch.entity.ResearchTopic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResearchResultRepository extends JpaRepository<ResearchResult, Long> {
    ResearchResult findByTopic(ResearchTopic topic);
}
