package com.park.kafka;

import com.park.config.KafkaConfig;
import com.park.model.SensorData;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 传感器数据消费者
 */
@Slf4j
@Service
public class SensorDataConsumer {

    @Resource
    private KafkaTemplate<String, String> kafkaTemplate;

    // 去重缓存
    private final Map<String, LocalDateTime> dedupCache = new ConcurrentHashMap<>();

    /**
     * 消费原始传感器数据
     */
    @KafkaListener(topics = KafkaConfig.TOPIC_PARKING_RAW, groupId = "park-consumer-group")
    public void consumeRawData(String message) {
        try {
            // 解析传感器数据
            SensorData sensorData = JSON.parseObject(message, SensorData.class);
            log.info("消费原始传感器数据: sensorId={}, spotId={}, status={}", 
                    sensorData.getSensorId(), sensorData.getSpotId(), sensorData.getStatus());

            // 去重处理
            String key = sensorData.getSensorId() + "_" + sensorData.getStatus();
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime lastTime = dedupCache.get(key);

            if (lastTime == null || now.minusSeconds(5).isAfter(lastTime)) {
                // 数据有效，转发到清洗主题
                kafkaTemplate.send(KafkaConfig.TOPIC_PARKING_CLEANED, message);
                dedupCache.put(key, now);
                log.info("数据已转发到清洗主题: {}", KafkaConfig.TOPIC_PARKING_CLEANED);
            } else {
                log.info("数据重复，已过滤: {}", key);
            }
        } catch (Exception e) {
            log.error("处理原始传感器数据失败: {}", e.getMessage(), e);
            // 转发到死信队列
            kafkaTemplate.send(KafkaConfig.TOPIC_PARKING_DLQ, message);
        }
    }

    /**
     * 消费清洗后的传感器数据
     */
    @KafkaListener(topics = KafkaConfig.TOPIC_PARKING_CLEANED, groupId = "park-consumer-group")
    public void consumeCleanedData(String message) {
        try {
            // 这里可以添加更多的清洗逻辑
            // 例如：数据验证、格式转换等
            log.info("消费清洗后的传感器数据: {}", message);

            // 转发到状态主题
            kafkaTemplate.send(KafkaConfig.TOPIC_PARKING_STATUS, message);
            log.info("数据已转发到状态主题: {}", KafkaConfig.TOPIC_PARKING_STATUS);
        } catch (Exception e) {
            log.error("处理清洗后的传感器数据失败: {}", e.getMessage(), e);
            // 转发到死信队列
            kafkaTemplate.send(KafkaConfig.TOPIC_PARKING_DLQ, message);
        }
    }
}
