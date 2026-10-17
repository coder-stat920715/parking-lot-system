package com.parking.spot.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "parking_spots",
        uniqueConstraints = @UniqueConstraint(columnNames = "spot_code"),
        indexes = @Index(name = "idx_spot_lookup", columnList = "spot_type,status,distance_from_entrance"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParkingSpot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "spot_code", nullable = false, length = 32)
    private String spotCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "floor_id", nullable = false)
    private ParkingFloor floor;

    @Enumerated(EnumType.STRING)
    @Column(name = "spot_type", nullable = false, length = 16)
    private SpotType spotType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    @Builder.Default
    private SpotStatus status = SpotStatus.AVAILABLE;

    @Column(name = "distance_from_entrance", nullable = false)
    private int distanceFromEntrance;

    @Column(name = "assigned_ticket_number", length = 64)
    private String assignedTicketNumber;

    @Column(name = "occupied_since")
    private Instant occupiedSince;

    /**
     * Optimistic lock. If two transactions (possibly on different service instances) read the same AVAILABLE spot
     * and both try to reserve it, only the first UPDATE ... WHERE version=? succeeds; the loser gets an
     * OptimisticLockingFailureException and re-selects. This is the last line of defence against double booking.
     */
    @Version
    private Long version;

    public void reserve(String ticketNumber) {
        if (status != SpotStatus.AVAILABLE) {
            throw new IllegalStateException("Spot " + spotCode + " is not available (" + status + ")");
        }
        this.status = SpotStatus.OCCUPIED;
        this.assignedTicketNumber = ticketNumber;
        this.occupiedSince = Instant.now();
    }

    public void release() {
        this.status = SpotStatus.AVAILABLE;
        this.assignedTicketNumber = null;
        this.occupiedSince = null;
    }

    public int getFloorNumber() {
        return floor.getFloorNumber();
    }
}
