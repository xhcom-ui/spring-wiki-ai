package com.park.model;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 车位状态模型
 */
@Data
public class ParkingSpotStatus {
    // 车位ID
    private String spotId;
    // 停车场ID
    private String parkingId;
    // 状态：0-空闲，1-占用
    private int status;
    // 更新时间
    private LocalDateTime updateTime;
    // 区域ID
    private String areaId;
    // 楼层
    private int floor;
}
