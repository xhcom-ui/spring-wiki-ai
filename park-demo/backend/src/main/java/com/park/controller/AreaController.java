package com.park.controller;

import com.park.entity.Area;
import com.park.service.AreaService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 区域控制器
 */
@RestController
@RequestMapping("/api/areas")
public class AreaController {

    @Resource
    private AreaService areaService;

    /**
     * 获取所有区域
     */
    @GetMapping
    public List<Area> getAllAreas() {
        return areaService.getAllAreas();
    }

    /**
     * 根据ID获取区域
     */
    @GetMapping("/{areaId}")
    public Area getAreaById(@PathVariable String areaId) {
        return areaService.getAreaById(areaId);
    }

    /**
     * 根据停车场ID获取区域
     */
    @GetMapping("/parking/{parkingId}")
    public List<Area> getAreasByParkingId(@PathVariable String parkingId) {
        return areaService.getAreasByParkingId(parkingId);
    }

    /**
     * 根据停车场ID和楼层获取区域
     */
    @GetMapping("/parking/{parkingId}/floor/{floor}")
    public List<Area> getAreasByParkingIdAndFloor(@PathVariable String parkingId, @PathVariable int floor) {
        return areaService.getAreasByParkingIdAndFloor(parkingId, floor);
    }

    /**
     * 创建区域
     */
    @PostMapping
    public Area createArea(@RequestBody Area area) {
        return areaService.createArea(area);
    }

    /**
     * 更新区域
     */
    @PutMapping("/{id}")
    public Area updateArea(@PathVariable Long id, @RequestBody Area area) {
        area.setId(id);
        return areaService.updateArea(area);
    }

    /**
     * 删除区域
     */
    @DeleteMapping("/{id}")
    public void deleteArea(@PathVariable Long id) {
        areaService.deleteArea(id);
    }
}
