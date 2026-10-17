package com.parking.ticket.dto;

import jakarta.validation.constraints.NotBlank;

public record MarkPaidRequest(@NotBlank String paymentReference) {
}
