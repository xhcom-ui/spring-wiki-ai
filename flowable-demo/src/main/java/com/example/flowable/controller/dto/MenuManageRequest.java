package com.example.flowable.controller.dto;

import lombok.Data;

@Data
public class MenuManageRequest {
    private String name;
    private String path;
    private String component;
    private String icon;
    private Long parentId;
    private Integer sort;
    private Integer type;
    private String permission;
    private Integer status;
}
