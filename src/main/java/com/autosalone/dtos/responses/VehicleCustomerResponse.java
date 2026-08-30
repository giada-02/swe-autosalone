package com.autosalone.dtos.responses;

import java.math.BigDecimal;
import java.util.UUID;

import com.autosalone.enums.VehicleCondition;
import com.autosalone.models.Vehicle;

public record VehicleCustomerResponse(
        UUID id,
        String brand,
        String model,
        String color,
        VehicleCondition condition,
        BigDecimal sellingPrice,
        String handoverDate,
        String licensePlate,
        String registrationDate,
        Double kilometers) {

    public static VehicleCustomerResponse fromEntity(Vehicle vehicle) {
        if (vehicle == null)
            return null;

        return new VehicleCustomerResponse(
                vehicle.getId(),
                vehicle.getBrand(),
                vehicle.getModel(),
                vehicle.getColor(),
                vehicle.getCondition(),
                vehicle.getSellingPrice(),
                vehicle.getHandoverDate() != null ? vehicle.getHandoverDate().toString() : null,
                vehicle.getLicensePlate(),
                vehicle.getRegistrationDate() != null ? vehicle.getRegistrationDate().toString() : null,
                vehicle.getKilometers());
    }
}