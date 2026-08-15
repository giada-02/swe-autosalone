package com.autosalone.dtos.responses;

import java.math.BigDecimal;
import java.util.UUID;

import com.autosalone.enums.VehicleCondition;
import com.autosalone.enums.VehicleStatus;
import com.autosalone.models.Vehicle;

public record VehicleResponse(
        UUID id,
        String brand,
        String model,
        String color,
        VehicleCondition condition,
        TransactionResponse purchaseTransaction,
        BigDecimal sellingPrice,
        String handoverDate,
        String licensePlate,
        String registrationDate,
        Double kilometers,
        boolean isInShowroom,
        VehicleStatus status,
        String withdrawalReason) {

    public static VehicleResponse fromEntity(Vehicle vehicle) {
        if (vehicle == null)
            return null;

        TransactionResponse transactionResponse = vehicle.getPurchaseTransaction() != null
                ? TransactionResponse.fromEntity(vehicle.getPurchaseTransaction())
                : null;

        return new VehicleResponse(
                vehicle.getId(),
                vehicle.getBrand(),
                vehicle.getModel(),
                vehicle.getColor(),
                vehicle.getCondition(),
                transactionResponse,
                vehicle.getSellingPrice(),
                vehicle.getHandoverDate() != null ? vehicle.getHandoverDate().toString() : null,
                vehicle.getLicensePlate(),
                vehicle.getRegistrationDate() != null ? vehicle.getRegistrationDate().toString() : null,
                vehicle.getKilometers(),
                vehicle.isInShowroom(),
                vehicle.getStatus(),
                vehicle.getWithdrawalReason());
    }
}