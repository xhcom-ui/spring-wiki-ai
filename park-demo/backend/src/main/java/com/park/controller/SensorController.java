package com.park.controller;

import com.park.entity.Sensor;
import com.park.service.SensorService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 传感器控制器
 */
@RestController
@RequestMapping("/api/sensors")
public class SensorController {

    @Resource
    private SensorService sensorService;

    /**
     * 获取所有传感器
     */
    @GetMapping
    public List<Sensor> getAllSensors() {
        return sensorService.getAllSensors();
    }

    /**
     * 根据ID获取传感器
     */
    @GetMapping("/{sensorId}")
    public Sensor getSensorById(@PathVariable String sensorId) {
        return sensorService.getSensorById(sensorId);
    }

    /**
     * 根据停车场ID获取传感器
     */
    @GetMapping("/parking/{parkingId}")
    public List<Sensor> getSensorsByParkingId(@PathVariable String parkingId) {
        return sensorService.getSensorsByParkingId(parkingId);
    }

    /**
     * 根据停车场ID和状态获取传感器
     */
    @GetMapping("/parking/{parkingId}/status/{status}")
    public List<Sensor> getSensorsByParkingIdAndStatus(@PathVariable String parkingId, @PathVariable int status) {
        return sensorService.getSensorsByParkingIdAndStatus(parkingId, status);
    }

    /**
     * 创建传感器
     */
    @PostMapping
    public Sensor createSensor(@RequestBody Sensor sensor) {
        return sensorService.createSensor(sensor);
    }

    /**
     * 更新传感器
     */
    @PutMapping("/{id}")
    public Sensor updateSensor(@PathVariable Long id, @RequestBody Sensor sensor) {
        sensor.setId(id);
        return sensorService.updateSensor(sensor);
    }

    /**
     * 删除传感器
     */
    @DeleteMapping("/{id}")
    public void deleteSensor(@PathVariable Long id) {
        sensorService.deleteSensor(id);
    }
}
