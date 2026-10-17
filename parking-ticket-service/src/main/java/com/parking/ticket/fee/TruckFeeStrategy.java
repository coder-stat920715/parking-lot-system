package com.parking.ticket.fee;

import com.parking.ticket.domain.VehicleType;
import org.springframework.stereotype.Component;

@Component
public class TruckFeeStrategy extends HourlyCappedFeeStrategy {

    public TruckFeeStrategy() {
        super(VehicleType.TRUCK, "40", "600");
    }
}
