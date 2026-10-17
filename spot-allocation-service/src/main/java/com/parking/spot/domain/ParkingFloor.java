package com.parking.spot.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "parking_floors", uniqueConstraints = @UniqueConstraint(columnNames = "floor_number"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParkingFloor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "floor_number", nullable = false)
    private Integer floorNumber;

    @Column(nullable = false, length = 64)
    private String name;
}
