package com.parking.ticket.fee;

import com.parking.ticket.domain.VehicleType;
import org.springframework.stereotype.Component;

@Component
public class CarFeeStrategy extends HourlyCappedFeeStrategy {

    public CarFeeStrategy() {
        super(VehicleType.CAR, "20", "300");
    }
}
