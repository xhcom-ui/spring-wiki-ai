package com.example.flowlong.controller.dto;

import lombok.Data;

@Data
public class FormCatalogManageRequest {
    private String formKey;
    private String title;
    private String description;
    private String schemaJson;
    private Integer status;
}
