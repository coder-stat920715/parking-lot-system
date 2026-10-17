package com.parking.ticket.fee;

import com.parking.ticket.domain.VehicleType;

import java.math.BigDecimal;
import java.time.Duration;

/** Strategy pattern: one implementation per vehicle type. */
public interface FeeCalculationStrategy {

    VehicleType vehicleType();

    BigDecimal calculate(Duration parkedFor);
}
