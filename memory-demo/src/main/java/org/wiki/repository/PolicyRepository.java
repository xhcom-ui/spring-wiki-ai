package org.wiki.repository;

import org.wiki.entity.Policy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PolicyRepository extends JpaRepository<Policy, Long> {
    List<Policy> findByName(String name);
    List<Policy> findByScope(String scope);
}
