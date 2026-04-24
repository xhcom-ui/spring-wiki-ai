package com.example.activiti.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Menu {
    private Long id;
    private String name;
    private String path;
    private String component;
    private String icon;
    private Long parentId;
    private Integer sort;
    private Integer type;
    private String permission;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
