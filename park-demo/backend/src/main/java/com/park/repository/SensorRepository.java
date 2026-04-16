package com.park.repository;

import com.park.entity.Sensor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 传感器Repository
 */
@Repository
public interface SensorRepository extends JpaRepository<Sensor, Long> {
    Sensor findBySensorId(String sensorId);
    List<Sensor> findByParkingId(String parkingId);
    List<Sensor> findByParkingIdAndStatus(String parkingId, int status);
}
