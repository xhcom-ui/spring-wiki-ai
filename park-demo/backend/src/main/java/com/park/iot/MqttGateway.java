package com.park.iot;

import com.park.config.KafkaConfig;
import com.park.model.SensorData;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
 * MQTT网关服务
 */
@Slf4j
@Service
public class MqttGateway {

    @Resource
    private MqttClient mqttClient;

    @Resource
    private KafkaTemplate<String, String> kafkaTemplate;

    @Value("${mqtt.topic}")
    private String mqttTopic;

    @PostConstruct
    public void init() {
        try {
            // 订阅MQTT主题
            mqttClient.subscribe(mqttTopic, (topic, message) -> {
                try {
                    // 解析消息
                    String payload = new String(message.getPayload());
                    log.info("收到MQTT消息: {}", payload);

                    // 转发到Kafka
                    kafkaTemplate.send(KafkaConfig.TOPIC_PARKING_RAW, payload);
                    log.info("消息已转发到Kafka: {}", KafkaConfig.TOPIC_PARKING_RAW);
                } catch (Exception e) {
                    log.error("处理MQTT消息失败: {}", e.getMessage(), e);
                }
            });
            log.info("MQTT网关初始化完成，已订阅主题: {}", mqttTopic);
        } catch (MqttException e) {
            log.error("MQTT网关初始化失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 发布消息到MQTT
     */
    public void publish(String topic, String message) {
        try {
            MqttMessage mqttMessage = new MqttMessage(message.getBytes());
            mqttMessage.setQos(1);
            mqttClient.publish(topic, mqttMessage);
            log.info("已发布消息到MQTT主题: {}", topic);
        } catch (MqttException e) {
            log.error("发布MQTT消息失败: {}", e.getMessage(), e);
        }
    }
}
