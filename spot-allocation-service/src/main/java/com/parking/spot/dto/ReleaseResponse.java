package com.parking.spot.dto;

public record ReleaseResponse(String ticketNumber, boolean released, String spotCode, String servedBy) {
}
