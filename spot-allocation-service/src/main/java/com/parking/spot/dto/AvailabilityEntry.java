package com.parking.spot.dto;

import com.parking.spot.domain.SpotType;

public record AvailabilityEntry(int floorNumber, SpotType spotType, long total, long available) {
}
