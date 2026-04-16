package com.park.model;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 区域聚合模型
 */
@Data
public class AreaAggregation {
    // 区域ID
    private String areaId;
    // 停车场ID
    private String parkingId;
    // 总车位数
    private int totalSpots;
    // 空闲车位数
    private int freeSpots;
    // 占用车位数
    private int occupiedSpots;
    // 更新时间
    private LocalDateTime updateTime;
    // 楼层
    private int floor;
}
