package com.parking.payment.client;

import java.math.BigDecimal;

/** The subset of the ticket-service TicketResponse this service needs. */
public record TicketView(String ticketNumber, String licensePlate, String vehicleType, String status,
                         BigDecimal feeAmount, String currency) {
}
