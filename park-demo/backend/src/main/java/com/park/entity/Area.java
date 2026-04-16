package com.park.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 区域实体类
 */
@Data
@Entity
@Table(name = "area")
public class Area {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // 区域ID
    @Column(unique = true, nullable = false)
    private String areaId;
    
    // 停车场ID
    @Column(nullable = false)
    private String parkingId;
    
    // 区域名称
    @Column(nullable = false)
    private String name;
    
    // 总车位数
    @Column(nullable = false)
    private int totalSpots;
    
    // 楼层
    private int floor;
    
    // 创建时间
    private LocalDateTime createdAt;
    
    // 更新时间
    private LocalDateTime updatedAt;
}
