package com.park.util;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import com.alibaba.fastjson.JSON;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * Redis工具类
 */
@Component
public class RedisUtil {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    // 车位状态前缀
    private static final String SPOT_STATUS_PREFIX = "parking:spot:status:";
    // 区域聚合前缀
    private static final String AREA_AGGREGATE_PREFIX = "parking:area:aggregate:";
    // 停车场状态前缀
    private static final String PARKING_STATUS_PREFIX = "parking:status:";

    /**
     * 存储车位状态
     */
    public void setSpotStatus(String spotId, Object status) {
        String key = SPOT_STATUS_PREFIX + spotId;
        redisTemplate.opsForValue().set(key, JSON.toJSONString(status), 1, TimeUnit.HOURS);
    }

    /**
     * 获取车位状态
     */
    public <T> T getSpotStatus(String spotId, Class<T> clazz) {
        String key = SPOT_STATUS_PREFIX + spotId;
        String value = (String) redisTemplate.opsForValue().get(key);
        return value != null ? JSON.parseObject(value, clazz) : null;
    }

    /**
     * 存储区域聚合数据
     */
    public void setAreaAggregation(String areaId, Object aggregation) {
        String key = AREA_AGGREGATE_PREFIX + areaId;
        redisTemplate.opsForValue().set(key, JSON.toJSONString(aggregation), 1, TimeUnit.HOURS);
    }

    /**
     * 获取区域聚合数据
     */
    public <T> T getAreaAggregation(String areaId, Class<T> clazz) {
        String key = AREA_AGGREGATE_PREFIX + areaId;
        String value = (String) redisTemplate.opsForValue().get(key);
        return value != null ? JSON.parseObject(value, clazz) : null;
    }

    /**
     * 存储停车场状态
     */
    public void setParkingStatus(String parkingId, Object status) {
        String key = PARKING_STATUS_PREFIX + parkingId;
        redisTemplate.opsForValue().set(key, JSON.toJSONString(status), 1, TimeUnit.HOURS);
    }

    /**
     * 获取停车场状态
     */
    public <T> T getParkingStatus(String parkingId, Class<T> clazz) {
        String key = PARKING_STATUS_PREFIX + parkingId;
        String value = (String) redisTemplate.opsForValue().get(key);
        return value != null ? JSON.parseObject(value, clazz) : null;
    }

    /**
     * 删除键
     */
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    /**
     * 清除所有车位状态
     */
    public void clearSpotStatuses() {
        // 这里简化处理，实际项目中应该使用scan命令
        redisTemplate.delete(redisTemplate.keys(SPOT_STATUS_PREFIX + "*"));
    }

    /**
     * 清除所有区域聚合数据
     */
    public void clearAreaAggregations() {
        redisTemplate.delete(redisTemplate.keys(AREA_AGGREGATE_PREFIX + "*"));
    }
}
