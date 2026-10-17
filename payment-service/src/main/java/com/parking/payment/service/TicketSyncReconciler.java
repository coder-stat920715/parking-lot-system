package com.parking.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TicketSyncReconciler {

    private final PaymentService paymentService;

    @Scheduled(fixedDelayString = "${parking.reconcile.interval-ms:30000}",
            initialDelayString = "${parking.reconcile.initial-delay-ms:30000}")
    public void run() {
        int synced = paymentService.reconcileTicketSync();
        if (synced > 0) {
            log.info("Reconciler synced {} payment(s) to the ticket service", synced);
        }
    }
}
