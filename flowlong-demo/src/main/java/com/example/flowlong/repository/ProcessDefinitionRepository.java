package com.example.flowlong.repository;

import com.example.flowlong.entity.ProcessDefinitionEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

public interface ProcessDefinitionRepository {
    ProcessDefinitionEntity selectById(Long id);

    List<ProcessDefinitionEntity> findAllByOrderByUpdatedAtDescIdDesc();

    List<ProcessDefinitionEntity> findByProcessKeyOrderByVersionDesc(String processKey);

    List<ProcessDefinitionEntity> findByProcessKeyAndStatusOrderByVersionDesc(@Param("processKey") String processKey, @Param("status") String status);

    List<ProcessDefinitionEntity> findByStatusOrderByUpdatedAtDesc(String status);

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

    default ProcessDefinitionEntity findFirstByProcessKeyAndStatusOrderByVersionDesc(String processKey, String status) {
        List<ProcessDefinitionEntity> items = findByProcessKeyAndStatusOrderByVersionDesc(processKey, status);
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
