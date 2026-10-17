package com.parking.ticket.fee;

import com.parking.ticket.domain.VehicleType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;

/**
 * Billing rule shared by all vehicle types: every started hour is billable (minimum one hour), and each 24h block
 * is capped at dailyCap. Subclasses supply rates and optionally a flat surcharge.
 */
public abstract class HourlyCappedFeeStrategy implements FeeCalculationStrategy {

    private final VehicleType vehicleType;
    private final BigDecimal hourlyRate;
    private final BigDecimal dailyCap;

    protected HourlyCappedFeeStrategy(VehicleType vehicleType, String hourlyRate, String dailyCap) {
        this.vehicleType = vehicleType;
        this.hourlyRate = new BigDecimal(hourlyRate);
        this.dailyCap = new BigDecimal(dailyCap);
    }

    @Override
    public VehicleType vehicleType() {
        return vehicleType;
    }

    @Override
    public BigDecimal calculate(Duration parkedFor) {
        long minutes = Math.max(1, (parkedFor.toSeconds() + 59) / 60);
        long hours = (minutes + 59) / 60;                 // round up to started hours
        long fullDays = hours / 24;
        long remainderHours = hours % 24;

        BigDecimal remainder = hourlyRate.multiply(BigDecimal.valueOf(remainderHours)).min(dailyCap);
        return dailyCap.multiply(BigDecimal.valueOf(fullDays))
                .add(remainder)
                .add(surcharge())
                .setScale(2, RoundingMode.HALF_UP);
    }

    protected BigDecimal surcharge() {
        return BigDecimal.ZERO;
    }
}
