package com.parking.spot.strategy;

import com.parking.spot.domain.AllocationStrategyType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class AllocationStrategyFactory {

    private final Map<AllocationStrategyType, SpotAllocationStrategy> strategies;
    private final AllocationStrategyType defaultType;

    public AllocationStrategyFactory(List<SpotAllocationStrategy> all,
                                     @Value("${parking.allocation.default-strategy:BEST_FIT}")
                                     AllocationStrategyType defaultType) {
        this.strategies = all.stream().collect(Collectors.toMap(
                SpotAllocationStrategy::type,
                Function.identity(),
                (a, b) -> a,
                () -> new EnumMap<AllocationStrategyType, SpotAllocationStrategy>(AllocationStrategyType.class)));
        this.defaultType = defaultType;
    }

    public SpotAllocationStrategy get(AllocationStrategyType requested) {
        return strategies.get(requested != null ? requested : defaultType);
    }
}
