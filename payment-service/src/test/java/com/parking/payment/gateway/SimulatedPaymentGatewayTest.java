package com.parking.payment.gateway;

import com.parking.payment.domain.PaymentMethod;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class SimulatedPaymentGatewayTest {

    private final SimulatedPaymentGateway gateway = new SimulatedPaymentGateway();

    private GatewayChargeRequest request(String token) {
        return new GatewayChargeRequest("PAY-1", new BigDecimal("40.00"), "INR", PaymentMethod.CARD, token);
    }

    @Test
    void approvesNormalTokens() {
        GatewayChargeResult result = gateway.charge(request("tok_ok"));
        assertThat(result.approved()).isTrue();
        assertThat(result.transactionId()).startsWith("SIM-");
    }

    @Test
    void declinesTheDeclineToken() {
        GatewayChargeResult result = gateway.charge(request("tok_decline"));
        assertThat(result.approved()).isFalse();
        assertThat(result.declineReason()).isNotBlank();
    }
}
