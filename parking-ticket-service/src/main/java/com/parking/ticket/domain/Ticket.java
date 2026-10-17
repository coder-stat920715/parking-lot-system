package com.parking.ticket.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "tickets", indexes = @Index(name = "idx_ticket_status", columnList = "status"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_number", nullable = false, unique = true, length = 40)
    private String ticketNumber;

    @ManyToOne(optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Column(name = "spot_code", nullable = false, length = 32)
    private String spotCode;

    @Column(name = "floor_number")
    private Integer floorNumber;

    @Column(name = "spot_type", length = 16)
    private String spotType;

    @Column(name = "entry_time", nullable = false)
    private Instant entryTime;

    @Column(name = "exit_time")
    private Instant exitTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private TicketStatus status;

    @Column(name = "fee_amount", precision = 12, scale = 2)
    private BigDecimal feeAmount;

    @Column(length = 3)
    private String currency;

    @Column(name = "payment_reference", length = 40)
    private String paymentReference;

    @Column(name = "paid_at")
    private Instant paidAt;

    /** False until the spot-allocation-service confirmed the release; a scheduled job retries failures. */
    @Column(name = "spot_released", nullable = false)
    private boolean spotReleased;

    /**
     * Set to the license plate while the ticket is open (ACTIVE / AWAITING_PAYMENT), NULL once PAID.
     * The UNIQUE constraint therefore guarantees "one open ticket per vehicle" at database level even under
     * concurrent entry requests and across multiple instances (NULLs do not collide).
     */
    @Column(name = "open_vehicle_key", unique = true, length = 24)
    private String openVehicleKey;

    @Version
    private Long version;
}
