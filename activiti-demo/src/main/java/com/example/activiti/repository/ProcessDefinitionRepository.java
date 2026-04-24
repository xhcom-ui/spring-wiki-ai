package com.example.activiti.repository;

import com.example.activiti.entity.ProcessDefinitionEntity;

import java.util.List;
import java.util.Optional;

public interface ProcessDefinitionRepository {
    ProcessDefinitionEntity selectById(Long id);

    List<ProcessDefinitionEntity> findAllByOrderByUpdatedAtDescIdDesc();

    List<ProcessDefinitionEntity> findByProcessKeyOrderByVersionDesc(String processKey);

    List<ProcessDefinitionEntity> findByStatusOrderByUpdatedAtDesc(String status);

    ProcessDefinitionEntity findByDeploymentId(String deploymentId);

    int insert(ProcessDefinitionEntity entity);

    int update(ProcessDefinitionEntity entity);

    int removeById(Long id);

    default Optional<ProcessDefinitionEntity> findById(Long id) {
        return Optional.ofNullable(selectById(id));
    }

    default ProcessDefinitionEntity findFirstByProcessKeyOrderByVersionDesc(String processKey) {
        List<ProcessDefinitionEntity> items = findByProcessKeyOrderByVersionDesc(processKey);
        return items.isEmpty() ? null : items.get(0);
    }

    default ProcessDefinitionEntity save(ProcessDefinitionEntity entity) {
        if (entity.getId() == null) {
            insert(entity);
        } else {
            update(entity);
        }
        return entity;
    }

    default void deleteById(Long id) {
        removeById(id);
    }
}
