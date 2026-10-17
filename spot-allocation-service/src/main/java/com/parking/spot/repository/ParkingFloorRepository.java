package com.parking.spot.repository;

import com.parking.spot.domain.ParkingFloor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParkingFloorRepository extends JpaRepository<ParkingFloor, Long> {
}
