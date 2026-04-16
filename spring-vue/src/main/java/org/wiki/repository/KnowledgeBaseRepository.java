package org.wiki.repository;

import org.wiki.entity.KnowledgeBase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KnowledgeBaseRepository extends JpaRepository<KnowledgeBase, Long> {
    List<KnowledgeBase> findByFileNameContaining(String fileName);
    List<KnowledgeBase> findByFileType(String fileType);
}
