package com.example.activiti.controller.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class TaskInboxItem {
    private String id;
    private String name;
    private String processInstanceId;
    private String processDefinitionId;
    private String processDefinitionKey;
    private String processDefinitionName;
    private String taskDefinitionKey;
    private String businessKey;
    private String assignee;
    private String owner;
    private String applicant;
    private String leaveStatus;
    private String formKey;
    private String todoPage;
    private String donePage;
    private String assignmentMode;
    private Boolean canClaim;
    private Boolean claimed;
    private List<String> candidateUsers;
    private List<String> candidateGroups;
    private Map<String, Object> variables;
    private String createTime;
}
