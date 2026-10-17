package com.parking.ticket.dto;

import com.parking.ticket.domain.TicketStatus;
import com.parking.ticket.domain.VehicleType;

import java.math.BigDecimal;
import java.time.Instant;

public record ExitResponse(
        String ticketNumber,
        String licensePlate,
        VehicleType vehicleType,
        Instant entryTime,
        Instant exitTime,
        long durationMinutes,
        BigDecimal feeAmount,
        String currency,
        TicketStatus status,
        String message) {
}
