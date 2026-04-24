package com.example.flowlong.entity;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class Role {
    private Long id;
    private String name;
    private String code;
    private String description;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<Menu> menus;
}
