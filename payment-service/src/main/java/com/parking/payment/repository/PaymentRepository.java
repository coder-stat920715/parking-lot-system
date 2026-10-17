package com.parking.payment.repository;

import com.parking.payment.domain.Payment;
import com.parking.payment.domain.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByPaymentReference(String paymentReference);

    Optional<Payment> findFirstByTicketNumberAndStatus(String ticketNumber, PaymentStatus status);

    List<Payment> findByStatusAndTicketSyncedFalse(PaymentStatus status);
}
