package com.example.flowable.controller.dto;

import lombok.Data;

import java.util.List;

@Data
public class RoleManageRequest {
    private String name;
    private String code;
    private String description;
    private Integer status;
    private List<Long> menuIds;
}
