package com.example.flowlong.controller.dto;

import lombok.Data;

@Data
public class DepartmentManageRequest {
    private String name;
    private String code;
    private String leader;
    private Long parentId;
    private Integer status;
    private String description;
}
