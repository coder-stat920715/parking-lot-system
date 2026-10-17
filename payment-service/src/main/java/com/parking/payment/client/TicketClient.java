package com.parking.payment.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/** Eureka service id of the ticket service; resolved and load-balanced by Spring Cloud LoadBalancer. */
@FeignClient(name = "PARKING-TICKET-SERVICE", contextId = "ticketClient")
public interface TicketClient {

    @GetMapping("/api/v1/tickets/{ticketNumber}")
    TicketView getTicket(@PathVariable("ticketNumber") String ticketNumber);

    @PostMapping("/internal/v1/tickets/{ticketNumber}/paid")
    TicketView markPaid(@PathVariable("ticketNumber") String ticketNumber, @RequestBody MarkPaidRequest request);
}
