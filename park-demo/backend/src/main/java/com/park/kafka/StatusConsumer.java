package com.park.kafka;

import com.park.config.KafkaConfig;
import com.park.model.SensorData;
import com.park.model.ParkingSpotStatus;
import com.park.util.RedisUtil;
import com.park.util.WebSocketHandler;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * 状态消费者
 */
@Slf4j
@Service
public class StatusConsumer {

    @Resource
    private RedisUtil redisUtil;

    @Resource
    private KafkaTemplate<String, String> kafkaTemplate;

    /**
     * 消费车位状态消息
     */
    @KafkaListener(topics = KafkaConfig.TOPIC_PARKING_STATUS, groupId = "park-consumer-group")
    public void consumeStatusData(String message) {
        try {
            // 解析传感器数据
            SensorData sensorData = JSON.parseObject(message, SensorData.class);
            log.info("消费车位状态数据: sensorId={}, spotId={}, status={}", 
                    sensorData.getSensorId(), sensorData.getSpotId(), sensorData.getStatus());

            // 构建车位状态
            ParkingSpotStatus status = new ParkingSpotStatus();
            status.setSpotId(sensorData.getSpotId());
            status.setParkingId(sensorData.getParkingId());
            status.setStatus(sensorData.getStatus());
            status.setUpdateTime(LocalDateTime.now());
            // 这里可以根据车位ID查询区域和楼层信息
            // 简化处理，实际项目中应该从数据库查询
            status.setAreaId("area_" + sensorData.getParkingId() + "_1");
            status.setFloor(1);

            // 存储到Redis
            redisUtil.setSpotStatus(sensorData.getSpotId(), status);
            log.info("车位状态已更新到Redis: spotId={}, status={}", sensorData.getSpotId(), sensorData.getStatus());

            // 通过WebSocket推送状态更新
            WebSocketHandler.sendMessage(status);
            log.info("车位状态已通过WebSocket推送: spotId={}", sensorData.getSpotId());

            // 转发到聚合主题，用于区域聚合
            kafkaTemplate.send(KafkaConfig.TOPIC_PARKING_AGGREGATE, message);
            log.info("数据已转发到聚合主题: {}", KafkaConfig.TOPIC_PARKING_AGGREGATE);
        } catch (Exception e) {
            log.error("处理车位状态数据失败: {}", e.getMessage(), e);
            // 转发到死信队列
            kafkaTemplate.send(KafkaConfig.TOPIC_PARKING_DLQ, message);
        }
    }
}
