package com.parking.payment.dto;

import com.parking.payment.domain.PaymentMethod;
import com.parking.payment.domain.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(
        String paymentReference,
        String ticketNumber,
        BigDecimal amount,
        String currency,
        PaymentMethod method,
        PaymentStatus status,
        String gatewayTransactionId,
        String failureReason,
        boolean ticketStatusSynced,
        Instant createdAt) {
}
