package com.syn.data.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体类
 */
@Data
@TableName("user")
public class User {
    @TableId
    private Long id;
    private String username;
    private String password;
    private String name;
    private String email;
    private String phone;
    private String role; // admin, user
    private Integer status; // 1-启用, 0-禁用
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
