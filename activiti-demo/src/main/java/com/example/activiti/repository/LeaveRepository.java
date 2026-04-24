package com.example.activiti.repository;

import com.example.activiti.entity.Leave;

import java.util.List;
import java.util.Optional;

public interface LeaveRepository {
    Leave selectById(Long id);

    Leave findByProcessInstanceId(String processInstanceId);

    List<Leave> findByApplicantOrderByCreatedAtDesc(String applicant);

    List<Leave> findAll();

    int insert(Leave leave);

    int update(Leave leave);

    int removeById(Long id);

    default Optional<Leave> findById(Long id) {
        return Optional.ofNullable(selectById(id));
    }

    default Leave save(Leave leave) {
        if (leave.getId() == null) {
            insert(leave);
        } else {
            update(leave);
        }
        return leave;
    }
}
