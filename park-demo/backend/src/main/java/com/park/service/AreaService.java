package com.park.service;

import com.park.entity.Area;
import com.park.repository.AreaRepository;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 区域服务
 */
@Service
public class AreaService {

    @Resource
    private AreaRepository areaRepository;

    /**
     * 获取所有区域
     */
    public List<Area> getAllAreas() {
        return areaRepository.findAll();
    }

    /**
     * 根据ID获取区域
     */
    public Area getAreaById(String areaId) {
        return areaRepository.findByAreaId(areaId);
    }

    /**
     * 根据停车场ID获取区域
     */
    public List<Area> getAreasByParkingId(String parkingId) {
        return areaRepository.findByParkingId(parkingId);
    }

    /**
     * 根据停车场ID和楼层获取区域
     */
    public List<Area> getAreasByParkingIdAndFloor(String parkingId, int floor) {
        return areaRepository.findByParkingIdAndFloor(parkingId, floor);
    }

    /**
     * 创建区域
     */
    public Area createArea(Area area) {
        area.setCreatedAt(LocalDateTime.now());
        area.setUpdatedAt(LocalDateTime.now());
        return areaRepository.save(area);
    }

    /**
     * 更新区域
     */
    public Area updateArea(Area area) {
        area.setUpdatedAt(LocalDateTime.now());
        return areaRepository.save(area);
    }

    /**
     * 删除区域
     */
    public void deleteArea(Long id) {
        areaRepository.deleteById(id);
    }
}
