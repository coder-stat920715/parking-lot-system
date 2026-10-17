package com.parking.spot.repository;

import com.parking.spot.domain.ParkingSpot;
import com.parking.spot.domain.SpotStatus;
import com.parking.spot.domain.SpotType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ParkingSpotRepository extends JpaRepository<ParkingSpot, Long> {

    @EntityGraph(attributePaths = "floor")
    List<ParkingSpot> findBySpotTypeInAndStatus(Collection<SpotType> spotTypes, SpotStatus status);

    @EntityGraph(attributePaths = "floor")
    Optional<ParkingSpot> findByAssignedTicketNumber(String ticketNumber);

    @Query("""
            select f.floorNumber as floorNumber,
                   s.spotType as spotType,
                   count(s) as total,
                   sum(case when s.status = com.parking.spot.domain.SpotStatus.AVAILABLE then 1 else 0 end) as available
            from ParkingSpot s join s.floor f
            group by f.floorNumber, s.spotType
            order by f.floorNumber, s.spotType
            """)
    List<AvailabilityRow> summarize();
}
