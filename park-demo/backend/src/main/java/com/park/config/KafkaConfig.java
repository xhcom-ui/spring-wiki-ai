package com.park.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Kafka配置类
 */
@Configuration
public class KafkaConfig {

    // 原始传感器数据主题
    public static final String TOPIC_PARKING_RAW = "parking.raw";
    // 清洗后的传感器数据主题
    public static final String TOPIC_PARKING_CLEANED = "parking.cleaned";
    // 车位状态主题
    public static final String TOPIC_PARKING_STATUS = "parking.status";
    // 区域聚合主题
    public static final String TOPIC_PARKING_AGGREGATE = "parking.aggregate";
    // 死信队列主题
    public static final String TOPIC_PARKING_DLQ = "parking.dlq";

    /**
     * 创建原始传感器数据主题
     */
    @Bean
    public NewTopic parkingRawTopic() {
        return TopicBuilder.name(TOPIC_PARKING_RAW)
                .partitions(3)
                .replicas(2)
                .build();
    }

    /**
     * 创建清洗后的传感器数据主题
     */
    @Bean
    public NewTopic parkingCleanedTopic() {
        return TopicBuilder.name(TOPIC_PARKING_CLEANED)
                .partitions(3)
                .replicas(2)
                .build();
    }

    /**
     * 创建车位状态主题
     */
    @Bean
    public NewTopic parkingStatusTopic() {
        return TopicBuilder.name(TOPIC_PARKING_STATUS)
                .partitions(3)
                .replicas(2)
                .build();
    }

    /**
     * 创建区域聚合主题
     */
    @Bean
    public NewTopic parkingAggregateTopic() {
        return TopicBuilder.name(TOPIC_PARKING_AGGREGATE)
                .partitions(3)
                .replicas(2)
                .build();
    }

    /**
     * 创建死信队列主题
     */
    @Bean
    public NewTopic parkingDlqTopic() {
        return TopicBuilder.name(TOPIC_PARKING_DLQ)
                .partitions(1)
                .replicas(1)
                .build();
    }
}
