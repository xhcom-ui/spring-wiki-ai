package com.park.controller;

import com.park.entity.ParkingLot;
import com.park.service.ParkingLotService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 停车场控制器
 */
@RestController
@RequestMapping("/api/parking-lots")
public class ParkingLotController {

    @Resource
    private ParkingLotService parkingLotService;

    /**
     * 获取所有停车场
     */
    @GetMapping
    public List<ParkingLot> getAllParkingLots() {
        return parkingLotService.getAllParkingLots();
    }

    /**
     * 根据ID获取停车场
     */
    @GetMapping("/{parkingId}")
    public ParkingLot getParkingLotById(@PathVariable String parkingId) {
        return parkingLotService.getParkingLotById(parkingId);
    }

    /**
     * 创建停车场
     */
    @PostMapping
    public ParkingLot createParkingLot(@RequestBody ParkingLot parkingLot) {
        return parkingLotService.createParkingLot(parkingLot);
    }

    /**
     * 更新停车场
     */
    @PutMapping("/{id}")
    public ParkingLot updateParkingLot(@PathVariable Long id, @RequestBody ParkingLot parkingLot) {
        parkingLot.setId(id);
        return parkingLotService.updateParkingLot(parkingLot);
    }

    /**
     * 删除停车场
     */
    @DeleteMapping("/{id}")
    public void deleteParkingLot(@PathVariable Long id) {
        parkingLotService.deleteParkingLot(id);
    }
}
