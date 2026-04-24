package com.example.flowlong.controller.dto;

import lombok.Data;

@Data
public class PostManageRequest {
    private String name;
    private String code;
    private Long departmentId;
    private String level;
    private Integer status;
    private String description;
}
