package com.parking.ticket.client;

public record SpotAllocationResponse(
        String ticketNumber,
        String spotCode,
        int floorNumber,
        String spotType,
        int distanceFromEntrance,
        String strategyUsed,
        String servedBy) {
}
