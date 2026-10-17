package com.parking.payment.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payments", indexes = @Index(name = "idx_payment_ticket", columnList = "ticket_number"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_reference", nullable = false, unique = true, length = 40)
    private String paymentReference;

    @Column(name = "ticket_number", nullable = false, length = 40)
    private String ticketNumber;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PaymentStatus status;

    @Column(name = "gateway_transaction_id", length = 64)
    private String gatewayTransactionId;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    /**
     * Equals the ticket number while the payment is PENDING or SUCCESS and NULL once FAILED. The UNIQUE constraint
     * guarantees at most one in-flight/successful payment per ticket, even for concurrent duplicate requests
     * (prevents charging a customer twice).
     */
    @Column(name = "active_ticket_key", unique = true, length = 40)
    private String activeTicketKey;

    /** False if the ticket-service could not be told yet; a scheduled job keeps retrying. */
    @Column(name = "ticket_synced", nullable = false)
    private boolean ticketSynced;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
