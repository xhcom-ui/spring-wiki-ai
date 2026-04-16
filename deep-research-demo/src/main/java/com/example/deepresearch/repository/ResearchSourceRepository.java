package com.example.deepresearch.repository;

import com.example.deepresearch.entity.ResearchSource;
import com.example.deepresearch.entity.ResearchTopic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResearchSourceRepository extends JpaRepository<ResearchSource, Long> {
    List<ResearchSource> findByTopic(ResearchTopic topic);
}
