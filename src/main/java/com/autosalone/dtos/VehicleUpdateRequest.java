package com.autosalone.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.autosalone.enums.VehicleCondition;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record VehicleUpdateRequest(
        @NotBlank String brand,
        @NotBlank String model,
        @NotBlank String color,
        @NotNull VehicleCondition condition,
        @Positive BigDecimal purchaseTransactionAmount,
        LocalDate purchaseTransactionDate,
        @PositiveOrZero BigDecimal sellingPrice,
        LocalDate handoverDate,
        String licensePlate,
        LocalDate registrationDate,
        @PositiveOrZero Double kilometers,
        @NotNull Boolean inShowroom) {

    public VehicleUpdateRequest {
        if ((purchaseTransactionAmount != null && purchaseTransactionDate == null) ||
                (purchaseTransactionAmount == null && purchaseTransactionDate != null)) {
            throw new IllegalArgumentException(
                    "If the purchase amount is provided, the purchase date must also be provided (and vice versa)");
        }
    }
}
