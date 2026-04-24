package com.example.activiti.controller.dto;

import lombok.Data;

@Data
public class ProcessDefinitionRequest {
    private String processKey;
    private String processName;
    private String bpmnXml;
    private String designerType;
    private String designerJson;
    private Long userId;
}
