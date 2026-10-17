package com.parking.spot.web;

import com.parking.spot.dto.AllocationRequest;
import com.parking.spot.dto.AllocationResponse;
import com.parking.spot.dto.ReleaseRequest;
import com.parking.spot.dto.ReleaseResponse;
import com.parking.spot.service.SpotAllocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Internal API: called by parking-ticket-service via Feign. NOT routed by the API gateway. */
@RestController
@RequestMapping("/internal/v1/spots")
@RequiredArgsConstructor
public class SpotAllocationController {

    private final SpotAllocationService service;

    @PostMapping("/allocate")
    public ResponseEntity<AllocationResponse> allocate(@Valid @RequestBody AllocationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.allocate(request));
    }

    @PostMapping("/release")
    public ReleaseResponse release(@Valid @RequestBody ReleaseRequest request) {
        return service.release(request.ticketNumber());
    }
}
