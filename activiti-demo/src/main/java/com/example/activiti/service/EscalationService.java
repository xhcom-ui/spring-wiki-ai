package com.example.activiti.service;

import lombok.extern.slf4j.Slf4j;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EscalationService implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) {
        log.info("Escalating leave application due to approval delay...");
        
        // Get process variables
        String applicant = (String) execution.getVariable("applicant");
        String deptManager = (String) execution.getVariable("deptManager");
        
        // Log escalation
        log.info("Escalating leave application from {} because {} did not approve within 24 hours", applicant, deptManager);
        
        // Set escalation flag
        execution.setVariable("escalated", true);
        execution.setVariable("escalationReason", "部门经理审批超时");
        
        // In a real system, you might send an email or notification here
        // For example: sendEmailToGeneralManager(execution);
        
        log.info("Escalation process completed");
    }
}
