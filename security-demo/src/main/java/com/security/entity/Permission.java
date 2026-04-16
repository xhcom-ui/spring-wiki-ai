package com.security.entity;

import lombok.Data;
import javax.persistence.*;
import java.util.Set;

/**
 * 权限实体
 */
@Data
@Entity
@Table(name = "sys_permission")
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private String url;

    @ManyToMany(mappedBy = "permissions")
    private Set<Role> roles;

}
