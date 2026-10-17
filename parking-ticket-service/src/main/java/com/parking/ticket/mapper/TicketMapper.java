package com.parking.ticket.mapper;

import com.parking.ticket.client.SpotAllocationResponse;
import com.parking.ticket.domain.Ticket;
import com.parking.ticket.dto.EntryResponse;
import com.parking.ticket.dto.ExitResponse;
import com.parking.ticket.dto.TicketResponse;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class TicketMapper {

    public EntryResponse toEntryResponse(Ticket t, SpotAllocationResponse spot) {
        return new EntryResponse(t.getTicketNumber(), t.getVehicle().getLicensePlate(), t.getVehicle().getVehicleType(),
                t.getSpotCode(), t.getFloorNumber(), t.getSpotType(), t.getEntryTime(), t.getStatus(),
                spot != null ? spot.servedBy() : null);
    }

    public ExitResponse toExitResponse(Ticket t, String message) {
        return new ExitResponse(t.getTicketNumber(), t.getVehicle().getLicensePlate(), t.getVehicle().getVehicleType(),
                t.getEntryTime(), t.getExitTime(), durationMinutes(t), t.getFeeAmount(), t.getCurrency(),
                t.getStatus(), message);
    }

    public TicketResponse toResponse(Ticket t) {
        return new TicketResponse(t.getTicketNumber(), t.getVehicle().getLicensePlate(), t.getVehicle().getVehicleType(),
                t.getSpotCode(), t.getFloorNumber(), t.getSpotType(), t.getEntryTime(), t.getExitTime(),
                t.getExitTime() != null ? durationMinutes(t) : null, t.getFeeAmount(), t.getCurrency(),
                t.getStatus(), t.getPaymentReference(), t.getPaidAt(), t.isSpotReleased());
    }

    private long durationMinutes(Ticket t) {
        return t.getExitTime() == null ? 0 : Duration.between(t.getEntryTime(), t.getExitTime()).toMinutes();
    }
}
