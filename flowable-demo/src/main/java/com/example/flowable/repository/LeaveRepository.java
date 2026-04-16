package com.example.flowable.repository;

import com.example.flowable.entity.Leave;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LeaveRepository extends JpaRepository<Leave, Long> {
    Leave findByProcessInstanceId(String processInstanceId);
    Leave findByBusinessKey(String businessKey);
}
