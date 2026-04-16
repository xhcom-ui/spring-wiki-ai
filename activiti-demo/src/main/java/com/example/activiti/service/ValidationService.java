package com.example.activiti.service;

import lombok.extern.slf4j.Slf4j;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ValidationService implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) {
        log.info("Validating leave application...");
        
        // Get process variables
        String applicant = (String) execution.getVariable("applicant");
        Integer days = (Integer) execution.getVariable("days");
        
        // Validate leave application
        boolean isValid = true;
        String validationMessage = "";
        
        if (applicant == null || applicant.isEmpty()) {
            isValid = false;
            validationMessage = "申请人不能为空";
        } else if (days == null || days <= 0) {
            isValid = false;
            validationMessage = "请假天数必须大于0";
        } else if (days > 30) {
            isValid = false;
            validationMessage = "请假天数不能超过30天";
        }
        
        // Set validation result
        execution.setVariable("validationResult", isValid ? "valid" : "invalid");
        execution.setVariable("validationMessage", validationMessage);
        
        log.info("Validation result: {} - {}", isValid ? "valid" : "invalid", validationMessage);
    }
}
