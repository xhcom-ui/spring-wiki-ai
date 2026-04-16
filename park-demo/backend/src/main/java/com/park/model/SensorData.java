package com.park.model;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 传感器数据模型
 */
@Data
public class SensorData {
    // 传感器ID
    private String sensorId;
    // 停车场ID
    private String parkingId;
    // 车位ID
    private String spotId;
    // 状态：0-空闲，1-占用
    private int status;
    // 上报时间
    private LocalDateTime timestamp;
    // 信号强度
    private int signalStrength;
    // 电池电量
    private int batteryLevel;
}
