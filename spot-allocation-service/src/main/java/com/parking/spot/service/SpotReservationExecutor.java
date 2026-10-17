package com.parking.spot.service;

import com.parking.spot.domain.ParkingSpot;
import com.parking.spot.domain.SpotStatus;
import com.parking.spot.dto.AllocationRequest;
import com.parking.spot.repository.ParkingSpotRepository;
import com.parking.spot.strategy.SpotAllocationStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Short DB transactions, kept in a separate bean so that the lock in SpotAllocationService is acquired BEFORE the
 * transaction starts and released AFTER it commits (otherwise another thread could read stale data).
 */
@Component
@RequiredArgsConstructor
public class SpotReservationExecutor {

    private final ParkingSpotRepository repository;

    @Transactional
    public Optional<ParkingSpot> reserve(AllocationRequest request, SpotAllocationStrategy strategy) {
        // Idempotency: a retried request for the same ticket gets the same spot.
        Optional<ParkingSpot> existing = repository.findByAssignedTicketNumber(request.ticketNumber());
        if (existing.isPresent()) {
            return existing;
        }
        List<ParkingSpot> candidates = repository.findBySpotTypeInAndStatus(
                request.vehicleType().compatibleSpotTypes(), SpotStatus.AVAILABLE);

        Optional<ParkingSpot> chosen = strategy.select(candidates, request.vehicleType());
        chosen.ifPresent(spot -> {
            spot.reserve(request.ticketNumber());
            repository.saveAndFlush(spot);   // flush now so a @Version conflict surfaces inside this method
        });
        return chosen;
    }

    @Transactional
    public Optional<ParkingSpot> release(String ticketNumber) {
        return repository.findByAssignedTicketNumber(ticketNumber).map(spot -> {
            spot.release();
            return repository.saveAndFlush(spot);
        });
    }
}
