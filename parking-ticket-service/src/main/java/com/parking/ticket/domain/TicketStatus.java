package com.parking.ticket.domain;

/** ACTIVE -> (exit) AWAITING_PAYMENT -> (payment) PAID. CANCELLED is reserved for operator actions. */
public enum TicketStatus { ACTIVE, AWAITING_PAYMENT, PAID, CANCELLED }
