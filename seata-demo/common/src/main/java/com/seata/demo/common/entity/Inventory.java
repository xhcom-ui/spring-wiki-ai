package com.seata.demo.common.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Inventory {
    private Long id;
    private Long productId;
    private Integer stock;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
