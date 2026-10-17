package com.parking.ticket.service;

import com.parking.ticket.client.SpotAllocationGateway;
import com.parking.ticket.client.SpotAllocationRequest;
import com.parking.ticket.client.SpotAllocationResponse;
import com.parking.ticket.domain.Ticket;
import com.parking.ticket.domain.TicketStatus;
import com.parking.ticket.domain.Vehicle;
import com.parking.ticket.domain.VehicleType;
import com.parking.ticket.dto.*;
import com.parking.ticket.exception.BadRequestException;
import com.parking.ticket.exception.ConflictException;
import com.parking.ticket.exception.ResourceNotFoundException;
import com.parking.ticket.fee.FeeCalculator;
import com.parking.ticket.mapper.TicketMapper;
import com.parking.ticket.repository.TicketRepository;
import com.parking.ticket.repository.VehicleRepository;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Orchestrates entry / exit. Remote calls (Feign) are deliberately NOT made inside DB transactions: every
 * repository call is its own short transaction, so a slow downstream never holds a database connection open.
 * Entities are detached between calls, hence every save() result is re-assigned (it carries the new @Version).
 */
@Slf4j
@Service
public class TicketService {

    private static final List<TicketStatus> OPEN_STATUSES = List.of(TicketStatus.ACTIVE, TicketStatus.AWAITING_PAYMENT);
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final TicketRepository ticketRepository;
    private final VehicleRepository vehicleRepository;
    private final SpotAllocationGateway spotGateway;
    private final FeeCalculator feeCalculator;
    private final TicketMapper mapper;
    private final boolean allowSimulatedDuration;

    public TicketService(TicketRepository ticketRepository, VehicleRepository vehicleRepository,
                         SpotAllocationGateway spotGateway, FeeCalculator feeCalculator, TicketMapper mapper,
                         @Value("${parking.demo.allow-simulated-duration:false}") boolean allowSimulatedDuration) {
        this.ticketRepository = ticketRepository;
        this.vehicleRepository = vehicleRepository;
        this.spotGateway = spotGateway;
        this.feeCalculator = feeCalculator;
        this.mapper = mapper;
        this.allowSimulatedDuration = allowSimulatedDuration;
    }

    // ------------------------------------------------------------------------------------------ ENTRY

    @RateLimiter(name = "ticketEntry")
    public EntryResponse entry(EntryRequest request) {
        String plate = normalizePlate(request.licensePlate());

        if (ticketRepository.existsByVehicleLicensePlateAndStatusIn(plate, OPEN_STATUSES)) {
            throw new ConflictException("Vehicle " + plate + " already has an open ticket");
        }
        Vehicle vehicle = findOrCreateVehicle(plate, request.vehicleType());
        String ticketNumber = generateTicketNumber();

        // Load-balanced remote call (Feign -> LoadBalancer -> one of the spot-allocation instances)
        SpotAllocationResponse spot = spotGateway.allocate(
                new SpotAllocationRequest(ticketNumber, request.vehicleType(), request.strategy()));

        Ticket ticket = Ticket.builder()
                .ticketNumber(ticketNumber)
                .vehicle(vehicle)
                .spotCode(spot.spotCode())
                .floorNumber(spot.floorNumber())
                .spotType(spot.spotType())
                .entryTime(Instant.now())
                .status(TicketStatus.ACTIVE)
                .openVehicleKey(plate)
                .spotReleased(false)
                .build();
        try {
            ticket = ticketRepository.saveAndFlush(ticket);
        } catch (RuntimeException ex) {
            // Saga-style compensation: do not leak the reserved spot if persisting the ticket fails.
            log.error("Persisting ticket {} failed - releasing spot {}", ticketNumber, spot.spotCode(), ex);
            try {
                spotGateway.release(ticketNumber);
            } catch (RuntimeException releaseFailure) {
                log.error("Compensation release failed for ticket {} (spot {} may be leaked): {}",
                        ticketNumber, spot.spotCode(), releaseFailure.getMessage());
            }
            if (ex instanceof DataIntegrityViolationException) {
                throw new ConflictException("Vehicle " + plate + " already has an open ticket");
            }
            throw ex;
        }
        log.info("Entry: ticket {} plate {} -> spot {} (served by {})",
                ticketNumber, plate, spot.spotCode(), spot.servedBy());
        return mapper.toEntryResponse(ticket, spot);
    }

    // ------------------------------------------------------------------------------------------ EXIT

    public ExitResponse exit(ExitRequest request) {
        Ticket ticket = findEntity(request.ticketNumber());

        if (ticket.getStatus() == TicketStatus.AWAITING_PAYMENT) {
            return mapper.toExitResponse(ticket, "Fee already calculated - awaiting payment");   // idempotent
        }
        if (ticket.getStatus() != TicketStatus.ACTIVE) {
            throw new ConflictException("Ticket " + ticket.getTicketNumber() + " is " + ticket.getStatus());
        }

        Duration parkedFor;
        Instant exitTime;
        if (request.simulatedDurationMinutes() != null) {
            if (!allowSimulatedDuration) {
                throw new BadRequestException("simulatedDurationMinutes is disabled");
            }
            parkedFor = Duration.ofMinutes(request.simulatedDurationMinutes());
            exitTime = ticket.getEntryTime().plus(parkedFor);
        } else {
            exitTime = Instant.now();
            parkedFor = Duration.between(ticket.getEntryTime(), exitTime);
        }

        BigDecimal fee = feeCalculator.calculate(ticket.getVehicle().getVehicleType(), parkedFor);
        ticket.setExitTime(exitTime);
        ticket.setFeeAmount(fee);
        ticket.setCurrency(FeeCalculator.CURRENCY);
        ticket.setStatus(TicketStatus.AWAITING_PAYMENT);
        ticket = ticketRepository.save(ticket);     // @Version rejects a concurrent double-exit

        log.info("Exit: ticket {} parked {} min, fee {} {}", ticket.getTicketNumber(), parkedFor.toMinutes(),
                fee, FeeCalculator.CURRENCY);
        return mapper.toExitResponse(ticket, "Please pay the fee via POST /api/v1/payments");
    }

    // ------------------------------------------------------------------------------------------ QUERY / PAID

    public TicketResponse get(String ticketNumber) {
        return mapper.toResponse(findEntity(ticketNumber));
    }

    /** Called by payment-service (internal endpoint) after a successful charge. Idempotent. */
    public TicketResponse markPaid(String ticketNumber, MarkPaidRequest request) {
        Ticket ticket = findEntity(ticketNumber);
        if (ticket.getStatus() == TicketStatus.PAID) {
            return mapper.toResponse(ticket);
        }
        if (ticket.getStatus() != TicketStatus.AWAITING_PAYMENT) {
            throw new ConflictException("Ticket " + ticketNumber + " is " + ticket.getStatus()
                    + "; the vehicle must exit before payment");
        }
        ticket.setStatus(TicketStatus.PAID);
        ticket.setPaymentReference(request.paymentReference());
        ticket.setPaidAt(Instant.now());
        ticket.setOpenVehicleKey(null);               // vehicle may now enter again
        ticket = ticketRepository.save(ticket);

        releaseSpot(ticket);                          // best effort; the reconciler retries failures
        return mapper.toResponse(ticket);
    }

    // ------------------------------------------------------------------------------------------ RECONCILIATION

    /** Retries spot releases that failed (e.g. circuit open) for tickets that are already paid. */
    public int retryPendingSpotReleases() {
        int released = 0;
        for (Ticket t : ticketRepository.findByStatusAndSpotReleasedFalse(TicketStatus.PAID)) {
            if (releaseSpot(t)) {
                released++;
            }
        }
        return released;
    }

    private boolean releaseSpot(Ticket ticket) {
        try {
            spotGateway.release(ticket.getTicketNumber());
            ticket.setSpotReleased(true);
            ticketRepository.save(ticket);
            return true;
        } catch (RuntimeException ex) {
            log.warn("Could not release spot for paid ticket {} - will retry: {}", ticket.getTicketNumber(),
                    ex.getMessage());
            return false;
        }
    }

    // ------------------------------------------------------------------------------------------ helpers

    private Ticket findEntity(String ticketNumber) {
        return ticketRepository.findByTicketNumber(ticketNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + ticketNumber));
    }

    private Vehicle findOrCreateVehicle(String plate, VehicleType type) {
        return vehicleRepository.findByLicensePlate(plate)
                .map(v -> {
                    if (v.getVehicleType() != type) {
                        v.setVehicleType(type);
                        return vehicleRepository.save(v);
                    }
                    return v;
                })
                .orElseGet(() -> {
                    try {
                        return vehicleRepository.save(Vehicle.builder()
                                .licensePlate(plate).vehicleType(type).createdAt(Instant.now()).build());
                    } catch (DataIntegrityViolationException race) {
                        return vehicleRepository.findByLicensePlate(plate).orElseThrow();
                    }
                });
    }

    private static String normalizePlate(String raw) {
        return raw.trim().toUpperCase(Locale.ROOT).replaceAll("\\s+", "");
    }

    private static String generateTicketNumber() {
        return "TKT-" + DAY.format(LocalDate.now(ZoneOffset.UTC)) + "-"
                + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }
}
