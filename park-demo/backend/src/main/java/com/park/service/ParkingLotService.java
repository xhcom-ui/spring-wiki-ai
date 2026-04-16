package com.park.service;

import com.park.entity.ParkingLot;
import com.park.repository.ParkingLotRepository;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 停车场服务
 */
@Service
public class ParkingLotService {

    @Resource
    private ParkingLotRepository parkingLotRepository;

    /**
     * 获取所有停车场
     */
    public List<ParkingLot> getAllParkingLots() {
        return parkingLotRepository.findAll();
    }

    /**
     * 根据ID获取停车场
     */
    public ParkingLot getParkingLotById(String parkingId) {
        return parkingLotRepository.findByParkingId(parkingId);
    }

    /**
     * 创建停车场
     */
    public ParkingLot createParkingLot(ParkingLot parkingLot) {
        parkingLot.setCreatedAt(LocalDateTime.now());
        parkingLot.setUpdatedAt(LocalDateTime.now());
        return parkingLotRepository.save(parkingLot);
    }

    /**
     * 更新停车场
     */
    public ParkingLot updateParkingLot(ParkingLot parkingLot) {
        parkingLot.setUpdatedAt(LocalDateTime.now());
        return parkingLotRepository.save(parkingLot);
    }

    /**
     * 删除停车场
     */
    public void deleteParkingLot(Long id) {
        parkingLotRepository.deleteById(id);
    }
}
