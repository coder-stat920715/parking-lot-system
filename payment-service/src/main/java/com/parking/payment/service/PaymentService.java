package com.parking.payment.service;

import com.parking.payment.client.TicketGateway;
import com.parking.payment.client.TicketView;
import com.parking.payment.domain.Payment;
import com.parking.payment.domain.PaymentStatus;
import com.parking.payment.dto.PaymentRequest;
import com.parking.payment.dto.PaymentResponse;
import com.parking.payment.exception.ConflictException;
import com.parking.payment.exception.ResourceNotFoundException;
import com.parking.payment.gateway.GatewayChargeRequest;
import com.parking.payment.gateway.GatewayChargeResult;
import com.parking.payment.gateway.PaymentGateway;
import com.parking.payment.mapper.PaymentMapper;
import com.parking.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Payment workflow:
 *  1. fetch the ticket (authoritative fee) and verify it is AWAITING_PAYMENT
 *  2. claim the ticket by inserting a PENDING payment (unique active_ticket_key => no double charge)
 *  3. charge through the PaymentGateway port
 *  4. on success tell ticket-service to mark the ticket PAID (which frees the spot); on failure free the claim
 * No remote call happens inside a DB transaction. Step 4 failures are healed by TicketSyncReconciler.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final String AWAITING_PAYMENT = "AWAITING_PAYMENT";
    private static final String PAID = "PAID";

    private final PaymentRepository paymentRepository;
    private final TicketGateway ticketGateway;
    private final PaymentGateway paymentGateway;
    private final PaymentMapper mapper;

    public PaymentResponse pay(PaymentRequest request) {
        TicketView ticket = ticketGateway.getTicket(request.ticketNumber());

        // Idempotent replay: already paid successfully -> return the existing payment (and heal sync if needed)
        Optional<Payment> done = paymentRepository.findFirstByTicketNumberAndStatus(
                ticket.ticketNumber(), PaymentStatus.SUCCESS);
        if (done.isPresent()) {
            return mapper.toResponse(syncTicket(done.get()));
        }
        if (PAID.equals(ticket.status())) {
            throw new ConflictException("Ticket " + ticket.ticketNumber() + " is already paid");
        }
        if (!AWAITING_PAYMENT.equals(ticket.status())) {
            throw new ConflictException("Ticket " + ticket.ticketNumber() + " is " + ticket.status()
                    + "; the vehicle must exit first (POST /api/v1/tickets/exit)");
        }

        Payment payment = Payment.builder()
                .paymentReference("PAY-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase())
                .ticketNumber(ticket.ticketNumber())
                .amount(ticket.feeAmount())
                .currency(ticket.currency())
                .method(request.method())
                .status(PaymentStatus.PENDING)
                .activeTicketKey(ticket.ticketNumber())
                .ticketSynced(false)
                .createdAt(Instant.now())
                .build();
        try {
            payment = paymentRepository.saveAndFlush(payment);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("A payment for ticket " + ticket.ticketNumber() + " is already in progress");
        }

        GatewayChargeResult result;
        try {
            result = paymentGateway.charge(new GatewayChargeRequest(payment.getPaymentReference(),
                    payment.getAmount(), payment.getCurrency(), payment.getMethod(), request.paymentToken()));
        } catch (RuntimeException e) {
            log.error("Gateway error for payment {}", payment.getPaymentReference(), e);
            result = GatewayChargeResult.declined("Payment gateway error");
        }

        if (result.approved()) {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setGatewayTransactionId(result.transactionId());
            payment = paymentRepository.save(payment);
            payment = syncTicket(payment);
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(result.declineReason());
            payment.setActiveTicketKey(null);             // release the claim so the customer can retry
            payment = paymentRepository.save(payment);
        }
        return mapper.toResponse(payment);
    }

    public PaymentResponse get(String paymentReference) {
        return paymentRepository.findByPaymentReference(paymentReference)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + paymentReference));
    }

    /** Retries notifying the ticket service for successful payments whose notification failed. */
    public int reconcileTicketSync() {
        int synced = 0;
        for (Payment p : paymentRepository.findByStatusAndTicketSyncedFalse(PaymentStatus.SUCCESS)) {
            if (syncTicket(p).isTicketSynced()) {
                synced++;
            }
        }
        return synced;
    }

    private Payment syncTicket(Payment payment) {
        if (payment.isTicketSynced()) {
            return payment;
        }
        try {
            ticketGateway.markPaid(payment.getTicketNumber(), payment.getPaymentReference());
            payment.setTicketSynced(true);
            return paymentRepository.save(payment);
        } catch (RuntimeException e) {
            log.warn("Payment {} succeeded but ticket {} could not be marked paid yet (will retry): {}",
                    payment.getPaymentReference(), payment.getTicketNumber(), e.getMessage());
            return payment;
        }
    }
}
