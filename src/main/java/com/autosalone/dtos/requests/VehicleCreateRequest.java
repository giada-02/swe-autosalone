package com.autosalone.dtos.requests;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.autosalone.enums.VehicleCondition;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record VehicleCreateRequest(
        @NotBlank String brand,
        @NotBlank String model,
        @NotBlank String color,
        @NotNull VehicleCondition condition,
        @PositiveOrZero BigDecimal sellingPrice,
        LocalDate handoverDate,
        @Size(max = 20, message = "cannot exceed 20 characters") String licensePlate,
        LocalDate registrationDate,
        @PositiveOrZero Double kilometers,
        @NotNull boolean inShowroom,
        PurchaseTransactionRequest purchaseTransaction) {
}
