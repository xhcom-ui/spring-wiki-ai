package com.example.flowable.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FormCatalog {
    private Long id;
    private String formKey;
    private String formName;
    private String pageLabel;
    private String componentKey;
    private String fieldSchemaJson;
    private String scope;
    private String description;
    private Integer sort;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
