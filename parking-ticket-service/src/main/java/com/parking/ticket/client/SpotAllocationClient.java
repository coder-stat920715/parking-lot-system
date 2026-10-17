package com.parking.ticket.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * "SPOT-ALLOCATION-SERVICE" is the Eureka service id, not a hostname: Spring Cloud LoadBalancer resolves it to the
 * list of live instances and round-robins across them on every call.
 */
@FeignClient(name = "SPOT-ALLOCATION-SERVICE", contextId = "spotAllocationClient")
public interface SpotAllocationClient {

    @PostMapping("/internal/v1/spots/allocate")
    SpotAllocationResponse allocate(@RequestBody SpotAllocationRequest request);

    @PostMapping("/internal/v1/spots/release")
    SpotReleaseResponse release(@RequestBody SpotReleaseRequest request);
}
