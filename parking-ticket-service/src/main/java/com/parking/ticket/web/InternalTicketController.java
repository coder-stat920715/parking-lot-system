package com.parking.ticket.web;

import com.parking.ticket.dto.MarkPaidRequest;
import com.parking.ticket.dto.TicketResponse;
import com.parking.ticket.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** Internal API for payment-service (Feign). NOT routed by the API gateway. */
@RestController
@RequestMapping("/internal/v1/tickets")
@RequiredArgsConstructor
public class InternalTicketController {

    private final TicketService ticketService;

    @PostMapping("/{ticketNumber}/paid")
    public TicketResponse markPaid(@PathVariable String ticketNumber, @Valid @RequestBody MarkPaidRequest request) {
        return ticketService.markPaid(ticketNumber, request);
    }
}
