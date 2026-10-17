package com.parking.spot.web;

import com.parking.spot.domain.SpotType;
import com.parking.spot.domain.VehicleType;
import com.parking.spot.dto.AvailabilityResponse;
import com.parking.spot.service.SpotAvailabilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Public API (exposed through the gateway). */
@RestController
@RequestMapping("/api/v1/spots")
@RequiredArgsConstructor
public class SpotAvailabilityController {

    private final SpotAvailabilityService service;

    @GetMapping("/available")
    public AvailabilityResponse available(@RequestParam(required = false) SpotType spotType,
                                          @RequestParam(required = false) Integer floor,
                                          @RequestParam(required = false) VehicleType vehicleType) {
        return service.getAvailability(spotType, floor, vehicleType);
    }
}
