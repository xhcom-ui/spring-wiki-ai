package com.park.service;

import com.park.entity.Sensor;
import com.park.repository.SensorRepository;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 传感器服务
 */
@Service
public class SensorService {

    @Resource
    private SensorRepository sensorRepository;

    /**
     * 获取所有传感器
     */
    public List<Sensor> getAllSensors() {
        return sensorRepository.findAll();
    }

    /**
     * 根据ID获取传感器
     */
    public Sensor getSensorById(String sensorId) {
        return sensorRepository.findBySensorId(sensorId);
    }

    /**
     * 根据停车场ID获取传感器
     */
    public List<Sensor> getSensorsByParkingId(String parkingId) {
        return sensorRepository.findByParkingId(parkingId);
    }

    /**
     * 根据停车场ID和状态获取传感器
     */
    public List<Sensor> getSensorsByParkingIdAndStatus(String parkingId, int status) {
        return sensorRepository.findByParkingIdAndStatus(parkingId, status);
    }

    /**
     * 创建传感器
     */
    public Sensor createSensor(Sensor sensor) {
        sensor.setCreatedAt(LocalDateTime.now());
        sensor.setUpdatedAt(LocalDateTime.now());
        return sensorRepository.save(sensor);
    }

    /**
     * 更新传感器
     */
    public Sensor updateSensor(Sensor sensor) {
        sensor.setUpdatedAt(LocalDateTime.now());
        return sensorRepository.save(sensor);
    }

    /**
     * 删除传感器
     */
    public void deleteSensor(Long id) {
        sensorRepository.deleteById(id);
    }
}
