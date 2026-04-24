package com.example.flowlong.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProcessDefinitionEntity {

    private Long id;

    private String processKey;

    private String processName;

    private String bpmnXml;

    private String designerType;

    private String designSchemaJson;

    private Integer version;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private Long createdBy;

    private Long updatedBy;
}
