package com.parking.payment.gateway;

public record GatewayChargeResult(boolean approved, String transactionId, String declineReason) {

    public static GatewayChargeResult approved(String transactionId) {
        return new GatewayChargeResult(true, transactionId, null);
    }

    public static GatewayChargeResult declined(String reason) {
        return new GatewayChargeResult(false, null, reason);
    }
}
