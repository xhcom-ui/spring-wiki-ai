package com.park.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 车位实体类
 */
@Data
@Entity
@Table(name = "parking_spot")
public class ParkingSpot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // 车位ID
    @Column(unique = true, nullable = false)
    private String spotId;
    
    // 停车场ID
    @Column(nullable = false)
    private String parkingId;
    
    // 区域ID
    private String areaId;
    
    // 楼层
    private int floor;
    
    // 车位编号
    private String spotNumber;
    
    // 状态：0-空闲，1-占用，2-故障
    @Column(nullable = false)
    private int status;
    
    // 传感器ID
    private String sensorId;
    
    // 创建时间
    private LocalDateTime createdAt;
    
    // 更新时间
    private LocalDateTime updatedAt;
}
