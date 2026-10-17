package com.parking.payment.mapper;

import com.parking.payment.domain.Payment;
import com.parking.payment.dto.PaymentResponse;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    public PaymentResponse toResponse(Payment p) {
        return new PaymentResponse(p.getPaymentReference(), p.getTicketNumber(), p.getAmount(), p.getCurrency(),
                p.getMethod(), p.getStatus(), p.getGatewayTransactionId(), p.getFailureReason(), p.isTicketSynced(),
                p.getCreatedAt());
    }
}
