package com.parking.ticket.client;

import com.parking.ticket.exception.ServiceUnavailableException;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Resilience wrapper around the Feign client. Aspect order is Retry( CircuitBreaker( call ) ):
 *  - FeignException (5xx / connection refused / no instance) is retried; LoadBalancer picks the next instance,
 *    which gives transparent fail-over while Eureka has not yet evicted a dead instance.
 *  - Repeated failures open the circuit; further calls fail fast with CallNotPermittedException.
 *  - Business errors (409 no spot, 404, 400) arrive as ConflictException etc. - not retried, not recorded.
 * The fallback lives on @Retry (the outermost aspect) so that it only runs after all retries are exhausted.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SpotAllocationGateway {

    private static final String RESILIENCE_NAME = "spotAllocation";

    private final SpotAllocationClient client;

    @Retry(name = RESILIENCE_NAME, fallbackMethod = "allocateFallback")
    @CircuitBreaker(name = RESILIENCE_NAME)
    public SpotAllocationResponse allocate(SpotAllocationRequest request) {
        return client.allocate(request);
    }

    @Retry(name = RESILIENCE_NAME, fallbackMethod = "releaseFallback")
    @CircuitBreaker(name = RESILIENCE_NAME)
    public SpotReleaseResponse release(String ticketNumber) {
        return client.release(new SpotReleaseRequest(ticketNumber));
    }

    // ---- fallbacks (one per exception family; anything else, e.g. business exceptions, is re-thrown as is) ----

    SpotAllocationResponse allocateFallback(SpotAllocationRequest request, CallNotPermittedException ex) {
        log.error("Circuit OPEN for spot allocation (ticket {})", request.ticketNumber());
        throw new ServiceUnavailableException("Spot allocation is temporarily unavailable (circuit open). Retry shortly.");
    }

    SpotAllocationResponse allocateFallback(SpotAllocationRequest request, FeignException ex) {
        log.error("Spot allocation failed for ticket {}: {}", request.ticketNumber(), ex.getMessage());
        throw new ServiceUnavailableException("Spot allocation service did not respond. Retry shortly.");
    }

    SpotReleaseResponse releaseFallback(String ticketNumber, CallNotPermittedException ex) {
        throw new ServiceUnavailableException("Spot release is temporarily unavailable (circuit open).");
    }

    SpotReleaseResponse releaseFallback(String ticketNumber, FeignException ex) {
        throw new ServiceUnavailableException("Spot release failed: " + ex.getMessage());
    }
}
