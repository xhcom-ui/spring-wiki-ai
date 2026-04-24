package com.example.activiti.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TaskPageCatalogItem {
    private Long id;
    private String itemKey;
    private String label;
    private String itemType;
    private String pageMode;
    private String businessKind;
    private String description;
    private String defaultTodoPage;
    private String defaultDonePage;
    private Integer systemFlag;
    private Integer sort;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
