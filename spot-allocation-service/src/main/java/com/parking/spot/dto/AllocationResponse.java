package com.parking.spot.dto;

import com.parking.spot.domain.AllocationStrategyType;
import com.parking.spot.domain.SpotType;

public record AllocationResponse(
        String ticketNumber,
        String spotCode,
        int floorNumber,
        SpotType spotType,
        int distanceFromEntrance,
        AllocationStrategyType strategyUsed,
        String servedBy) {
}
