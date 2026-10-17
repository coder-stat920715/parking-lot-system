package com.parking.payment.gateway;

import com.parking.payment.domain.PaymentMethod;

import java.math.BigDecimal;

public record GatewayChargeRequest(String paymentReference, BigDecimal amount, String currency,
                                   PaymentMethod method, String paymentToken) {
}
