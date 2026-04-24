package com.example.flowable.entity;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Map<String, Object> processVariables;
    private List<String> visitedNodeLabels;
    private List<String> currentNodeLabels;
    private List<String> visitedFormLabels;
    private List<String> visitedPageLabels;
}
