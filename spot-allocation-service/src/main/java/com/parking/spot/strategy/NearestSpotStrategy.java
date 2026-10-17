package com.parking.spot.strategy;

import com.parking.spot.domain.AllocationStrategyType;
import com.parking.spot.domain.ParkingSpot;
import com.parking.spot.domain.VehicleType;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Picks the compatible spot closest to the entrance, regardless of how oversized it is for the vehicle. */
@Component
public class NearestSpotStrategy implements SpotAllocationStrategy {

    @Override
    public AllocationStrategyType type() {
        return AllocationStrategyType.NEAREST;
    }

    @Override
    public Optional<ParkingSpot> select(List<ParkingSpot> availableCandidates, VehicleType vehicleType) {
        return availableCandidates.stream()
                .min(Comparator.comparingInt(ParkingSpot::getDistanceFromEntrance)
                        .thenComparing(ParkingSpot::getSpotCode));
    }
}
