package com.example.flowlong.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Leave {
    private Long id;
    private String processInstanceId;
    private String businessKey;
    private String applicant;
    private String deptManager;
    private String generalManager;
    private String status;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer days;
    private String reason;
    private String passedNodeLabelsJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
