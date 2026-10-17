package com.parking.spot.repository;

import com.parking.spot.domain.SpotType;

/** Interface projection for the availability aggregate query. */
public interface AvailabilityRow {
    Integer getFloorNumber();
    SpotType getSpotType();
    Long getTotal();
    Long getAvailable();
}
