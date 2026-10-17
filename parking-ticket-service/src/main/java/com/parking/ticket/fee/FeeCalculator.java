package com.parking.ticket.fee;

import com.parking.ticket.domain.VehicleType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class FeeCalculator {

    public static final String CURRENCY = "INR";

    private final Map<VehicleType, FeeCalculationStrategy> strategies;

    public FeeCalculator(List<FeeCalculationStrategy> all) {
        this.strategies = all.stream()
                .collect(Collectors.toMap(FeeCalculationStrategy::vehicleType, Function.identity()));
    }

    public BigDecimal calculate(VehicleType vehicleType, Duration parkedFor) {
        FeeCalculationStrategy strategy = strategies.get(vehicleType);
        if (strategy == null) {
            throw new IllegalStateException("No fee strategy registered for " + vehicleType);
        }
        return strategy.calculate(parkedFor);
    }
}
