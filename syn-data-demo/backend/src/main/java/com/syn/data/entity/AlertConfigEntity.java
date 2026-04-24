package com.syn.data.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("alert_config")
public class AlertConfigEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String type;

    private String level;

    private String threshold;

    private String notificationMethods;

    private String recipients;

    private Integer enabled;

    private String description;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
