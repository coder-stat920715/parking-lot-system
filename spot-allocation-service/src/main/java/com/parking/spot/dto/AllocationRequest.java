package com.parking.spot.dto;

import com.parking.spot.domain.AllocationStrategyType;
import com.parking.spot.domain.VehicleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AllocationRequest(
        @NotBlank @Size(max = 64) String ticketNumber,
        @NotNull VehicleType vehicleType,
        AllocationStrategyType strategy) {
}
