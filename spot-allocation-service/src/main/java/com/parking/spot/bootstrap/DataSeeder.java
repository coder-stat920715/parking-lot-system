package com.parking.spot.bootstrap;

import com.parking.spot.domain.ParkingFloor;
import com.parking.spot.domain.ParkingSpot;
import com.parking.spot.domain.SpotType;
import com.parking.spot.repository.ParkingFloorRepository;
import com.parking.spot.repository.ParkingSpotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Seeds 3 floors x (4 SMALL, 6 MEDIUM, 3 LARGE, 2 EV) = 45 spots when the shared DB is empty.
 * Unique constraints make a concurrent seed by a second instance fail harmlessly.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private static final String[] FLOOR_NAMES = {"Ground", "First", "Second"};
    private static final Map<SpotType, Integer> LAYOUT = new LinkedHashMap<>();

    static {
        LAYOUT.put(SpotType.SMALL, 4);
        LAYOUT.put(SpotType.MEDIUM, 6);
        LAYOUT.put(SpotType.LARGE, 3);
        LAYOUT.put(SpotType.EV, 2);
    }

    private final ParkingFloorRepository floorRepository;
    private final ParkingSpotRepository spotRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (floorRepository.count() > 0) {
            log.info("Parking layout already present - skipping seed");
            return;
        }
        try {
            for (int f = 0; f < FLOOR_NAMES.length; f++) {
                ParkingFloor floor = floorRepository.save(
                        ParkingFloor.builder().floorNumber(f).name(FLOOR_NAMES[f]).build());
                List<ParkingSpot> spots = new ArrayList<>();
                int distance = f * 100;
                for (Map.Entry<SpotType, Integer> entry : LAYOUT.entrySet()) {
                    for (int i = 1; i <= entry.getValue(); i++) {
                        distance++;
                        spots.add(ParkingSpot.builder()
                                .spotCode(String.format("F%d-%s%02d", f, entry.getKey().name().charAt(0), i))
                                .floor(floor)
                                .spotType(entry.getKey())
                                .distanceFromEntrance(distance)
                                .build());
                    }
                }
                spotRepository.saveAll(spots);
            }
            log.info("Seeded parking layout: {} spots", spotRepository.count());
        } catch (DataIntegrityViolationException e) {
            log.info("Parking layout was seeded concurrently by another instance");
        }
    }
}
