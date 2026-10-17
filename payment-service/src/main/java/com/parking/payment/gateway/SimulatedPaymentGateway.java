package com.parking.payment.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Always approves after a short delay, except for the token "tok_decline" (simulates an issuer decline). */
@Slf4j
@Component
public class SimulatedPaymentGateway implements PaymentGateway {

    public static final String DECLINE_TOKEN = "tok_decline";

    @Override
    public GatewayChargeResult charge(GatewayChargeRequest request) {
        try {
            Thread.sleep(150);   // simulated network latency to the PSP
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return GatewayChargeResult.declined("Interrupted");
        }
        if (DECLINE_TOKEN.equalsIgnoreCase(request.paymentToken())) {
            log.info("Simulated gateway DECLINED {} {}", request.amount(), request.currency());
            return GatewayChargeResult.declined("Card declined by issuer");
        }
        String txId = "SIM-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        log.info("Simulated gateway APPROVED {} {} -> {}", request.amount(), request.currency(), txId);
        return GatewayChargeResult.approved(txId);
    }
}
