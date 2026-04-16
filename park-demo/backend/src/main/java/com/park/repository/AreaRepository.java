package com.park.repository;

import com.park.entity.Area;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 区域Repository
 */
@Repository
public interface AreaRepository extends JpaRepository<Area, Long> {
    Area findByAreaId(String areaId);
    List<Area> findByParkingId(String parkingId);
    List<Area> findByParkingIdAndFloor(String parkingId, int floor);
}
