package com.security.entity;

import lombok.Data;
import javax.persistence.*;

/**
 * 菜单实体
 */
@Data
@Entity
@Table(name = "sys_menu")
public class Menu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name; // 菜单名称

    @Column(nullable = false)
    private String path; // 菜单路径

    private String component; // 前端组件

    private String icon; // 菜单图标

    private Long parentId; // 父菜单ID

    private Integer orderNum; // 排序号

    @Column(nullable = false)
    private String permission; // 菜单权限

}
