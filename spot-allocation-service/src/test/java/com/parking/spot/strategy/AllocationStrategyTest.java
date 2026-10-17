package com.parking.spot.strategy;

import com.parking.spot.domain.ParkingSpot;
import com.parking.spot.domain.SpotType;
import com.parking.spot.domain.VehicleType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class AllocationStrategyTest {

    private static ParkingSpot spot(String code, SpotType type, int distance) {
        return ParkingSpot.builder().spotCode(code).spotType(type).distanceFromEntrance(distance).build();
    }

    @Test
    void nearestPicksSmallestDistanceEvenIfOversized() {
        List<ParkingSpot> candidates = List.of(
                spot("S-far", SpotType.SMALL, 50),
                spot("L-near", SpotType.LARGE, 2),
                spot("M-mid", SpotType.MEDIUM, 10));

        Optional<ParkingSpot> chosen = new NearestSpotStrategy().select(candidates, VehicleType.MOTORCYCLE);

        assertThat(chosen.orElseThrow().getSpotCode()).isEqualTo("L-near");
    }

    @Test
    void bestFitPrefersTightestSpotType() {
        List<ParkingSpot> candidates = List.of(
                spot("S-far", SpotType.SMALL, 50),
                spot("L-near", SpotType.LARGE, 2),
                spot("M-mid", SpotType.MEDIUM, 10));

        Optional<ParkingSpot> chosen = new BestFitStrategy().select(candidates, VehicleType.MOTORCYCLE);

        assertThat(chosen.orElseThrow().getSpotCode()).isEqualTo("S-far");
    }

    @Test
    void bestFitBreaksTiesByDistance() {
        List<ParkingSpot> candidates = List.of(
                spot("M-b", SpotType.MEDIUM, 9),
                spot("M-a", SpotType.MEDIUM, 3));

        assertThat(new BestFitStrategy().select(candidates, VehicleType.CAR).orElseThrow().getSpotCode())
                .isEqualTo("M-a");
    }

    @Test
    void emptyCandidatesYieldEmpty() {
        assertThat(new NearestSpotStrategy().select(List.of(), VehicleType.CAR)).isEmpty();
        assertThat(new BestFitStrategy().select(List.of(), VehicleType.CAR)).isEmpty();
    }
}
