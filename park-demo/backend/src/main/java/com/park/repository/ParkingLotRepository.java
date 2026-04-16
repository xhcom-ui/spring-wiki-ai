package com.park.repository;

import com.park.entity.ParkingLot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 停车场Repository
 */
@Repository
public interface ParkingLotRepository extends JpaRepository<ParkingLot, Long> {
    ParkingLot findByParkingId(String parkingId);
}
