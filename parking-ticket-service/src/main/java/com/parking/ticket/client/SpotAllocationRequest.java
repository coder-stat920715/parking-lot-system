package com.parking.ticket.client;

import com.parking.ticket.domain.AllocationStrategyType;
import com.parking.ticket.domain.VehicleType;

public record SpotAllocationRequest(String ticketNumber, VehicleType vehicleType, AllocationStrategyType strategy) {
}
