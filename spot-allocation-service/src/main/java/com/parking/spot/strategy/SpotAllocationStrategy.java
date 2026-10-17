package com.parking.spot.strategy;

import com.parking.spot.domain.AllocationStrategyType;
import com.parking.spot.domain.ParkingSpot;
import com.parking.spot.domain.VehicleType;

import java.util.List;
import java.util.Optional;

/** Strategy pattern: how to pick ONE spot from the already-filtered, available, compatible candidates. */
public interface SpotAllocationStrategy {

    AllocationStrategyType type();

    Optional<ParkingSpot> select(List<ParkingSpot> availableCandidates, VehicleType vehicleType);
}
