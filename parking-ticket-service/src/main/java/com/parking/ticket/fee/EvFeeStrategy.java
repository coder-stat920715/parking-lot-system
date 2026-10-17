package com.parking.ticket.fee;

import com.parking.ticket.domain.VehicleType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class EvFeeStrategy extends HourlyCappedFeeStrategy {

    public EvFeeStrategy() {
        super(VehicleType.EV_CAR, "25", "400");
    }

    /** Flat charging-session fee on top of the hourly parking rate. */
    @Override
    protected BigDecimal surcharge() {
        return new BigDecimal("50");
    }
}
