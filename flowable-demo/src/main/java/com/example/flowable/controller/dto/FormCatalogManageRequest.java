package com.example.flowable.controller.dto;

import lombok.Data;

@Data
public class FormCatalogManageRequest {
    private String formKey;
    private String formName;
    private String pageLabel;
    private String componentKey;
    private String fieldSchemaJson;
    private String scope;
    private String description;
    private Integer sort;
    private Integer status;
}
