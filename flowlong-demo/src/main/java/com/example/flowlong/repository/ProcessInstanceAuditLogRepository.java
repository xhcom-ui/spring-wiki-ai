package com.example.flowlong.repository;

import com.example.flowlong.entity.ProcessInstanceAuditLog;

import java.util.List;
import java.util.Optional;

public interface ProcessInstanceAuditLogRepository {
    ProcessInstanceAuditLog selectById(Long id);

    List<ProcessInstanceAuditLog> findByProcessInstanceIdOrderByCreatedAtAscIdAsc(String processInstanceId);

    int insert(ProcessInstanceAuditLog entity);

    default Optional<ProcessInstanceAuditLog> findById(Long id) {
        return Optional.ofNullable(selectById(id));
    }

    default ProcessInstanceAuditLog save(ProcessInstanceAuditLog entity) {
        insert(entity);
        return entity;
    }
}
