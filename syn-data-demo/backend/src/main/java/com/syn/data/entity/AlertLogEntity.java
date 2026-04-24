package com.syn.data.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("alert_log")
public class AlertLogEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long alertId;

    private String alertName;

    private String type;

    private String level;

    private String message;

    private Long taskId;

    private String taskName;

    private String status;

    private LocalDateTime sendTime;

    private String recipients;

    private LocalDateTime createdAt;
}
