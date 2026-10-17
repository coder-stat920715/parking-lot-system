package com.parking.ticket.fee;

import com.parking.ticket.domain.VehicleType;
import org.springframework.stereotype.Component;

@Component
public class MotorcycleFeeStrategy extends HourlyCappedFeeStrategy {

    public MotorcycleFeeStrategy() {
        super(VehicleType.MOTORCYCLE, "10", "150");
    }
}
