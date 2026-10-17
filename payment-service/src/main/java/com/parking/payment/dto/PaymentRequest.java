package com.parking.payment.dto;

import com.parking.payment.domain.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * The amount is intentionally NOT part of the request: the server takes it from the ticket (source of truth).
 * paymentToken is the tokenised card/UPI credential; the simulated gateway declines the token "tok_decline".
 */
public record PaymentRequest(
        @NotBlank String ticketNumber,
        @NotNull PaymentMethod method,
        String paymentToken) {
}
