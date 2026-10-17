package com.parking.ticket.repository;

import com.parking.ticket.domain.Ticket;
import com.parking.ticket.domain.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    Optional<Ticket> findByTicketNumber(String ticketNumber);

    boolean existsByVehicleLicensePlateAndStatusIn(String licensePlate, Collection<TicketStatus> statuses);

    List<Ticket> findByStatusAndSpotReleasedFalse(TicketStatus status);
}
