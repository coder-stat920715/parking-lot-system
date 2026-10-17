package com.parking.spot.service;

import com.parking.spot.domain.ParkingSpot;
import com.parking.spot.dto.AllocationRequest;
import com.parking.spot.dto.AllocationResponse;
import com.parking.spot.dto.ReleaseResponse;
import com.parking.spot.exception.LockAcquisitionException;
import com.parking.spot.exception.NoSpotAvailableException;
import com.parking.spot.lock.SpotLockService;
import com.parking.spot.mapper.SpotMapper;
import com.parking.spot.strategy.AllocationStrategyFactory;
import com.parking.spot.strategy.SpotAllocationStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * Double-booking protection, in two layers:
 *  1. Distributed lock (Redis; JVM-local fallback) serialises allocations per vehicle type across ALL instances.
 *  2. @Version optimistic locking on ParkingSpot catches whatever slips through (cross vehicle-type overlap,
 *     lock lease expiry, Redis outage). The loser retries and selects another spot.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpotAllocationService {

    private static final int MAX_ATTEMPTS = 3;
    private static final Duration LOCK_WAIT = Duration.ofSeconds(3);
    private static final Duration LOCK_LEASE = Duration.ofSeconds(5);

    private final SpotReservationExecutor executor;
    private final SpotLockService lockService;
    private final AllocationStrategyFactory strategyFactory;
    private final SpotMapper mapper;

    public AllocationResponse allocate(AllocationRequest request) {
        SpotAllocationStrategy strategy = strategyFactory.get(request.strategy());
        String lockKey = "allocation:" + request.vehicleType();

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                Optional<ParkingSpot> reserved = lockService.executeWithLock(
                        lockKey, LOCK_WAIT, LOCK_LEASE, () -> executor.reserve(request, strategy));
                ParkingSpot spot = reserved.orElseThrow(() -> new NoSpotAvailableException(
                        "No available spot for vehicle type " + request.vehicleType()));
                log.info("Allocated spot {} to ticket {} (strategy={}, attempt={})",
                        spot.getSpotCode(), request.ticketNumber(), strategy.type(), attempt);
                return mapper.toAllocationResponse(spot, strategy.type());
            } catch (OptimisticLockingFailureException e) {
                log.warn("Optimistic lock conflict allocating for ticket {} (attempt {}/{}); retrying",
                        request.ticketNumber(), attempt, MAX_ATTEMPTS);
                backoff(attempt);
            }
        }
        throw new LockAcquisitionException("Allocation contention too high, please retry");
    }

    public ReleaseResponse release(String ticketNumber) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                Optional<ParkingSpot> released = executor.release(ticketNumber);
                released.ifPresentOrElse(
                        s -> log.info("Released spot {} (ticket {})", s.getSpotCode(), ticketNumber),
                        () -> log.info("Release for ticket {}: nothing to release (already free)", ticketNumber));
                return mapper.toReleaseResponse(ticketNumber, released.orElse(null));
            } catch (OptimisticLockingFailureException e) {
                backoff(attempt);
            }
        }
        throw new LockAcquisitionException("Release contention too high, please retry");
    }

    private void backoff(int attempt) {
        try {
            Thread.sleep(25L * attempt);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
