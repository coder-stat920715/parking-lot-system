package com.parking.spot.strategy;

import com.parking.spot.domain.AllocationStrategyType;
import com.parking.spot.domain.ParkingSpot;
import com.parking.spot.domain.VehicleType;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Picks the tightest-fitting spot type first (e.g. a motorcycle prefers SMALL over LARGE), keeping large spots
 * free for trucks; ties are broken by distance to the entrance.
 */
@Component
public class BestFitStrategy implements SpotAllocationStrategy {

    @Override
    public AllocationStrategyType type() {
        return AllocationStrategyType.BEST_FIT;
    }

    @Override
    public Optional<ParkingSpot> select(List<ParkingSpot> availableCandidates, VehicleType vehicleType) {
        List<com.parking.spot.domain.SpotType> preference = vehicleType.compatibleSpotTypes();
        return availableCandidates.stream()
                .min(Comparator.<ParkingSpot>comparingInt(s -> preference.indexOf(s.getSpotType()))
                        .thenComparingInt(ParkingSpot::getDistanceFromEntrance)
                        .thenComparing(ParkingSpot::getSpotCode));
    }
}
