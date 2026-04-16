package org.wiki.repository;

import org.wiki.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findByScope(String scope);
    List<Event> findByMemoryId(Long memoryId);
    List<Event> findByTimestampBetween(LocalDateTime start, LocalDateTime end);
    List<Event> findByScopeAndTimestampBetween(String scope, LocalDateTime start, LocalDateTime end);
    List<Event> findByMemoryChange(String memoryChange);
}
