package com.parking.ticket.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/** simulatedDurationMinutes is a demo-only switch (see parking.demo.allow-simulated-duration). */
public record ExitRequest(
        @NotBlank String ticketNumber,
        @Min(1) Long simulatedDurationMinutes) {
}
