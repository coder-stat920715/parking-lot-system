package com.parking.spot.dto;

import jakarta.validation.constraints.NotBlank;

public record ReleaseRequest(@NotBlank String ticketNumber) {
}
