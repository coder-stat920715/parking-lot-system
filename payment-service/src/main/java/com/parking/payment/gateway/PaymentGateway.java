package com.parking.payment.gateway;

/** Port to an external PSP (Stripe, Razorpay, ...). Swap the implementation without touching the service. */
public interface PaymentGateway {

    GatewayChargeResult charge(GatewayChargeRequest request);
}
