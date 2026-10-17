package com.parking.spot.service;

import com.parking.spot.domain.SpotType;
import com.parking.spot.domain.VehicleType;
import com.parking.spot.dto.AvailabilityEntry;
import com.parking.spot.dto.AvailabilityResponse;
import com.parking.spot.mapper.SpotMapper;
import com.parking.spot.repository.ParkingSpotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SpotAvailabilityService {

    private final ParkingSpotRepository repository;
    private final SpotMapper mapper;

    @Transactional(readOnly = true)
    public AvailabilityResponse getAvailability(SpotType spotType, Integer floor, VehicleType vehicleType) {
        List<AvailabilityEntry> entries = repository.summarize().stream()
                .filter(r -> spotType == null || r.getSpotType() == spotType)
                .filter(r -> floor == null || floor.equals(r.getFloorNumber()))
                .filter(r -> vehicleType == null || vehicleType.compatibleSpotTypes().contains(r.getSpotType()))
                .map(mapper::toEntry)
                .toList();
        long totalAvailable = entries.stream().mapToLong(AvailabilityEntry::available).sum();
        return new AvailabilityResponse(Instant.now(), mapper.instanceId(), totalAvailable, entries);
    }
}
