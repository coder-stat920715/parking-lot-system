package com.parking.ticket.client;

public record SpotReleaseResponse(String ticketNumber, boolean released, String spotCode, String servedBy) {
}
