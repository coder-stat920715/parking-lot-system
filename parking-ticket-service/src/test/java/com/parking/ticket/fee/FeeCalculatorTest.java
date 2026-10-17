package com.parking.ticket.fee;

import com.parking.ticket.domain.VehicleType;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FeeCalculatorTest {

    private final FeeCalculator calculator = new FeeCalculator(List.of(
            new MotorcycleFeeStrategy(), new CarFeeStrategy(), new TruckFeeStrategy(), new EvFeeStrategy()));

    @Test
    void zeroDurationStillBillsOneHour() {
        assertThat(calculator.calculate(VehicleType.CAR, Duration.ZERO)).isEqualByComparingTo("20.00");
    }

    @Test
    void startedHoursAreRoundedUp() {
        assertThat(calculator.calculate(VehicleType.CAR, Duration.ofMinutes(61))).isEqualByComparingTo("40.00");
        assertThat(calculator.calculate(VehicleType.MOTORCYCLE, Duration.ofMinutes(60))).isEqualByComparingTo("10.00");
    }

    @Test
    void dailyCapApplies() {
        assertThat(calculator.calculate(VehicleType.CAR, Duration.ofHours(24))).isEqualByComparingTo("300.00");
        assertThat(calculator.calculate(VehicleType.CAR, Duration.ofHours(30))).isEqualByComparingTo("420.00");
    }

    @Test
    void evAddsChargingSurcharge() {
        assertThat(calculator.calculate(VehicleType.EV_CAR, Duration.ofHours(2))).isEqualByComparingTo("100.00");
    }

    @Test
    void truckRate() {
        assertThat(calculator.calculate(VehicleType.TRUCK, Duration.ofHours(3))).isEqualByComparingTo("120.00");
    }
}
