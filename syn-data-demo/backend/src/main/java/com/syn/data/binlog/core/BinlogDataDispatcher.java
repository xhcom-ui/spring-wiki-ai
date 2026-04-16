package com.syn.data.binlog.core;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
public class BinlogDataDispatcher implements MySQLBinlogEventListener {
    private final Map<Long, MySqlTable> tableNameMap = new HashMap<>();
    private final Map<String, List<DataListenerContainer>> listenerMap = new HashMap<>();
    private RedisTemplate<String, Object> redisTemplate;

    public void setRedisTemplate(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void addListener(String database, String table, List<DataListenerContainer> listeners) {
        String key = database + "." + table;
        this.listenerMap.put(key, listeners);
    }

    @Override
    public void onEvent(Object event) {
        // 这里需要根据实际的事件格式进行解析
        // 目前只是一个简单的实现
        log.info("收到Binlog事件: {}", event);
        
        // 这里可以添加事件解析逻辑
        // 例如：
        // if (event instanceof TableMapEvent) {
        //     handleTableMapEvent((TableMapEvent) event);
        // } else if (event instanceof UpdateRowsEvent) {
        //     handleUpdateRowsEvent((UpdateRowsEvent) event);
        // } else if (event instanceof WriteRowsEvent) {
        //     handleWriteRowsEvent((WriteRowsEvent) event);
        // } else if (event instanceof DeleteRowsEvent) {
        //     handleDeleteRowsEvent((DeleteRowsEvent) event);
        // }
    }

    private boolean lock(Object header) {
        // 这里需要根据实际的header格式进行处理
        // 目前只是一个简单的实现
        String key = "binlog_" + System.currentTimeMillis();
        boolean lockResult = Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, "1", 600, TimeUnit.SECONDS));
        log.info("BinlogDataDispatcher加锁key:{},结果{}：", key, lockResult);
        //避免多机部署同时消费问题
        return lockResult;
    }
}
