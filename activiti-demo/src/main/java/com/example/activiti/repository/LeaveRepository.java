package com.example.activiti.repository;

import com.example.activiti.entity.Leave;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LeaveRepository extends JpaRepository<Leave, Long> {
    Leave findByProcessInstanceId(String processInstanceId);
}
