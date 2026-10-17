package com.parking.spot.dto;

import java.time.Instant;
import java.util.List;

public record AvailabilityResponse(Instant timestamp, String servedBy, long totalAvailable,
                                   List<AvailabilityEntry> entries) {
}
