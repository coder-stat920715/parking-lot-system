package com.parking.ticket.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SpotReleaseReconciler {

    private final TicketService ticketService;

    @Scheduled(fixedDelayString = "${parking.reconcile.interval-ms:30000}",
            initialDelayString = "${parking.reconcile.initial-delay-ms:30000}")
    public void run() {
        int released = ticketService.retryPendingSpotReleases();
        if (released > 0) {
            log.info("Reconciler released {} previously pending spot(s)", released);
        }
    }
}
