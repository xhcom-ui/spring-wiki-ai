package com.example.flowlong.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "sys_menu")
public class Menu {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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
