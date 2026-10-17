package com.parking.ticket.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "vehicles", uniqueConstraints = @UniqueConstraint(columnNames = "license_plate"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "license_plate", nullable = false, length = 20)
    private String licensePlate;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", nullable = false, length = 16)
    private VehicleType vehicleType;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
