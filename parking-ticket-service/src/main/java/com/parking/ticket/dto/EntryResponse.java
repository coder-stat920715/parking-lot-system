package com.parking.ticket.dto;

import com.parking.ticket.domain.TicketStatus;
import com.parking.ticket.domain.VehicleType;

import java.time.Instant;

public record EntryResponse(
        String ticketNumber,
        String licensePlate,
        VehicleType vehicleType,
        String spotCode,
        Integer floorNumber,
        String spotType,
        Instant entryTime,
        TicketStatus status,
        String spotServiceInstance) {
}
