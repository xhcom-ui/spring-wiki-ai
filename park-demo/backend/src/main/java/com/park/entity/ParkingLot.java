package com.park.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 停车场实体类
 */
@Data
@Entity
@Table(name = "parking_lot")
public class ParkingLot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // 停车场ID
    @Column(unique = true, nullable = false)
    private String parkingId;
    
    // 停车场名称
    @Column(nullable = false)
    private String name;
    
    // 总车位数
    @Column(nullable = false)
    private int totalSpots;
    
    // 地址
    private String address;
    
    // 状态：0-关闭，1-开放
    @Column(nullable = false)
    private int status;
    
    // 创建时间
    private LocalDateTime createdAt;
    
    // 更新时间
    private LocalDateTime updatedAt;
}
