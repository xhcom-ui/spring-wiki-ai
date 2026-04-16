package com.park.controller;

import com.park.entity.ParkingSpot;
import com.park.service.ParkingSpotService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 车位控制器
 */
@RestController
@RequestMapping("/api/parking-spots")
public class ParkingSpotController {

    @Resource
    private ParkingSpotService parkingSpotService;

    /**
     * 获取所有车位
     */
    @GetMapping
    public List<ParkingSpot> getAllParkingSpots() {
        return parkingSpotService.getAllParkingSpots();
    }

    /**
     * 根据ID获取车位
     */
    @GetMapping("/{spotId}")
    public ParkingSpot getParkingSpotById(@PathVariable String spotId) {
        return parkingSpotService.getParkingSpotById(spotId);
    }

    /**
     * 根据停车场ID获取车位
     */
    @GetMapping("/parking/{parkingId}")
    public List<ParkingSpot> getParkingSpotsByParkingId(@PathVariable String parkingId) {
        return parkingSpotService.getParkingSpotsByParkingId(parkingId);
    }

    /**
     * 根据停车场ID和区域ID获取车位
     */
    @GetMapping("/parking/{parkingId}/area/{areaId}")
    public List<ParkingSpot> getParkingSpotsByParkingIdAndAreaId(@PathVariable String parkingId, @PathVariable String areaId) {
        return parkingSpotService.getParkingSpotsByParkingIdAndAreaId(parkingId, areaId);
    }

    /**
     * 根据停车场ID和楼层获取车位
     */
    @GetMapping("/parking/{parkingId}/floor/{floor}")
    public List<ParkingSpot> getParkingSpotsByParkingIdAndFloor(@PathVariable String parkingId, @PathVariable int floor) {
        return parkingSpotService.getParkingSpotsByParkingIdAndFloor(parkingId, floor);
    }

    /**
     * 创建车位
     */
    @PostMapping
    public ParkingSpot createParkingSpot(@RequestBody ParkingSpot parkingSpot) {
        return parkingSpotService.createParkingSpot(parkingSpot);
    }

    /**
     * 更新车位
     */
    @PutMapping("/{id}")
    public ParkingSpot updateParkingSpot(@PathVariable Long id, @RequestBody ParkingSpot parkingSpot) {
        parkingSpot.setId(id);
        return parkingSpotService.updateParkingSpot(parkingSpot);
    }

    /**
     * 删除车位
     */
    @DeleteMapping("/{id}")
    public void deleteParkingSpot(@PathVariable Long id) {
        parkingSpotService.deleteParkingSpot(id);
    }
}
