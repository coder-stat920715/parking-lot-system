package com.parking.spot.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ParkingSpotTest {

    @Test
    void cannotReserveTwice() {
        ParkingSpot spot = ParkingSpot.builder().spotCode("F0-M01").spotType(SpotType.MEDIUM).build();
        spot.reserve("T1");

        assertThat(spot.getStatus()).isEqualTo(SpotStatus.OCCUPIED);
        assertThatThrownBy(() -> spot.reserve("T2")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void releaseMakesSpotAvailableAgain() {
        ParkingSpot spot = ParkingSpot.builder().spotCode("F0-M01").spotType(SpotType.MEDIUM).build();
        spot.reserve("T1");
        spot.release();

        assertThat(spot.getStatus()).isEqualTo(SpotStatus.AVAILABLE);
        assertThat(spot.getAssignedTicketNumber()).isNull();
    }
}
