package com.example.activiti.service;

import lombok.extern.slf4j.Slf4j;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NotificationService implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) {
        log.info("Sending notification to applicant...");
        
        // Get process variables
        String applicant = (String) execution.getVariable("applicant");
        String generalApproval = (String) execution.getVariable("generalApproval");
        Integer days = (Integer) execution.getVariable("days");
        String reason = (String) execution.getVariable("reason");
        
        // Determine notification message
        String message = "";
        if ("approved".equals(generalApproval)) {
            message = String.format("亲爱的 %s，您的请假申请已批准！请假天数：%d天，请假原因：%s", applicant, days, reason);
        } else {
            message = String.format("亲爱的 %s，您的请假申请未通过。请假天数：%d天，请假原因：%s", applicant, days, reason);
        }
        
        // Set notification message
        execution.setVariable("notificationMessage", message);
        
        // Log notification
        log.info("Notification sent to {}: {}", applicant, message);
        
        // In a real system, you might send an email, SMS, or push notification here
        // For example: sendEmailToApplicant(applicant, message);
        // Or: sendSmsToApplicant(applicant, message);
        
        log.info("Notification process completed");
    }
}
