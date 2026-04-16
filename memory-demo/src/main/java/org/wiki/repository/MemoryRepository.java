package org.wiki.repository;

import org.wiki.entity.Memory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MemoryRepository extends JpaRepository<Memory, Long> {
    List<Memory> findByCategory(String category);
    List<Memory> findByImportanceGreaterThanEqual(int importance);
    List<Memory> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    List<Memory> findByCategoryAndImportanceGreaterThanEqual(String category, int importance);
}
