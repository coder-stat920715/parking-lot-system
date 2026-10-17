package com.parking.payment.client;

import com.parking.payment.exception.ServiceUnavailableException;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Resilience wrapper (Retry( CircuitBreaker( call ) )) - see SpotAllocationGateway in ticket-service for details. */
@Slf4j
@Component
@RequiredArgsConstructor
public class TicketGateway {

    private static final String RESILIENCE_NAME = "ticketService";

    private final TicketClient client;

    @Retry(name = RESILIENCE_NAME, fallbackMethod = "getTicketFallback")
    @CircuitBreaker(name = RESILIENCE_NAME)
    public TicketView getTicket(String ticketNumber) {
        return client.getTicket(ticketNumber);
    }

    @Retry(name = RESILIENCE_NAME, fallbackMethod = "markPaidFallback")
    @CircuitBreaker(name = RESILIENCE_NAME)
    public TicketView markPaid(String ticketNumber, String paymentReference) {
        return client.markPaid(ticketNumber, new MarkPaidRequest(paymentReference));
    }

    TicketView getTicketFallback(String ticketNumber, CallNotPermittedException ex) {
        throw new ServiceUnavailableException("Ticket service is temporarily unavailable (circuit open).");
    }

    TicketView getTicketFallback(String ticketNumber, FeignException ex) {
        log.error("Ticket lookup failed for {}: {}", ticketNumber, ex.getMessage());
        throw new ServiceUnavailableException("Ticket service did not respond. Retry shortly.");
    }

    TicketView markPaidFallback(String ticketNumber, String paymentReference, CallNotPermittedException ex) {
        throw new ServiceUnavailableException("Ticket service is temporarily unavailable (circuit open).");
    }

    TicketView markPaidFallback(String ticketNumber, String paymentReference, FeignException ex) {
        throw new ServiceUnavailableException("Ticket service did not respond: " + ex.getMessage());
    }
}
