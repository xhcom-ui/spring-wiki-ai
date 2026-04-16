package com.park.repository;

import com.park.entity.ParkingSpot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 车位Repository
 */
@Repository
public interface ParkingSpotRepository extends JpaRepository<ParkingSpot, Long> {
    ParkingSpot findBySpotId(String spotId);
    List<ParkingSpot> findByParkingId(String parkingId);
    List<ParkingSpot> findByParkingIdAndAreaId(String parkingId, String areaId);
    List<ParkingSpot> findByParkingIdAndFloor(String parkingId, int floor);
}
