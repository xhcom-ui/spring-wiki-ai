package com.example.flowlong.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FormCatalog {
    private Long id;
    private String formKey;
    private String title;
    private String description;
    private String schemaJson;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
