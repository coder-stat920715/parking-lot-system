package com.parking.ticket.dto;

import com.parking.ticket.domain.TicketStatus;
import com.parking.ticket.domain.VehicleType;

import java.math.BigDecimal;
import java.time.Instant;

public record TicketResponse(
        String ticketNumber,
        String licensePlate,
        VehicleType vehicleType,
        String spotCode,
        Integer floorNumber,
        String spotType,
        Instant entryTime,
        Instant exitTime,
        Long durationMinutes,
        BigDecimal feeAmount,
        String currency,
        TicketStatus status,
        String paymentReference,
        Instant paidAt,
        boolean spotReleased) {
}
