package com.park.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 传感器实体类
 */
@Data
@Entity
@Table(name = "sensor")
public class Sensor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // 传感器ID
    @Column(unique = true, nullable = false)
    private String sensorId;
    
    // 停车场ID
    @Column(nullable = false)
    private String parkingId;
    
    // 车位ID
    private String spotId;
    
    // 类型：0-地磁，1-视频，2-地锁
    @Column(nullable = false)
    private int type;
    
    // 状态：0-离线，1-在线
    @Column(nullable = false)
    private int status;
    
    // 信号强度
    private int signalStrength;
    
    // 电池电量
    private int batteryLevel;
    
    // 最后上报时间
    private LocalDateTime lastReportedAt;
    
    // 创建时间
    private LocalDateTime createdAt;
    
    // 更新时间
    private LocalDateTime updatedAt;
}
