package com.park.service;

import com.park.entity.ParkingSpot;
import com.park.repository.ParkingSpotRepository;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 车位服务
 */
@Service
public class ParkingSpotService {

    @Resource
    private ParkingSpotRepository parkingSpotRepository;

    /**
     * 获取所有车位
     */
    public List<ParkingSpot> getAllParkingSpots() {
        return parkingSpotRepository.findAll();
    }

    /**
     * 根据ID获取车位
     */
    public ParkingSpot getParkingSpotById(String spotId) {
        return parkingSpotRepository.findBySpotId(spotId);
    }

    /**
     * 根据停车场ID获取车位
     */
    public List<ParkingSpot> getParkingSpotsByParkingId(String parkingId) {
        return parkingSpotRepository.findByParkingId(parkingId);
    }

    /**
     * 根据停车场ID和区域ID获取车位
     */
    public List<ParkingSpot> getParkingSpotsByParkingIdAndAreaId(String parkingId, String areaId) {
        return parkingSpotRepository.findByParkingIdAndAreaId(parkingId, areaId);
    }

    /**
     * 根据停车场ID和楼层获取车位
     */
    public List<ParkingSpot> getParkingSpotsByParkingIdAndFloor(String parkingId, int floor) {
        return parkingSpotRepository.findByParkingIdAndFloor(parkingId, floor);
    }

    /**
     * 创建车位
     */
    public ParkingSpot createParkingSpot(ParkingSpot parkingSpot) {
        parkingSpot.setCreatedAt(LocalDateTime.now());
        parkingSpot.setUpdatedAt(LocalDateTime.now());
        return parkingSpotRepository.save(parkingSpot);
    }

    /**
     * 更新车位
     */
    public ParkingSpot updateParkingSpot(ParkingSpot parkingSpot) {
        parkingSpot.setUpdatedAt(LocalDateTime.now());
        return parkingSpotRepository.save(parkingSpot);
    }

    /**
     * 删除车位
     */
    public void deleteParkingSpot(Long id) {
        parkingSpotRepository.deleteById(id);
    }
}
