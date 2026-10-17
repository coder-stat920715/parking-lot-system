package com.parking.spot.domain;

import java.util.List;

/** Compatible spot types are ordered tightest-fit first (used by BestFitStrategy). */
public enum VehicleType {
    MOTORCYCLE(List.of(SpotType.SMALL, SpotType.MEDIUM, SpotType.LARGE)),
    CAR(List.of(SpotType.MEDIUM, SpotType.LARGE)),
    TRUCK(List.of(SpotType.LARGE)),
    EV_CAR(List.of(SpotType.EV));

    private final List<SpotType> compatibleSpotTypes;

    VehicleType(List<SpotType> compatibleSpotTypes) {
        this.compatibleSpotTypes = compatibleSpotTypes;
    }

    public List<SpotType> compatibleSpotTypes() {
        return compatibleSpotTypes;
    }
}
