package com.autosalone.dtos.responses;

import java.math.BigDecimal;
import java.time.LocalDate;
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
        LocalDate handoverDate,
        String licensePlate,
        LocalDate registrationDate,
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
                vehicle.getHandoverDate(),
                vehicle.getLicensePlate(),
                vehicle.getRegistrationDate(),
                vehicle.getKilometers(),
                vehicle.isInShowroom(),
                vehicle.getStatus(),
                vehicle.getWithdrawalReason());
    }
}