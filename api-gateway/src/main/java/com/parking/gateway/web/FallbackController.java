package com.parking.gateway.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/** Target of the gateway CircuitBreaker filters (forward:/fallback/{service}); handles every HTTP method. */
@RestController
public class FallbackController {

    @RequestMapping("/fallback/{service}")
    public ResponseEntity<Map<String, Object>> fallback(@PathVariable String service) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        body.put("error", "Service Unavailable");
        body.put("message", service + " is currently unavailable or too slow. Please retry shortly.");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }
}
