package com.park.kafka;

import com.park.config.KafkaConfig;
import com.park.model.SensorData;
import com.park.model.AreaAggregation;
import com.park.util.RedisUtil;
import com.park.util.WebSocketHandler;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 聚合消费者
 */
@Slf4j
@Service
public class AggregateConsumer {

    @Resource
    private RedisUtil redisUtil;

    // 区域聚合缓存
    private final ConcurrentHashMap<String, AreaAggregation> aggregationCache = new ConcurrentHashMap<>();

    // 定时任务线程池
    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);

    public AggregateConsumer() {
        // 每1秒执行一次聚合
        executorService.scheduleAtFixedRate(this::aggregateAndUpdate, 1, 1, TimeUnit.SECONDS);
    }

    /**
     * 消费聚合消息
     */
    @KafkaListener(topics = KafkaConfig.TOPIC_PARKING_AGGREGATE, groupId = "park-consumer-group")
    public void consumeAggregateData(String message) {
        try {
            // 解析传感器数据
            SensorData sensorData = JSON.parseObject(message, SensorData.class);
            log.info("消费聚合数据: sensorId={}, spotId={}, status={}", 
                    sensorData.getSensorId(), sensorData.getSpotId(), sensorData.getStatus());

            // 构建区域ID
            String areaId = "area_" + sensorData.getParkingId() + "_1";

            // 更新聚合缓存
            aggregationCache.compute(areaId, (key, aggregation) -> {
                if (aggregation == null) {
                    aggregation = new AreaAggregation();
                    aggregation.setAreaId(areaId);
                    aggregation.setParkingId(sensorData.getParkingId());
                    aggregation.setTotalSpots(100); // 简化处理，实际项目中应该从数据库查询
                    aggregation.setFreeSpots(0);
                    aggregation.setOccupiedSpots(0);
                    aggregation.setFloor(1);
                }

                // 更新聚合数据
                if (sensorData.getStatus() == 0) {
                    aggregation.setFreeSpots(aggregation.getFreeSpots() + 1);
                    aggregation.setOccupiedSpots(aggregation.getOccupiedSpots() - 1);
                } else {
                    aggregation.setFreeSpots(aggregation.getFreeSpots() - 1);
                    aggregation.setOccupiedSpots(aggregation.getOccupiedSpots() + 1);
                }

                // 确保数据合理
                if (aggregation.getFreeSpots() < 0) {
                    aggregation.setFreeSpots(0);
                }
                if (aggregation.getOccupiedSpots() < 0) {
                    aggregation.setOccupiedSpots(0);
                }
                if (aggregation.getFreeSpots() > aggregation.getTotalSpots()) {
                    aggregation.setFreeSpots(aggregation.getTotalSpots());
                }
                if (aggregation.getOccupiedSpots() > aggregation.getTotalSpots()) {
                    aggregation.setOccupiedSpots(aggregation.getTotalSpots());
                }

                aggregation.setUpdateTime(LocalDateTime.now());
                return aggregation;
            });

        } catch (Exception e) {
            log.error("处理聚合数据失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 聚合并更新Redis
     */
    private void aggregateAndUpdate() {
        aggregationCache.forEach((areaId, aggregation) -> {
            // 存储到Redis
            redisUtil.setAreaAggregation(areaId, aggregation);
            log.info("区域聚合数据已更新到Redis: areaId={}, freeSpots={}, occupiedSpots={}", 
                    areaId, aggregation.getFreeSpots(), aggregation.getOccupiedSpots());

            // 通过WebSocket推送聚合更新
            WebSocketHandler.sendMessage(aggregation);
            log.info("区域聚合数据已通过WebSocket推送: areaId={}", areaId);
        });
    }
}
