package com.seata.demo.inventory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_inventory")
public class Inventory {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long productId;
    private Integer stock;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
