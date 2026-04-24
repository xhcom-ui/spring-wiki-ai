package com.example.flowlong.repository;

import com.example.flowlong.entity.FlowLongDeploymentRecord;

import java.util.List;
import java.util.Optional;

public interface FlowLongDeploymentRecordRepository {
    FlowLongDeploymentRecord selectById(Long id);

    List<FlowLongDeploymentRecord> findByProcessKeyOrderByDeployedAtDescIdDesc(String processKey);

    FlowLongDeploymentRecord findFirstByProcessKeyAndFlowLongProcessVersionOrderByDeployedAtDescIdDesc(String processKey, Integer flowLongProcessVersion);

    int insert(FlowLongDeploymentRecord record);

    int update(FlowLongDeploymentRecord record);

    int removeById(Long id);

    default Optional<FlowLongDeploymentRecord> findById(Long id) {
        return Optional.ofNullable(selectById(id));
    }

    default FlowLongDeploymentRecord findFirstByProcessKeyOrderByDeployedAtDescIdDesc(String processKey) {
        List<FlowLongDeploymentRecord> items = findByProcessKeyOrderByDeployedAtDescIdDesc(processKey);
        return items.isEmpty() ? null : items.get(0);
    }

    default FlowLongDeploymentRecord save(FlowLongDeploymentRecord record) {
        if (record.getId() == null) {
            insert(record);
        } else {
            update(record);
        }
        return record;
    }
}
