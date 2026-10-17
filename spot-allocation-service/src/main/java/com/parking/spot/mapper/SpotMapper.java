package com.parking.spot.mapper;

import com.parking.spot.domain.AllocationStrategyType;
import com.parking.spot.domain.ParkingSpot;
import com.parking.spot.dto.AllocationResponse;
import com.parking.spot.dto.AvailabilityEntry;
import com.parking.spot.dto.ReleaseResponse;
import com.parking.spot.repository.AvailabilityRow;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SpotMapper {

    private final String instanceId;

    public SpotMapper(@Value("${spring.application.name}") String appName,
                      @Value("${server.port}") String port) {
        this.instanceId = appName + ":" + port;
    }

    /** Identifies which service instance handled the call (visible in responses to demo load balancing). */
    public String instanceId() {
        return instanceId;
    }

    public AllocationResponse toAllocationResponse(ParkingSpot spot, AllocationStrategyType strategy) {
        return new AllocationResponse(spot.getAssignedTicketNumber(), spot.getSpotCode(), spot.getFloorNumber(),
                spot.getSpotType(), spot.getDistanceFromEntrance(), strategy, instanceId);
    }

    public ReleaseResponse toReleaseResponse(String ticketNumber, ParkingSpot spotOrNull) {
        return new ReleaseResponse(ticketNumber, spotOrNull != null,
                spotOrNull != null ? spotOrNull.getSpotCode() : null, instanceId);
    }

    public AvailabilityEntry toEntry(AvailabilityRow row) {
        return new AvailabilityEntry(row.getFloorNumber(), row.getSpotType(), row.getTotal(), row.getAvailable());
    }
}
