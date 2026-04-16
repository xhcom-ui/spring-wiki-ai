package com.example.activiti.repository;

import com.example.activiti.entity.ProcessDefinitionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProcessDefinitionRepository extends JpaRepository<ProcessDefinitionEntity, Long> {
    
    // Find process definitions by key
    List<ProcessDefinitionEntity> findByProcessKey(String processKey);
    
    // Find latest version of a process definition by key
    @Query("SELECT p FROM ProcessDefinitionEntity p WHERE p.processKey = :processKey ORDER BY p.version DESC LIMIT 1")
    ProcessDefinitionEntity findLatestByProcessKey(@Param("processKey") String processKey);
    
    // Find process definitions by status
    List<ProcessDefinitionEntity> findByStatus(String status);
    
    // Find process definition by deployment ID
    ProcessDefinitionEntity findByDeploymentId(String deploymentId);
}
