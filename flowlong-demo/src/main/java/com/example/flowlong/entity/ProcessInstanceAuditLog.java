package com.example.flowlong.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProcessInstanceAuditLog {
    private Long id;
    private String processKey;
    private String processInstanceId;
    private String businessKey;
    private Long taskId;
    private String taskKey;
    private String taskName;
    private String nodeLabel;
    private String pageLabel;
    private String formKey;
    private String formLabel;
    private String actionType;
    private String comment;
    private String approvalComment;
    private String rejectReason;
    private String systemRemark;
    private String variableSnapshotJson;
    private String formSnapshotJson;
    private Long operatorId;
    private String operatorName;
    private LocalDateTime createdAt;
}
