package com.parking.ticket.web;

import com.parking.ticket.dto.*;
import com.parking.ticket.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Public API (exposed through the gateway). */
@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @PostMapping("/entry")
    public ResponseEntity<EntryResponse> entry(@Valid @RequestBody EntryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ticketService.entry(request));
    }

    @PostMapping("/exit")
    public ExitResponse exit(@Valid @RequestBody ExitRequest request) {
        return ticketService.exit(request);
    }

    @GetMapping("/{ticketNumber}")
    public TicketResponse get(@PathVariable String ticketNumber) {
        return ticketService.get(ticketNumber);
    }
}
