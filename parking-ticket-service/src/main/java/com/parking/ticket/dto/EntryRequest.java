package com.parking.ticket.dto;

import com.parking.ticket.domain.AllocationStrategyType;
import com.parking.ticket.domain.VehicleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record EntryRequest(
        @NotBlank
        @Pattern(regexp = "^[A-Za-z0-9\\- ]{3,20}$", message = "must be 3-20 letters, digits, spaces or hyphens")
        String licensePlate,
        @NotNull VehicleType vehicleType,
        AllocationStrategyType strategy) {
}
