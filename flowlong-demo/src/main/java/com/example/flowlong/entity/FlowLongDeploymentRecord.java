package com.example.flowlong.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FlowLongDeploymentRecord {

    private Long id;

    private String processKey;

    private String processName;

    private Long flowLongProcessId;

    private Integer flowLongProcessVersion;

    private Long sourceDefinitionId;

    private Integer sourceDefinitionVersion;

    private String sourceType;

    private Long deployedBy;

    private String deployedByName;

    private LocalDateTime deployedAt;
}
